package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LeviathanAxeCooldownPayload(int remainingTicks, int totalTicks) implements CustomPacketPayload {
    public static final Type<LeviathanAxeCooldownPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "leviathan_axe_cooldown"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LeviathanAxeCooldownPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.remainingTicks());
                buf.writeVarInt(payload.totalTicks());
            },
            buf -> new LeviathanAxeCooldownPayload(buf.readVarInt(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
