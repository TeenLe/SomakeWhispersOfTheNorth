package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CloseDialoguePayload(UUID sessionId) implements CustomPacketPayload {
    public static final Type<CloseDialoguePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "close_dialogue"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CloseDialoguePayload> STREAM_CODEC =
            StreamCodec.of((buf, value) -> buf.writeUUID(value.sessionId()),
                    buf -> new CloseDialoguePayload(buf.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
