package team.steelcode.simple_auths.setup;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import team.steelcode.simple_auths.data.db.service.PlayerEntityDBService;
import team.steelcode.simple_auths.data.enums.IStatus;
import team.steelcode.simple_auths.data.enums.StatusType;

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
            context.getSource().getPlayer().sendSystemMessage(Component.literal(status.getDescription()));

            if (status.getStatus() == StatusType.KO_ERROR) {
                return -1;
            } else if (status.getStatus() == StatusType.KO_WARN) {
                return 0;
            } else {
                return 1;
            }

        } else {
            context.getSource().sendSystemMessage(Component.literal("Passwords mismatch, please, try it again."));
            return 1;
        }

    }

    private static int registerPlayer(CommandContext<CommandSourceStack> context, String password, String repeatedPassword) {
        if (password.equals(repeatedPassword)) {

            IStatus status = PlayerEntityDBService.registerUser(context.getSource().getPlayer().getScoreboardName(), password, context.getSource().getPlayer().getUUID());
            context.getSource().getPlayer().sendSystemMessage(Component.literal(status.getDescription()));

            if (status.getStatus() == StatusType.KO_ERROR) {
                return -1;
            } else if (status.getStatus() == StatusType.KO_WARN) {
                return 0;
            } else {
                return 1;
            }

        } else {
            context.getSource().sendSystemMessage(Component.literal("Passwords mismatch, please, try it again."));
            return 1;
        }
    }

    private static int loginPlayer(CommandContext<CommandSourceStack> context, String password) {

        IStatus status = PlayerEntityDBService.loginUser(context.getSource().getPlayer().getScoreboardName(), password);
        context.getSource().getPlayer().sendSystemMessage(Component.literal(status.getDescription()));


        if (status.getStatus() == StatusType.KO_ERROR) {
            return -1;
        } else if (status.getStatus() == StatusType.KO_WARN) {
            return 0;
        } else {
            return 1;
        }
    }
}
