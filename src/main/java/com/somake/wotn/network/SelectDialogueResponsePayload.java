package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SelectDialogueResponsePayload(UUID sessionId, String responseId) implements CustomPacketPayload {
    public static final Type<SelectDialogueResponsePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "select_dialogue_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectDialogueResponsePayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUUID(value.sessionId());
                buf.writeUtf(value.responseId(), 128);
            }, buf -> new SelectDialogueResponsePayload(buf.readUUID(), buf.readUtf(128)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
