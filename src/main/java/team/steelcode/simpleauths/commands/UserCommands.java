package team.steelcode.simpleauths.commands;


import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import team.steelcode.simpleauths.cache.LoggedPlayersService;
import team.steelcode.simpleauths.database.entities.User;
import team.steelcode.simpleauths.database.entities.UserIPHistory;
import team.steelcode.simpleauths.database.services.UserService;
import team.steelcode.simpleauths.database.util.UserStats;
import team.steelcode.simpleauths.network.AuthSyncPacket;

import java.util.List;

public class UserCommands {

    private static final UserService userService = new UserService();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("auth")

                .then(Commands.literal("changepassword")
                        .then(Commands.argument("oldpassword", StringArgumentType.word())
                                .then(Commands.argument("newpassword", StringArgumentType.word())
                                        .executes(UserCommands::changePassword))))
                .then(Commands.literal("stats")
                        .executes(UserCommands::showUserStats))
                .then(Commands.literal("iphistory")
                        .executes(UserCommands::showIPHistory))
                .then(Commands.literal("help")
                        .executes(UserCommands::showHelp))
        );


        dispatcher.register(Commands.literal("register")
                        .then(Commands.argument("password", StringArgumentType.word())
                                .then(Commands.argument("repeat_password", StringArgumentType.word())
                                        .executes(UserCommands::registerUser))));

        dispatcher.register(Commands.literal("login")
                        .then(Commands.argument("password", StringArgumentType.word())
                                .executes(UserCommands::loginUser)));
    }

    private static int registerUser(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String password = StringArgumentType.getString(context, "password");
            String repeat_password = StringArgumentType.getString(context, "repeat_password");

            String playerUUID = player.getStringUUID();
            String playerName = player.getName().getString();

            // Verificar si ya está registrado
            if (userService.userExists(playerUUID)) {
                player.sendSystemMessage(Component.literal("§c¡Ya estás registrado! Usa /login <contraseña>"));
                return 0;
            }

            // Validar contraseña
            if (password.length() < 4 || password.length() > 16) {
                player.sendSystemMessage(Component.literal("§cLa contraseña debe tener entre 4 y 16 caracteres."));
                return 0;
            }

           if (!password.equals(repeat_password)) {
               player.sendSystemMessage(Component.literal("§cLas contraseñas no coinciden."));
               return 0;
           }


            User user = userService.createUser(playerName, password);
            System.out.println("Usuario registrado: " + user.getUuid() + " - " + user.getPassword());
            player.sendSystemMessage(Component.literal("§a¡Registro exitoso! Ahora puedes usar /auth login <contraseña>"));

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al registrar usuario: " + e.getMessage()));
            return 0;
        }
    }

    private static int loginUser(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String password = StringArgumentType.getString(context, "password");

            String playerName = player.getName().getString();

            // Verificar si está registrado
            if (!userService.userExists(playerName)) {
                player.sendSystemMessage(Component.literal("§cNo estás registrado. Usa /auth register <contraseña> <email>"));
                return 0;
            }

            // Validar credenciales
            if (!userService.validateCredentials(playerName, password)) {
                player.sendSystemMessage(Component.literal("§cContraseña incorrecta."));
                return 0;
            }

            player.sendSystemMessage(Component.literal("§a¡Inicio de sesión exitoso!"));

            LoggedPlayersService.loginPlayer(player.getStringUUID());
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new AuthSyncPacket(true));
            }

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al iniciar sesión: " + e.getMessage()));
            return 0;
        }
    }

    public static int showHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§6=== Comandos de SimpleAuths ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/auth register <contraseña> §7- Registrar nueva cuenta"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/auth login <contraseña> §7- Iniciar sesión"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/auth changepassword <actual> <nueva> §7- Cambiar contraseña"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/auth stats §7- Ver tus estadísticas"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/auth iphistory §7- Ver historial de IPs"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/auth help §7- Mostrar este mensaje de ayuda"), false);

        return 1;
    }

    private static int changePassword(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String oldPassword = StringArgumentType.getString(context, "oldpassword");
            String newPassword = StringArgumentType.getString(context, "newpassword");

            String username = player.getName().getString();

            // Verificar si está registrado
            if (!userService.userExists(username)) {
                player.sendSystemMessage(Component.literal("§cNo estás registrado."));
                return 0;
            }

            // Validar nueva contraseña
            if (newPassword.length() < 4 || newPassword.length() > 16) {
                player.sendSystemMessage(Component.literal("§cLa nueva contraseña debe tener entre 4 y 16 caracteres."));
                return 0;
            }

            if (userService.changePassword(username, oldPassword, newPassword)) {
                player.sendSystemMessage(Component.literal("§aContraseña cambiada exitosamente."));
                return 1;
            } else {
                player.sendSystemMessage(Component.literal("§cContraseña actual incorrecta."));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al cambiar contraseña: " + e.getMessage()));
            return 0;
        }
    }

    private static int showUserStats(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String username = player.getName().getString();

            UserStats stats = userService.getUserStats(username);
            if (stats == null) {
                player.sendSystemMessage(Component.literal("§cNo estás registrado. Usa /auth register <contraseña> <email>"));
                return 0;
            }

            player.sendSystemMessage(Component.literal("§6=== Estadísticas de Usuario ==="));
            player.sendSystemMessage(Component.literal("§eTiempo jugado: §f" + formatPlayTime(stats.playTime())));
            player.sendSystemMessage(Component.literal("§eIPs totales: §f" + stats.totalIPs()));
            player.sendSystemMessage(Component.literal("§eIPs confiables: §a" + stats.trustedIPs()));
            player.sendSystemMessage(Component.literal("§eIPs bloqueadas: §c" + stats.blockedIPs()));
            player.sendSystemMessage(Component.literal("§eMáximo IPs permitidas: §f" + stats.maxIpsAllowed()));
            player.sendSystemMessage(Component.literal("§eVerificación IP requerida: " + (stats.requiresIPVerification() ? "§aSí" : "§cNo")));

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al mostrar estadísticas: " + e.getMessage()));
            return 0;
        }
    }

    private static int showIPHistory(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String username = player.getName().getString();

            List<UserIPHistory> ipHistory = userService.getUserIPHistory(username);
            if (ipHistory.isEmpty()) {
                player.sendSystemMessage(Component.literal("§cNo tienes historial de IPs."));
                return 0;
            }

            player.sendSystemMessage(Component.literal("§6=== Historial de IPs ==="));

            int count = 0;
            for (UserIPHistory ip : ipHistory) {
                if (count >= 10) { // Limitar a 10 IPs para no spamear el chat
                    player.sendSystemMessage(Component.literal("§7... y " + (ipHistory.size() - count) + " más"));
                    break;
                }

                String status = "";
                if (ip.getTrusted()) status += "§a[CONFIABLE] ";
                if (ip.getBlocked()) status += "§c[BLOQUEADA] ";

                player.sendSystemMessage(Component.literal(String.format("§e%s §f- Logins: §b%d §f- Último: §7%s %s",
                        ip.getIpAddress(),
                        ip.getLoginCount(),
                        ip.getLastSeen().toString().substring(0, 16), // Solo fecha y hora, sin segundos
                        status)));

                count++;
            }

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al mostrar historial de IPs: " + e.getMessage()));
            return 0;
        }
    }

    private static String formatPlayTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }
}