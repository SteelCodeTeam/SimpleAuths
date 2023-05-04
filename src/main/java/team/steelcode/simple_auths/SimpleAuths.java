package team.steelcode.simple_auths;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import team.steelcode.simple_auths.configs.JdbcConnections;
import team.steelcode.simple_auths.data.PlayerCache;
import team.steelcode.simple_auths.modules.events.ServerEventHandler;
import team.steelcode.simple_auths.setup.ModCommandsRegister;

import java.io.IOException;

// The value here shouldmatch an entry in the META-INF/mods.toml file
@Mod(SimpleAuths.MOD_ID)
public class SimpleAuths {

    public static final String MOD_ID = "simple_auths";
    public static final Logger LOGGER = LogUtils.getLogger();


    public SimpleAuths() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new ServerEventHandler());
    }

    @SubscribeEvent
    public void onCommandsRegister(RegisterCommandsEvent event){
        ModCommandsRegister.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) throws IOException {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
        JdbcConnections.initializeSessionFactory();
        PlayerCache.initialize();

    }
}
