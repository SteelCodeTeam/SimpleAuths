package team.steelcode.simple_auths.setup;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import team.steelcode.simple_auths.data.db.service.PlayerEntityDBService;
import team.steelcode.simple_auths.data.enums.IStatus;
import team.steelcode.simple_auths.data.enums.StatusType;

import java.util.Collection;

public class OpCommandsRegister {

    private static final LiteralArgumentBuilder<CommandSourceStack> unregisterCommand =
        Commands.literal("unregister").requires(commandSource -> commandSource.hasPermission(2))
            .then(Commands.argument("target", EntityArgument.players())
                .executes(context -> unregisterPlayers(context, EntityArgument.getPlayers(context, "target"))));


    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(unregisterCommand);
    }


    private static int unregisterPlayers(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        StringBuilder playerNames = new StringBuilder();
        IStatus status;

        for (ServerPlayer player: targets) {
            status = PlayerEntityDBService.unregisterPlayerByUsername(player.getScoreboardName());
            if (status.getStatus() == StatusType.KO_ERROR) {
                return -1;
            }

            playerNames.append(player.getScoreboardName()).append(", ");

            player.connection.disconnect(Component.literal("You were unregistered"));
        }

        playerNames.replace(playerNames.lastIndexOf(", "), playerNames.length() - 1, ".");

        context.getSource().getPlayer().sendSystemMessage(Component.literal("Players successfully unregistered: " + playerNames));

        return 1;

    }

}
