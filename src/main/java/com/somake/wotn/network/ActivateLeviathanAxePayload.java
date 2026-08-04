package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ActivateLeviathanAxePayload() implements CustomPacketPayload {
    public static final ActivateLeviathanAxePayload INSTANCE = new ActivateLeviathanAxePayload();
    public static final Type<ActivateLeviathanAxePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "activate_leviathan_axe"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ActivateLeviathanAxePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
