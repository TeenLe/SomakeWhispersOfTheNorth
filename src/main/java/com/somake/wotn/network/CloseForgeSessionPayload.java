package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CloseForgeSessionPayload(UUID sessionId) implements CustomPacketPayload {
    public static final Type<CloseForgeSessionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "close_forge_session"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CloseForgeSessionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> buf.writeUUID(value.sessionId()),
            buf -> new CloseForgeSessionPayload(buf.readUUID()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
