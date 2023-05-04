package team.steelcode.simple_auths.modules.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import team.steelcode.simple_auths.data.PlayerCache;

@Mod.EventBusSubscriber
public class ServerEventHandler {

    @SubscribeEvent
    public static void OnPlayerLoggedOutEvent(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (!event.getEntity().isLocalPlayer()) {
            PlayerCache.removePlayerByUsername(event.getEntity().getScoreboardName());
        }
    }

    @SubscribeEvent
    public static void onJoinWorld(final PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.getEntity().level.isClientSide) {
            if (event.getEntity() instanceof ServerPlayer) {
                Player player = event.getEntity();
                player.setSpeed(0);
                player.setInvulnerable(true);

            }
        }
    }

    @SubscribeEvent
    public static void onItemPickUp(final PlayerEvent.ItemPickupEvent event) {
        if (!event.getEntity().isLocalPlayer()) {
            if (!PlayerCache.playerIsLoggedByUUID(event.getEntity().getUUID())) {
                if (event.isCancelable()) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onChangeDimensionEvent(final PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!PlayerCache.playerIsLoggedByUUID(event.getEntity().getUUID())) {
            if (event.isCancelable()) {
                event.setCanceled(true);
                return;
            }
        }
    }

}
