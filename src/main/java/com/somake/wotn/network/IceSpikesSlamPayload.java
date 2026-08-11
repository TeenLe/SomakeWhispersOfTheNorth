package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record IceSpikesSlamPayload(int casterId, long startGameTime, boolean rightArm)
        implements CustomPacketPayload {
    public static final Type<IceSpikesSlamPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "ice_spikes_slam"));
    public static final StreamCodec<RegistryFriendlyByteBuf, IceSpikesSlamPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeVarInt(value.casterId());
                buf.writeVarLong(value.startGameTime());
                buf.writeBoolean(value.rightArm());
            },
            buf -> new IceSpikesSlamPayload(buf.readVarInt(), buf.readVarLong(), buf.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
