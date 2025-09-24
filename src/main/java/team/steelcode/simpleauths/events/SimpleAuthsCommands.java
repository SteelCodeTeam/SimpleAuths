package team.steelcode.simpleauths.events;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import team.steelcode.simpleauths.database.DatabaseConfig;
import team.steelcode.simpleauths.commands.AdminCommands;
import team.steelcode.simpleauths.commands.IPCommands;
import team.steelcode.simpleauths.commands.UserCommands;

import static team.steelcode.simpleauths.database.DatabaseConfig.*;
import static team.steelcode.simpleauths.database.DatabaseConfig.ipHistoryTableName;
import static team.steelcode.simpleauths.database.DatabaseConfig.userTableName;

@EventBusSubscriber(modid = "simple_auths")
public class SimpleAuthsCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        DatabaseConfig.initialize(jdbcUrl,username, password, userTableName, ipHistoryTableName, "MySQL");

        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // Registrar comandos de usuario
        UserCommands.register(dispatcher);

        // Registrar comandos de administrador
        AdminCommands.register(dispatcher);

        // Registrar comandos de IP (opcional)
        IPCommands.register(dispatcher);

        System.out.println("[SimpleAuths] Comandos registrados exitosamente");
    }
}