package team.steelcode.simpleauths.events;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import team.steelcode.simpleauths.cache.LoggedPlayersService;
import team.steelcode.simpleauths.nbt.PlayerLoggedNbt;
import team.steelcode.simpleauths.network.AuthSyncPacket;

@EventBusSubscriber(modid = "simple_auths")
public class NotLoggedPlayerEvents {

    private static boolean needsLogin(Player player) {
        return !LoggedPlayersService.isPlayerLogged(player.getStringUUID());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (needsLogin(player)) {
                LoggedPlayersService.addUnloggedPlayer(player.getStringUUID(), player.blockPosition());
                PlayerLoggedNbt.setPlayerLoggedIn(player, false);
                PacketDistributor.sendToPlayer(player, new AuthSyncPacket(false));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLeft(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LoggedPlayersService.removePlayer(player.getStringUUID());
            PacketDistributor.sendToPlayer(player, new AuthSyncPacket(false));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (needsLogin(player)) {
            Vec3 loginPos = getLoginPosition(player);
            if (player.position().distanceTo(loginPos) > 0.5) {
                player.teleportTo(loginPos.x, loginPos.y, loginPos.z);
            }

            player.setDeltaMovement(Vec3.ZERO);

            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0f);

            player.setHealth(player.getMaxHealth());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (needsLogin(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (needsLogin(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (needsLogin(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (needsLogin(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (needsLogin(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    // DAMAGE PREVENTION
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(ArmorHurtEvent event) {
        if (event.getEntity() instanceof Player player && needsLogin(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof Player player && needsLogin(player)) {
            event.setNewDamage(0);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (needsLogin(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        if (event.getPlayer() instanceof Player player && needsLogin(player)) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            if (needsLogin(player)) {
                player.getInventory().add(event.getEntity().getItem());
                player.containerMenu.broadcastChanges();
                event.setCanceled(true);
            }
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (needsLogin(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && needsLogin(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (needsLogin(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Player player && needsLogin(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerSleep(CanPlayerSleepEvent event) {
        if (needsLogin(event.getEntity())) {
            event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (needsLogin(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (needsLogin(event.getEntity())) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            Vec3 loginPos = getLoginPosition(player);
            player.teleportTo(loginPos.x, loginPos.y, loginPos.z);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCommandEvent(CommandEvent event) {
        if (event.getParseResults().getContext().getSource().getEntity() instanceof Player player) {
            if (needsLogin(player)) {
                String command = event.getParseResults().getReader().getString();
                if (!isAllowedCommand(command)) {
                    event.setCanceled(true);
                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.sendSystemMessage(Component.literal("You must login first!"));
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onChatEvent(ServerChatEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            if (needsLogin(player)) {
                if (!event.getMessage().getString().startsWith("/")) {
                    event.setCanceled(true);
                    player.sendSystemMessage(Component.literal("You must login first!"));
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerXpChange(PlayerXpEvent.XpChange event) {
        if (needsLogin(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLevelChange(PlayerXpEvent.LevelChange event) {
        if (needsLogin(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static boolean isAllowedCommand(String command) {
        return command.startsWith("login") ||
                command.startsWith("auth help") ||
                command.startsWith("register");
    }

    private static Vec3 getLoginPosition(ServerPlayer player) {
        BlockPos pos = LoggedPlayersService.getPlayerPos(player.getStringUUID());
        return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }
}