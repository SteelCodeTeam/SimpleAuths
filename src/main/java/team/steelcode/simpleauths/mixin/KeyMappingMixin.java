package team.steelcode.simpleauths.mixin;


import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.steelcode.simpleauths.events.ClientAuthHandler;

@Mixin(KeyMapping.class)
public class KeyMappingMixin {

    @Shadow
    private InputConstants.Key key;


    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    private void onConsumeClick(CallbackInfoReturnable<Boolean> cir) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (!ClientAuthHandler.isPlayerLoggedIn()) {
            if (simpleAuths$isBlockedKey(this.key.getValue())) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void onIsDown(CallbackInfoReturnable<Boolean> cir) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        if (!ClientAuthHandler.isPlayerLoggedIn()) {
            if (simpleAuths$isBlockedKey(this.key.getValue())) {
                cir.setReturnValue(false);
            }
        }
    }

    @Unique
    private static boolean simpleAuths$isBlockedKey(int keyCode) {
        return Minecraft.getInstance().options.keyChat.getKey().getValue() != keyCode
                && Minecraft.getInstance().options.keyCommand.getKey().getValue() != keyCode;
    }
}