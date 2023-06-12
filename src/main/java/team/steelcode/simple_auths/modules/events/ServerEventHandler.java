package team.steelcode.simple_auths.modules.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import team.steelcode.simple_auths.SimpleAuths;
import team.steelcode.simple_auths.data.LoggedPlayerCache;
import team.steelcode.simple_auths.data.UnloggedPlayerCache;
import team.steelcode.simple_auths.data.db.entity.PlayerEntityDB;
import team.steelcode.simple_auths.modules.mod_modifier.ModModifiers;

import java.util.List;

import static net.minecraftforge.eventbus.api.EventPriority.HIGHEST;
import static net.minecraftforge.eventbus.api.EventPriority.LOWEST;

@Mod.EventBusSubscriber
public class ServerEventHandler {

    @SubscribeEvent
    public static void onPlayerLoggedInServerEvent(final PlayerEvent.PlayerLoggedInEvent event) {
        if (!LoggedPlayerCache.playerIsLoggedByUUID(event.getEntity().getUUID())) {


            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                UnloggedPlayerCache.addPlayer(new PlayerEntityDB(
                        serverPlayer.getScoreboardName(), null, serverPlayer.getUUID(), serverPlayer.position(), true));
                serverPlayer.setGameMode(GameType.ADVENTURE);
                serverPlayer.setInvulnerable(true);
            }

        }
    }

    @SubscribeEvent
    public static void OnPlayerLoggedOutEvent(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (!event.getEntity().isLocalPlayer()) {
            LoggedPlayerCache.removePlayerByUsername(event.getEntity().getScoreboardName());
            UnloggedPlayerCache.removePlayerByUsername(event.getEntity().getScoreboardName());
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

                if (!LoggedPlayerCache.playerIsLoggedByUUID(serverPlayer.getUUID())) {

                    if (serverPlayer.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                        if (!serverPlayer.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(ModModifiers.WALK_SPEED)) {
                            serverPlayer.getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(ModModifiers.WALK_SPEED);
                        }
                    }

                    if (serverPlayer.getAttribute(Attributes.FLYING_SPEED) != null) {
                        if (!serverPlayer.getAttribute(Attributes.FLYING_SPEED).hasModifier(ModModifiers.FLY_SPEED)) {
                            serverPlayer.getAttribute(Attributes.FLYING_SPEED).addPermanentModifier(ModModifiers.FLY_SPEED);
                        }
                    }

                    try {
                        Vec3 vec = UnloggedPlayerCache.getPostionForPlayerUUID(serverPlayer.getUUID());
                        serverPlayer.teleportTo(vec.x, vec.y, vec.z);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else if (LoggedPlayerCache.playerIsLoggedByUUID(serverPlayer.getUUID())) {
                    if (LoggedPlayerCache.getPlayerByUUID(serverPlayer.getUUID()).getDebuffApplied()) {
                        LoggedPlayerCache.getPlayerByUUID(serverPlayer.getUUID()).setDebuffApplied(false);
                        if (serverPlayer.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                            if (serverPlayer.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(ModModifiers.WALK_SPEED)) {
                                serverPlayer.getAttribute(Attributes.MOVEMENT_SPEED).removeModifiers();
                            }
                        }
                        if (serverPlayer.getAttribute(Attributes.FLYING_SPEED) != null) {
                            if (serverPlayer.getAttribute(Attributes.FLYING_SPEED).hasModifier(ModModifiers.FLY_SPEED)) {
                                serverPlayer.getAttribute(Attributes.FLYING_SPEED).removeModifiers();
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = LOWEST)
    public static void LivingJumpEvent(final LivingEvent.LivingJumpEvent event) {
        if (!LoggedPlayerCache.playerIsLoggedByUUID(event.getEntity().getUUID())) {
            if (event.getEntity() instanceof Player player) {
                player.setJumping(false);
                player.setDeltaMovement(0, 0, 0);
                player.hasImpulse = false;
            }
        }
    }

    @SubscribeEvent(priority = HIGHEST)
    public static void onInteract(final PlayerInteractEvent event) {
        if (!event.getEntity().isLocalPlayer()) {
            if (!LoggedPlayerCache.playerIsLoggedByUUID(event.getEntity().getUUID())) {
                event.setCancellationResult(InteractionResult.FAIL);
            }
        }
    }

    @SubscribeEvent(priority = HIGHEST)
    public static void onContainerOpen(final PlayerContainerEvent.Open event) {
        Player player = event.getEntity();
        if (!LoggedPlayerCache.playerIsLoggedByUUID(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    // Using a block (right-click function)
    @SubscribeEvent(priority = HIGHEST)
    public static void onUseBlock(final PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!LoggedPlayerCache.playerIsLoggedByUUID(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    // Punching a block
    @SubscribeEvent(priority = HIGHEST)
    public static void onAttackBlock(final PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (!LoggedPlayerCache.playerIsLoggedByUUID(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    // Using an item
    @SubscribeEvent(priority = HIGHEST)
    public static void onUseItem(final PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!LoggedPlayerCache.playerIsLoggedByUUID(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    // Attacking an entity
    @SubscribeEvent(priority = HIGHEST)
    public static void onAttackEntity(final AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!LoggedPlayerCache.playerIsLoggedByUUID(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    // Interacting with entity
    @SubscribeEvent(priority = HIGHEST)
    public static void onUseEntity(final PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (!LoggedPlayerCache.playerIsLoggedByUUID(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onChatEvent(final ServerChatEvent.Submitted event) {
        if (!LoggedPlayerCache.playerIsLoggedByUUID(event.getPlayer().getUUID())) {
            if (!event.getMessage().getString().startsWith("/login") || !event.getMessage().getString().startsWith("/register")) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onItemPickUpEvent(final PlayerEvent.ItemPickupEvent event) {
        Player player = event.getEntity();

        if (!LoggedPlayerCache.playerIsLoggedByUUID(player.getUUID())) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                int slot = serverPlayer.getInventory().findSlotMatchingItem(event.getStack());
                serverPlayer.getInventory().removeItem(slot, event.getStack().getCount());
                serverPlayer.drop(event.getStack(), true);
            }
        }
    }

    @SubscribeEvent
    public static void OnItemDropEvent(final ItemTossEvent event) {
        if (!LoggedPlayerCache.playerIsLoggedByUUID(event.getPlayer().getUUID())) {

            if (event.isCancelable()) {
                ItemStack itemStack = event.getEntity().getItem();
                event.setCanceled(true);
                event.getPlayer().getInventory().add(itemStack);
            }
        }
    }
}
