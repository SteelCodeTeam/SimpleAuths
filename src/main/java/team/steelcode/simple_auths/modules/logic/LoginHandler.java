package team.steelcode.simple_auths.modules.logic;

import com.google.common.collect.ImmutableMultimap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import team.steelcode.simple_auths.data.LoggedPlayerCache;
import team.steelcode.simple_auths.data.UnloggedPlayerCache;
import team.steelcode.simple_auths.data.db.entity.PlayerEntityDB;
import team.steelcode.simple_auths.data.db.service.PlayerEntityDBService;
import team.steelcode.simple_auths.data.enums.IStatus;
import team.steelcode.simple_auths.data.enums.StatusType;
import team.steelcode.simple_auths.modules.mod_modifier.ModModifiers;
import team.steelcode.simple_auths.setup.ConfigSpecRegister;

import java.util.ArrayList;
import java.util.List;

public class LoginHandler {


    public static IStatus login(ServerPlayer player, String username, String hashedPassword) {
        IStatus status = tryLogPlayer(username, hashedPassword);

        if (status.getStatus().equals(StatusType.OK) || status.getStatus().equals(StatusType.OK_WARN)) {
            setDefaultModifiers(player);
            LoggedPlayerCache.addPlayer(PlayerEntityDB.of(player));
            UnloggedPlayerCache.removePlayerByUIID(player.getUUID());
        }

        return status;
    }

    private static IStatus tryLogPlayer(String username, String hashedPassword) {
        return PlayerEntityDBService.loginUser(username, hashedPassword);
    }

    private static void setDefaultModifiers(ServerPlayer player) {
        player.setGameMode(getGameType());
        player.setInvulnerable(false);
    }


    private static GameType getGameType() {
        if (ConfigSpecRegister.DEFAULT_GAMETYPE.equals("adventure")) {
            return GameType.ADVENTURE;
        } else if (ConfigSpecRegister.DEFAULT_GAMETYPE.equals("survival")) {
            return GameType.SURVIVAL;
        } else if (ConfigSpecRegister.DEFAULT_GAMETYPE.equals("creative")) {
            return GameType.CREATIVE;
        } else if (ConfigSpecRegister.DEFAULT_GAMETYPE.equals("spectator")) {
            return GameType.SPECTATOR;
        } else {
            return GameType.DEFAULT_MODE;
        }
    }

}
