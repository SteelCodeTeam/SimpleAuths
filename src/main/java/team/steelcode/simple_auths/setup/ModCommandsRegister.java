package team.steelcode.simple_auths.setup;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public class ModCommandsRegister {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        OpCommandsRegister.register(dispatcher);
        NormalCommandsRegister.register(dispatcher);
    }

}
