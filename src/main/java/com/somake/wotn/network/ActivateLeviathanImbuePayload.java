package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ActivateLeviathanImbuePayload() implements CustomPacketPayload {
    public static final ActivateLeviathanImbuePayload INSTANCE = new ActivateLeviathanImbuePayload();
    public static final Type<ActivateLeviathanImbuePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "activate_leviathan_imbue"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ActivateLeviathanImbuePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
