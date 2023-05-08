package team.steelcode.simple_auths.modules.server_data;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class ModModifiers {
    public static AttributeModifier NOT_LOGGED_SPEED_FLY = new AttributeModifier("not_logged_speed_modifier_fly", -1000D, AttributeModifier.Operation.MULTIPLY_TOTAL);

    public static AttributeModifier NOT_LOGGED_SPEED_WALK = new AttributeModifier("not_logged_speed_modifier_walk", -1000D, AttributeModifier.Operation.MULTIPLY_TOTAL);
}
