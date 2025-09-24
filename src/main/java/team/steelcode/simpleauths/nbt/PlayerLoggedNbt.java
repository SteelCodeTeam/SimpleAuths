package team.steelcode.simpleauths.nbt;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public class PlayerLoggedNbt {

    public static Optional<Boolean> isLoggedIn(Player player) {
        return player.getPersistentData().getBoolean("logged");
    }

    public static void setPlayerLoggedIn(Player player, boolean logged) {
        player.getPersistentData().putBoolean("logged", logged);
    }

}
