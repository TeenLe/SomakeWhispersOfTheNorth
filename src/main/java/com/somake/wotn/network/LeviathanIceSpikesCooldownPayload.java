package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LeviathanIceSpikesCooldownPayload(int remainingTicks, int totalTicks, boolean denied)
        implements CustomPacketPayload {
    public static final Type<LeviathanIceSpikesCooldownPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "leviathan_ice_spikes_cooldown"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LeviathanIceSpikesCooldownPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeVarInt(value.remainingTicks()); buf.writeVarInt(value.totalTicks()); buf.writeBoolean(value.denied()); },
            buf -> new LeviathanIceSpikesCooldownPayload(buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
