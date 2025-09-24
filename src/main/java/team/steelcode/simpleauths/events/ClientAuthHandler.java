package team.steelcode.simpleauths.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import team.steelcode.simpleauths.network.AuthSyncPacket;

@EventBusSubscriber(modid = "simple_auths")
public class ClientAuthHandler {

    public static boolean isLoggedIn = false;

    public static void handleAuthSync(AuthSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            isLoggedIn = packet.isLoggedIn();
        });
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0");

        registrar.playToClient(
                AuthSyncPacket.TYPE,
                AuthSyncPacket.STREAM_CODEC,
                ClientAuthHandler::handleAuthSync
        );
    }

    public static boolean isPlayerLoggedIn() {
        return isLoggedIn;
    }

    public static void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

}
