package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CameraShakePayload(
        double x,
        double y,
        double z,
        float radius,
        float magnitude,
        int durationTicks) implements CustomPacketPayload {

    public static final Type<CameraShakePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "camera_shake"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CameraShakePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeDouble(payload.x());
                buf.writeDouble(payload.y());
                buf.writeDouble(payload.z());
                buf.writeFloat(payload.radius());
                buf.writeFloat(payload.magnitude());
                buf.writeVarInt(payload.durationTicks());
            },
            buf -> new CameraShakePayload(
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
