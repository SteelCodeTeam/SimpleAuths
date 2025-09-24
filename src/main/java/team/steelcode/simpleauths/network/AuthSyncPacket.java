package team.steelcode.simpleauths.network;


import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AuthSyncPacket(boolean isLoggedIn) implements CustomPacketPayload {

    public static final Type<AuthSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("simple_auths", "auth_sync")
    );

    public static final StreamCodec<ByteBuf, AuthSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            AuthSyncPacket::isLoggedIn,
            AuthSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
