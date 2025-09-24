package team.steelcode.simpleauths.commands;



import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import team.steelcode.simpleauths.database.entities.IPValidationResult;
import team.steelcode.simpleauths.database.entities.UserIPHistory;
import team.steelcode.simpleauths.database.services.IPControlService;
import team.steelcode.simpleauths.database.services.UserService;

import java.util.List;

public class IPCommands {

    private static final IPControlService ipService = new IPControlService();
    private static final UserService userService = new UserService();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("authip")
                .then(Commands.literal("validate")
                        .then(Commands.argument("ip", StringArgumentType.word())
                                .executes(IPCommands::validateIP)))

                .then(Commands.literal("trust")
                        .then(Commands.argument("ip", StringArgumentType.word())
                                .executes(IPCommands::trustCurrentUserIP)))

                .then(Commands.literal("check")
                        .then(Commands.argument("ip", StringArgumentType.word())
                                .executes(IPCommands::checkIPInfo)))

                .then(Commands.literal("myips")
                        .executes(IPCommands::showMyIPs))

                .then(Commands.literal("help")
                        .executes(IPCommands::showHelp))
        );

        // Comandos de administración para IPs
        dispatcher.register(Commands.literal("authipAdmin")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("lookup")
                        .then(Commands.argument("ip", StringArgumentType.word())
                                .executes(IPCommands::lookupIP)))

                .then(Commands.literal("globalblock")
                        .then(Commands.argument("ip", StringArgumentType.word())
                                .executes(IPCommands::globalBlockIP)))

                .then(Commands.literal("globaltrust")
                        .then(Commands.argument("ip", StringArgumentType.word())
                                .executes(IPCommands::globalTrustIP)))

                .then(Commands.literal("stats")
                        .executes(IPCommands::showIPStats))
        );
    }

    // Comandos de usuario
    private static int validateIP(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String ipAddress = StringArgumentType.getString(context, "ip");
            String playerUUID = player.getUUID().toString();

            if (!userService.userExists(playerUUID)) {
                player.sendSystemMessage(Component.literal("§cNo estás registrado."));
                return 0;
            }

            if (!isValidIP(ipAddress)) {
                player.sendSystemMessage(Component.literal("§cFormato de IP inválido."));
                return 0;
            }

            IPValidationResult result = ipService.validateUserIP(playerUUID, ipAddress);
            String message = getValidationMessage(result);

            player.sendSystemMessage(Component.literal("§eResultado de validación para IP " + ipAddress + ":"));
            player.sendSystemMessage(Component.literal(message));

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al validar IP: " + e.getMessage()));
            return 0;
        }
    }

    private static int trustCurrentUserIP(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String ipAddress = StringArgumentType.getString(context, "ip");
            String playerUUID = player.getUUID().toString();

            if (!userService.userExists(playerUUID)) {
                player.sendSystemMessage(Component.literal("§cNo estás registrado."));
                return 0;
            }

            if (!isValidIP(ipAddress)) {
                player.sendSystemMessage(Component.literal("§cFormato de IP inválido."));
                return 0;
            }

            ipService.trustIP(playerUUID, ipAddress);
            player.sendSystemMessage(Component.literal("§aIP " + ipAddress + " marcada como confiable."));

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al confiar IP: " + e.getMessage()));
            return 0;
        }
    }

    private static int checkIPInfo(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String ipAddress = StringArgumentType.getString(context, "ip");
            String playerUUID = player.getUUID().toString();

            if (!userService.userExists(playerUUID)) {
                player.sendSystemMessage(Component.literal("§cNo estás registrado."));
                return 0;
            }

            if (!isValidIP(ipAddress)) {
                player.sendSystemMessage(Component.literal("§cFormato de IP inválido."));
                return 0;
            }

            List<UserIPHistory> ipHistory = userService.getUserIPHistory(playerUUID);
            UserIPHistory targetIP = ipHistory.stream()
                    .filter(ip -> ip.getIpAddress().equals(ipAddress))
                    .findFirst()
                    .orElse(null);

            if (targetIP == null) {
                player.sendSystemMessage(Component.literal("§cNo tienes registros para la IP " + ipAddress));
                return 0;
            }

            player.sendSystemMessage(Component.literal("§6=== Información de IP " + ipAddress + " ==="));
            player.sendSystemMessage(Component.literal("§ePrimera vez vista: §f" + targetIP.getFirstSeen().toString().substring(0, 16)));
            player.sendSystemMessage(Component.literal("§eÚltima vez vista: §f" + targetIP.getLastSeen().toString().substring(0, 16)));
            player.sendSystemMessage(Component.literal("§eTotal de logins: §b" + targetIP.getLoginCount()));
            player.sendSystemMessage(Component.literal("§eEstado: " + (targetIP.getTrusted() ? "§aConfiable" : "§eNormal") +
                    (targetIP.getBlocked() ? " §c[BLOQUEADA]" : "")));

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al verificar información de IP: " + e.getMessage()));
            return 0;
        }
    }

    private static int showMyIPs(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String playerUUID = player.getUUID().toString();

            List<UserIPHistory> ipHistory = userService.getUserIPHistory(playerUUID);
            if (ipHistory.isEmpty()) {
                player.sendSystemMessage(Component.literal("§cNo tienes historial de IPs."));
                return 0;
            }

            player.sendSystemMessage(Component.literal("§6=== Mis IPs Registradas ==="));

            // Separar por estado
            long trustedCount = ipHistory.stream().filter(UserIPHistory::getTrusted).count();
            long blockedCount = ipHistory.stream().filter(UserIPHistory::getBlocked).count();
            long normalCount = ipHistory.size() - trustedCount - blockedCount;

            player.sendSystemMessage(Component.literal("§eTotal: §f" + ipHistory.size() +
                    " (§aConfiables: " + trustedCount +
                    "§f, §eNormales: " + normalCount +
                    "§f, §cBloqueadas: " + blockedCount + "§f)"));

            // Mostrar IPs con estado
            int count = 0;
            for (UserIPHistory ip : ipHistory) {
                if (count >= 8) { // Limitar para no spamear
                    player.sendSystemMessage(Component.literal("§7... y " + (ipHistory.size() - count) + " más"));
                    break;
                }

                String status = "§eNormal";
                if (ip.getTrusted()) status = "§aConfiable";
                if (ip.getBlocked()) status = "§cBloqueada";

                player.sendSystemMessage(Component.literal("§f" + ip.getIpAddress() + " §7(" + status + "§7) - §bLogins: " + ip.getLoginCount()));
                count++;
            }

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al mostrar IPs: " + e.getMessage()));
            return 0;
        }
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§6=== Comandos de IP SimpleAuths ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/authip validate <ip> §7- Validar una IP"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/authip trust <ip> §7- Marcar tu IP como confiable"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/authip check <ip> §7- Ver información de tu IP"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/authip myips §7- Ver todas tus IPs registradas"), false);

        return 1;
    }

    // Comandos de administración
    private static int lookupIP(CommandContext<CommandSourceStack> context) {
        try {
            String ipAddress = StringArgumentType.getString(context, "ip");

            if (!isValidIP(ipAddress)) {
                context.getSource().sendFailure(Component.literal("§cFormato de IP inválido."));
                return 0;
            }

            // Aquí necesitarías un método en el repositorio para buscar todos los usuarios que han usado una IP
            context.getSource().sendSuccess(() -> Component.literal("§c[PENDIENTE] Búsqueda de IP " + ipAddress + " (implementar método en repositorio)"), false);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al buscar IP: " + e.getMessage()));
            return 0;
        }
    }

    private static int globalBlockIP(CommandContext<CommandSourceStack> context) {
        try {
            String ipAddress = StringArgumentType.getString(context, "ip");

            if (!isValidIP(ipAddress)) {
                context.getSource().sendFailure(Component.literal("§cFormato de IP inválido."));
                return 0;
            }

            // Implementar bloqueo global de IP
            context.getSource().sendSuccess(() -> Component.literal("§c[PENDIENTE] IP " + ipAddress + " bloqueada globalmente (implementar método)"), false);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al bloquear IP globalmente: " + e.getMessage()));
            return 0;
        }
    }

    private static int globalTrustIP(CommandContext<CommandSourceStack> context) {
        try {
            String ipAddress = StringArgumentType.getString(context, "ip");

            if (!isValidIP(ipAddress)) {
                context.getSource().sendFailure(Component.literal("§cFormato de IP inválido."));
                return 0;
            }

            // Implementar confianza global de IP
            context.getSource().sendSuccess(() -> Component.literal("§a[PENDIENTE] IP " + ipAddress + " marcada como confiable globalmente (implementar método)"), false);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al confiar IP globalmente: " + e.getMessage()));
            return 0;
        }
    }

    private static int showIPStats(CommandContext<CommandSourceStack> context) {
        try {
            // Implementar estadísticas generales de IPs
            context.getSource().sendSuccess(() -> Component.literal("§6=== Estadísticas Generales de IPs ==="), false);
            context.getSource().sendSuccess(() -> Component.literal("§c[PENDIENTE] Implementar estadísticas generales"), false);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError al mostrar estadísticas de IPs: " + e.getMessage()));
            return 0;
        }
    }

    // Métodos auxiliares
    private static String getValidationMessage(IPValidationResult result) {
        return switch (result) {
            case APPROVED -> "§a✓ IP aprobada";
            case BLOCKED -> "§c✗ IP bloqueada";
            case TOO_MANY_IPS -> "§c✗ Demasiadas IPs registradas";
            case TOO_FREQUENT_CHANGES -> "§c✗ Cambios de IP demasiado frecuentes";
            case REQUIRES_VERIFICATION -> "§e⚠ Requiere verificación";
            case MULTI_ACCOUNT_WARNING -> "§6⚠ Advertencia: múltiples cuentas detectadas";
            case MULTI_ACCOUNT_BLOCKED -> "§c✗ Bloqueada: múltiples cuentas";
            case TOO_MANY_IPS_WARNING -> "§6⚠ Advertencia: demasiadas IPs";
            case USER_NOT_FOUND -> "§c✗ Usuario no encontrado";
            default -> "§7? Estado desconocido";
        };
    }

    private static boolean isValidIP(String ip) {
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