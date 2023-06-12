package team.steelcode.simple_auths.setup;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import team.steelcode.simple_auths.SimpleAuths;
import team.steelcode.simple_auths.data.db.service.PlayerEntityDBService;
import team.steelcode.simple_auths.data.enums.IStatus;
import team.steelcode.simple_auths.data.enums.StatusType;
import team.steelcode.simple_auths.data.language_providers.ModLanguageProviderES;
import team.steelcode.simple_auths.modules.logic.LoginHandler;
import team.steelcode.simple_auths.modules.logic.PasswordManager;

public class NormalCommandsRegister {


    private static final LiteralArgumentBuilder<CommandSourceStack> loginCommand =
        Commands.literal("login").requires(commandSource -> commandSource.hasPermission(0))
            .then(Commands.argument("password", StringArgumentType.word())
                .executes(context -> loginPlayer(context, StringArgumentType.getString(context, "password"))));


    private static final LiteralArgumentBuilder<CommandSourceStack> registerCommand =
        Commands.literal("register").requires(commandSource -> commandSource.hasPermission(0))
            .then(Commands.argument("password", StringArgumentType.word())
            .then(Commands.argument("repeated_password", StringArgumentType.word())
                .executes(context -> registerPlayer(context, StringArgumentType.getString(context, "password"), StringArgumentType.getString(context, "repeated_password")))));

    private static final LiteralArgumentBuilder<CommandSourceStack> changePasswordCommand =
        Commands.literal("change-password").requires(commandSource -> commandSource.hasPermission(0))
            .then(Commands.argument("password", StringArgumentType.word())
            .then(Commands.argument("repeated_password", StringArgumentType.word())
               .executes(context -> changePassword(context, StringArgumentType.getString(context, "password"), StringArgumentType.getString(context, "repeated_password")))));




    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(loginCommand);
        dispatcher.register(registerCommand);
        dispatcher.register(changePasswordCommand);

    }



    private static int changePassword(CommandContext<CommandSourceStack> context, String password, String repeatedPassword) {
        if (password.equals(repeatedPassword)) {
            IStatus status = PlayerEntityDBService.changePasswordFromUsername(context.getSource().getPlayer().getScoreboardName(), password);
            context.getSource().getPlayer().sendSystemMessage(Component.translatable(status.getDescription()));

            if (status.getStatus() == StatusType.KO_ERROR) {
                return -1;
            } else if (status.getStatus() == StatusType.KO_WARN) {
                return 0;
            } else {
                return 1;
            }

        } else {
            context.getSource().sendSystemMessage(Component.translatable(ModLanguageProviderES.PREFIX + "password_mismatch"));
            return 1;
        }

    }

    private static int registerPlayer(CommandContext<CommandSourceStack> context, String password, String repeatedPassword) {
        if (password.equals(repeatedPassword)) {

            String hashedPassword = PasswordManager.hashPassword(password);

            if (hashedPassword != null) {
                IStatus status = PlayerEntityDBService.registerUser(context.getSource().getPlayer().getScoreboardName(), hashedPassword, context.getSource().getPlayer().getUUID());
                context.getSource().getPlayer().sendSystemMessage(Component.translatable(status.getDescription()));

                if (status.getStatus() == StatusType.KO_ERROR) {
                    return -1;
                } else if (status.getStatus() == StatusType.KO_WARN) {
                    return 0;
                } else {
                    LoginHandler.login(context.getSource().getPlayer(), context.getSource().getPlayer().getScoreboardName(), hashedPassword);
                    return 1;
                }
            } else {
                context.getSource().getPlayer().sendSystemMessage(Component.translatable("No se ha podido establecer una seguridad cifrando la contraseña. Habla con un administrador."));
                return -1;
            }
        } else {
            context.getSource().sendSystemMessage(Component.translatable(ModLanguageProviderES.PREFIX + "password_mismatch"));
            return 1;
        }
    }

    private static int loginPlayer(CommandContext<CommandSourceStack> context, String password) {

        ServerPlayer player = context.getSource().getPlayer();

        String hashedPassword = PasswordManager.hashPassword(password);

        if (hashedPassword != null) {
            IStatus status = LoginHandler.login(player, player.getScoreboardName(), hashedPassword);
            context.getSource().getPlayer().sendSystemMessage(Component.translatable(status.getDescription()));

            if (status.getStatus() == StatusType.KO_ERROR) {
                SimpleAuths.LOGGER.error(status.getDescription());
                return -1;
            } else if (status.getStatus() == StatusType.KO_WARN) {
                SimpleAuths.LOGGER.warn(status.getDescription());
                return 0;
            } else {
                SimpleAuths.LOGGER.info(status.getDescription());
                return 1;
            }
        } else
            context.getSource().getPlayer().sendSystemMessage(Component.translatable("No se ha podido establecer una seguridad cifrando la contraseña. Habla con un administrador."));
            return -1;
    }
}
