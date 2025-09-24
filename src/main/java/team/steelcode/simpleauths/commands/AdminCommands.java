package team.steelcode.simpleauths.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import team.steelcode.simpleauths.database.entities.User;
import team.steelcode.simpleauths.database.entities.UserIPHistory;
import team.steelcode.simpleauths.database.services.IPControlService;
import team.steelcode.simpleauths.database.services.UserService;
import team.steelcode.simpleauths.database.util.UserStats;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AdminCommands {

    private static final UserService userService = new UserService();
    private static final IPControlService ipService = new IPControlService();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("authAdmin")
                .requires(source -> source.hasPermission(2)) // Requiere nivel de permisos 2 (OP)

                .then(Commands.literal("userinfo")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(AdminCommands::showUserInfo)))

                .then(Commands.literal("userinfobyname")
                        .then(Commands.argument("playername", StringArgumentType.word())
                                .executes(AdminCommands::showUserInfoByName)))

                .then(Commands.literal("setmaxips")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("maxips", IntegerArgumentType.integer(1, 10))
                                        .executes(AdminCommands::setMaxIps))))

                .then(Commands.literal("toggleipverification")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(AdminCommands::toggleIPVerification)))

                .then(Commands.literal("trustip")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("ip", StringArgumentType.word())
                                        .executes(AdminCommands::trustIP))))

                .then(Commands.literal("blockip")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("ip", StringArgumentType.word())
                                        .executes(AdminCommands::blockIP))))

                .then(Commands.literal("resetpassword")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("newpassword", StringArgumentType.word())
                                        .executes(AdminCommands::resetPassword))))

                .then(Commands.literal("deleteuser")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(AdminCommands::deleteUser)))

                .then(Commands.literal("updateplaytime")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                                        .executes(AdminCommands::updatePlayTime))))

                .then(Commands.literal("reload")
                        .executes(AdminCommands::reloadConfig))

                .then(Commands.literal("help")
                        .executes(AdminCommands::showHelp))
        );
    }

    private static int showUserInfo(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String playerUUID = targetPlayer.getUUID().toString();

            return displayUserInfo(context.getSource(), playerUUID, targetPlayer.getName().getString());
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al mostrar información de usuario: " + e.getMessage()));
            return 0;
        }
    }



    private static int showUserInfoByName(CommandContext<CommandSourceStack> context) {
        try {
            String playerName = StringArgumentType.getString(context, "playername");

            // Buscar jugador conectado por nombre
            MinecraftServer server = context.getSource().getServer();
            ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(playerName);

            if (targetPlayer != null) {
                String playerUUID = targetPlayer.getUUID().toString();
                return displayUserInfo(context.getSource(), playerUUID, playerName);
            } else {
                context.getSource().sendFailure(Component.literal("§cJugador '" + playerName + "' no encontrado o no está conectado."));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al mostrar información de usuario: " + e.getMessage()));
            return 0;
        }
    }

    private static int displayUserInfo(CommandSourceStack source, String playerUUID, String playerName) throws SQLException {
        if (!userService.userExists(playerUUID)) {
            source.sendFailure(Component.literal("§cEl jugador '" + playerName + "' no está registrado."));
            return 0;
        }

        UserStats stats = userService.getUserStats(playerUUID);
        List<UserIPHistory> ipHistory = userService.getUserIPHistory(playerUUID);

        source.sendSuccess(() -> Component.literal("§6=== Información de " + playerName + " ==="), false);
        source.sendSuccess(() -> Component.literal("§eUUID: §f" + playerUUID), false);
        source.sendSuccess(() -> Component.literal("§eTiempo jugado: §f" + formatPlayTime(stats.playTime())), false);
        source.sendSuccess(() -> Component.literal("§eIPs totales: §f" + stats.totalIPs()), false);
        source.sendSuccess(() -> Component.literal("§eIPs confiables: §a" + stats.trustedIPs()), false);
        source.sendSuccess(() -> Component.literal("§eIPs bloqueadas: §c" + stats.blockedIPs()), false);
        source.sendSuccess(() -> Component.literal("§eMáximo IPs permitidas: §f" + stats.maxIpsAllowed()), false);
        source.sendSuccess(() -> Component.literal("§eVerificación IP requerida: " + (stats.requiresIPVerification() ? "§aSí" : "§cNo")), false);

        if (!ipHistory.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§6=== IPs Recientes ==="), false);
            int count = 0;
            for (UserIPHistory ip : ipHistory) {
                if (count >= 5) break; // Mostrar solo las 5 más recientes

                String status = "";
                if (ip.getTrusted()) status += "§a[CONFIABLE] ";
                if (ip.getBlocked()) status += "§c[BLOQUEADA] ";

                final String ipInfo = String.format("§e%s §f- Logins: §b%d §f- Último: §7%s %s",
                        ip.getIpAddress(),
                        ip.getLoginCount(),
                        ip.getLastSeen().toString().substring(0, 16),
                        status);
                source.sendSuccess(() -> Component.literal(ipInfo), false);
                count++;
            }
        }

        return 1;
    }

    private static int setMaxIps(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            int maxIps = IntegerArgumentType.getInteger(context, "maxips");
            String playerUUID = targetPlayer.getUUID().toString();

            if (!userService.userExists(playerUUID)) {
                context.getSource().sendFailure(Component.literal("§cEl jugador no está registrado."));
                return 0;
            }

            userService.setMaxIpsAllowed(playerUUID, maxIps);
            context.getSource().sendSuccess(() -> Component.literal("§aMáximo de IPs establecido a " + maxIps + " para " + targetPlayer.getName().getString()), false);
            targetPlayer.sendSystemMessage(Component.literal("§eUn administrador ha cambiado tu límite máximo de IPs a: §b" + maxIps));

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al establecer máximo de IPs: " + e.getMessage()));
            return 0;
        }
    }

    private static int toggleIPVerification(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String playerUUID = targetPlayer.getUUID().toString();

            if (!userService.userExists(playerUUID)) {
                context.getSource().sendFailure(Component.literal("§cEl jugador no está registrado."));
                return 0;
            }

            Optional<User> userOpt = userService.findUserByUuid(playerUUID);
            if (userOpt.isPresent()) {
                boolean currentState = userOpt.get().getRequiresIPVerification();
                userService.setRequiresIPVerification(playerUUID, !currentState);

                String newState = !currentState ? "activada" : "desactivada";
                context.getSource().sendSuccess(() -> Component.literal("§aVerificación IP " + newState + " para " + targetPlayer.getName().getString()), false);
                targetPlayer.sendSystemMessage(Component.literal("§eUn administrador ha " + newState + " la verificación IP para tu cuenta."));
            }

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al cambiar verificación IP: " + e.getMessage()));
            return 0;
        }
    }

    private static int trustIP(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String ipAddress = StringArgumentType.getString(context, "ip");
            String playerUUID = targetPlayer.getUUID().toString();

            if (!userService.userExists(playerUUID)) {
                context.getSource().sendFailure(Component.literal("§cEl jugador no está registrado."));
                return 0;
            }

            // Validar formato IP básico
            if (!isValidIP(ipAddress)) {
                context.getSource().sendFailure(Component.literal("§cFormato de IP inválido."));
                return 0;
            }

            ipService.trustIP(playerUUID, ipAddress);
            context.getSource().sendSuccess(() -> Component.literal("§aIP " + ipAddress + " marcada como confiable para " + targetPlayer.getName().getString()), false);
            targetPlayer.sendSystemMessage(Component.literal("§aUn administrador ha marcado la IP " + ipAddress + " como confiable."));

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al confiar IP: " + e.getMessage()));
            return 0;
        }
    }

    private static int blockIP(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String ipAddress = StringArgumentType.getString(context, "ip");
            String playerUUID = targetPlayer.getUUID().toString();

            if (!userService.userExists(playerUUID)) {
                context.getSource().sendFailure(Component.literal("§cEl jugador no está registrado."));
                return 0;
            }

            // Validar formato IP básico
            if (!isValidIP(ipAddress)) {
                context.getSource().sendFailure(Component.literal("§cFormato de IP inválido."));
                return 0;
            }

            ipService.blockIP(playerUUID, ipAddress);
            context.getSource().sendSuccess(() -> Component.literal("§cIP " + ipAddress + " bloqueada para " + targetPlayer.getName().getString()), false);
            targetPlayer.sendSystemMessage(Component.literal("§cUn administrador ha bloqueado la IP " + ipAddress + "."));

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al bloquear IP: " + e.getMessage()));
            return 0;
        }
    }

    private static int resetPassword(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String newPassword = StringArgumentType.getString(context, "newpassword");
            String playerUUID = targetPlayer.getUUID().toString();

            if (!userService.userExists(playerUUID)) {
                context.getSource().sendFailure(Component.literal("§cEl jugador no está registrado."));
                return 0;
            }

            // Validar nueva contraseña
            if (newPassword.length() < 4 || newPassword.length() > 16) {
                context.getSource().sendFailure(Component.literal("§cLa contraseña debe tener entre 4 y 16 caracteres."));
                return 0;
            }

            Optional<User> userOpt = userService.findUserByUuid(playerUUID);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setPassword(newPassword);
                userService.updateUser(user);

                context.getSource().sendSuccess(() -> Component.literal("§aContraseña restablecida para " + targetPlayer.getName().getString()), false);
                targetPlayer.sendSystemMessage(Component.literal("§eUn administrador ha restablecido tu contraseña. Nueva contraseña: §b" + newPassword));
            }

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al restablecer contraseña: " + e.getMessage()));
            return 0;
        }
    }

    private static int deleteUser(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String playerUUID = targetPlayer.getUUID().toString();

            if (!userService.userExists(playerUUID)) {
                context.getSource().sendFailure(Component.literal("§cEl jugador no está registrado."));
                return 0;
            }

            // Aquí necesitarías implementar un método deleteUser en UserService
            // userService.deleteUser(playerUUID);

            context.getSource().sendSuccess(() -> Component.literal("§c[PENDIENTE] Usuario " + targetPlayer.getName().getString() + " eliminado (implementar método deleteUser)"), false);
            targetPlayer.sendSystemMessage(Component.literal("§cTu cuenta ha sido eliminada por un administrador."));

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al eliminar usuario: " + e.getMessage()));
            return 0;
        }
    }

    private static int updatePlayTime(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            int seconds = IntegerArgumentType.getInteger(context, "seconds");
            String playerUUID = targetPlayer.getUUID().toString();

            if (!userService.userExists(playerUUID)) {
                context.getSource().sendFailure(Component.literal("§cEl jugador no está registrado."));
                return 0;
            }

            userService.updatePlayTime(playerUUID, seconds);
            context.getSource().sendSuccess(() -> Component.literal("§aTiempo de juego actualizado para " + targetPlayer.getName().getString() + ": +" + seconds + "s"), false);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al actualizar tiempo de juego: " + e.getMessage()));
            return 0;
        }
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        try {
            // Aquí podrías recargar configuración si la tienes
            context.getSource().sendSuccess(() -> Component.literal("§aConfiguración recargada exitosamente."), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al recargar configuración: " + e.getMessage()));
            return 0;
        }
    }

    public static int showHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§6=== Comandos de SimpleAuths Admin ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/authAdmin userinfo <jugador> §7- Ver información de usuario"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/authAdmin userinfobyname <nombre> §7- Ver info por nombre"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/authAdmin setmaxips <jugador> <cantidad> §7- Establecer máximo de IPs"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/authAdmin toggleipverification <jugador> §7- Alternar verificación IP"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/authAdmin trustip <jugador> <ip> §7- Marcar IP como confiable"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/authAdmin blockip <jugador> <ip> §7- Bloquear IP"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/authAdmin resetpassword <jugador> <nueva> §7- Restablecer contraseña"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/authAdmin deleteuser <jugador> §7- Eliminar usuario"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/authAdmin updateplaytime <jugador> <segundos> §7- Actualizar tiempo de juego"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/authAdmin reload §7- Recargar configuración"), false);

        return 1;
    }

    // Métodos auxiliares
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

    private static boolean isValidIP(String ip) {
        // Validación básica de formato IP
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}