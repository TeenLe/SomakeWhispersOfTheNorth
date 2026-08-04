package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LeviathanImbueStatePayload(int activeTicks, int cooldownTicks, boolean denied)
        implements CustomPacketPayload {
    public static final Type<LeviathanImbueStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "leviathan_imbue_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LeviathanImbueStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.activeTicks());
                buf.writeVarInt(payload.cooldownTicks());
                buf.writeBoolean(payload.denied());
            },
            buf -> new LeviathanImbueStatePayload(buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
