package team.steelcode.simple_auths.modules.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import team.steelcode.simple_auths.data.PlayerCache;

import java.util.List;

import static net.minecraftforge.eventbus.api.EventPriority.HIGHEST;
import static net.minecraftforge.eventbus.api.EventPriority.LOWEST;

@Mod.EventBusSubscriber
public class ServerEventHandler {

    @SubscribeEvent
    public static void OnPlayerLoggedOutEvent(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (!event.getEntity().isLocalPlayer()) {
            PlayerCache.removePlayerByUsername(event.getEntity().getScoreboardName());
        }
    }

    @SubscribeEvent(priority = LOWEST)
    public static void onTickEvent(final TickEvent.LevelTickEvent event) {

        if (event.side != LogicalSide.SERVER) {
            return;
        }

        List<? extends Player> players = event.level.players();

        for (Player player: players) {
            if (player instanceof ServerPlayer serverPlayer) {

                if (!PlayerCache.playerIsLoggedByUUID(serverPlayer.getUUID())) {
                    serverPlayer.setGameMode(GameType.ADVENTURE);
                    player.setInvulnerable(true);
                    player.teleportTo(player.position().x, player.position().y, player.position().z);
                }
            }
        }
    }



    @SubscribeEvent(priority = HIGHEST)
    public static void onInteract(final PlayerInteractEvent event) {
        if (!event.getEntity().isLocalPlayer()) {
            if (!PlayerCache.playerIsLoggedByUUID(event.getEntity().getUUID())) {
                event.setCancellationResult(InteractionResult.FAIL);
            }
        }
    }

    @SubscribeEvent(priority = HIGHEST)
    public static void onContainerOpen(final PlayerContainerEvent.Open event) {
        Player player = event.getEntity();
        if(!PlayerCache.playerIsLoggedByUUID(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    // Using a block (right-click function)
    @SubscribeEvent(priority = HIGHEST)
    public static void onUseBlock(final PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if(!PlayerCache.playerIsLoggedByUUID(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    // Punching a block
    @SubscribeEvent(priority = HIGHEST)
    public static void onAttackBlock(final PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if(!PlayerCache.playerIsLoggedByUUID(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    // Using an item
    @SubscribeEvent(priority = HIGHEST)
    public static void onUseItem(final PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if(!PlayerCache.playerIsLoggedByUUID(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    // Attacking an entity
    @SubscribeEvent(priority = HIGHEST)
    public static void onAttackEntity(final AttackEntityEvent event) {
        Player player = event.getEntity();
        if(!PlayerCache.playerIsLoggedByUUID(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    // Interacting with entity
    @SubscribeEvent(priority = HIGHEST)
    public static void onUseEntity(final PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if(!PlayerCache.playerIsLoggedByUUID(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onChatEvent(final ServerChatEvent.Submitted event) {
        if(!PlayerCache.playerIsLoggedByUUID(event.getPlayer().getUUID())) {
            if (!event.getMessage().getString().startsWith("/login") || !event.getMessage().getString().startsWith("/register")) {
                event.setCanceled(true);
            }
        }
    }

}
