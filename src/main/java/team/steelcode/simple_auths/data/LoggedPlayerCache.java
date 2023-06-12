package team.steelcode.simple_auths.data;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import team.steelcode.simple_auths.data.db.entity.PlayerEntityDB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class LoggedPlayerCache {

    private static List<PlayerEntityDB> players;

    public static boolean playerIsLoggedByUUID(UUID uuid) {
        return players.stream().anyMatch(playerEntityDB -> playerEntityDB.getUuid() == uuid);
    }

    public static PlayerEntityDB getPlayerByUUID(UUID uuid) {
        return players.stream().filter(playerEntityDB -> playerEntityDB.getUuid() == uuid).findFirst().orElse(null);
    }

    public static boolean playerIsLoggedByUsername(String username) {
        return players.stream().anyMatch(playerEntityDB -> playerEntityDB.getUsername().equals(username));
    }

    public static void addPlayer(PlayerEntityDB player) {
        player.setHashedPassword(null);

        players.add(player);
    }

    public static void removePlayerByUsername(String username) {
        players = players.stream().filter(player -> !player.getUsername().equals(username)).collect(Collectors.toList());
    }

    public static void removePlayerByUUID(UUID uuid) {
        players = players.stream().filter(player -> !player.getUuid().equals(uuid)).collect(Collectors.toList());
    }

    public static void initialize() {
        players = new ArrayList<>();
    }
}
