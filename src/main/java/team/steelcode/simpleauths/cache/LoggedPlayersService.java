package team.steelcode.simpleauths.cache;


import net.minecraft.core.BlockPos;

import java.util.*;

public class LoggedPlayersService {

    private static Map<String, BlockPos> UNLOGGED_PLAYERS;
    private static Set<String> LOGGED_PLAYERS;

    public static void initialize() {
        UNLOGGED_PLAYERS = new HashMap<>();
        LOGGED_PLAYERS = new HashSet<>();
    }

    public static void addUnloggedPlayer(String uuid, BlockPos pos) {
        if (uuid != null && !isPlayerLogged(uuid)) {
            UNLOGGED_PLAYERS.put(uuid, pos);
        }
    }

    public static void loginPlayer(String uuid) {
        if (uuid != null) {
            UNLOGGED_PLAYERS.remove(uuid);
            LOGGED_PLAYERS.add(uuid);
        }
    }

    public static void updatePlayerPos(String uuid, BlockPos pos) {
        if (uuid != null && isPlayerUnlogged(uuid)) {
            UNLOGGED_PLAYERS.put(uuid, pos);
        }
    }

    public static BlockPos getPlayerPos(String uuid) {
        if (uuid != null && isPlayerUnlogged(uuid)) {
            return UNLOGGED_PLAYERS.get(uuid);
        }
        return null;
    }

    public static void removePlayer(String uuid) {
        if (uuid != null) {
            UNLOGGED_PLAYERS.remove(uuid);
            LOGGED_PLAYERS.remove(uuid);
        }
    }

    public static boolean isPlayerLogged(String uuid) {
        return uuid != null && LOGGED_PLAYERS.contains(uuid);
    }

    public static boolean isPlayerUnlogged(String uuid) {
        return uuid != null && UNLOGGED_PLAYERS.containsKey(uuid);
    }

    public static boolean isPlayerPresent(String uuid) {
        return isPlayerLogged(uuid) || isPlayerUnlogged(uuid);
    }

    public static List<String> getLoggedPlayers() {
        return new ArrayList<>(LOGGED_PLAYERS);
    }

    public static List<String> getUnloggedPlayers() {
        return new ArrayList<>(UNLOGGED_PLAYERS.keySet());
    }

    public static List<String> getAllPlayers() {
        List<String> allPlayers = new ArrayList<>(LOGGED_PLAYERS);
        allPlayers.addAll(UNLOGGED_PLAYERS.keySet());
        return allPlayers;
    }

    public static int getLoggedPlayersCount() {
        return LOGGED_PLAYERS.size();
    }

    public static int getUnloggedPlayersCount() {
        return UNLOGGED_PLAYERS.size();
    }

    public static int getTotalPlayersCount() {
        return LOGGED_PLAYERS.size() + UNLOGGED_PLAYERS.size();
    }

    public static void clearAllPlayers() {
        LOGGED_PLAYERS.clear();
        UNLOGGED_PLAYERS.clear();
    }

    public static void clearLoggedPlayers() {
        LOGGED_PLAYERS.clear();
    }

    public static void clearUnloggedPlayers() {
        UNLOGGED_PLAYERS.clear();
    }

    public static boolean hasLoggedPlayers() {
        return !LOGGED_PLAYERS.isEmpty();
    }

    public static boolean hasUnloggedPlayers() {
        return !UNLOGGED_PLAYERS.isEmpty();
    }

    public static boolean hasPlayers() {
        return hasLoggedPlayers() || hasUnloggedPlayers();
    }
}