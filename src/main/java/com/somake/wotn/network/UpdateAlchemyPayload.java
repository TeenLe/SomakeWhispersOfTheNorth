package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UpdateAlchemyPayload(OpenAlchemyPayload snapshot) implements CustomPacketPayload {
    public static final Type<UpdateAlchemyPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "update_alchemy"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateAlchemyPayload> STREAM_CODEC =
            OpenAlchemyPayload.STREAM_CODEC.map(UpdateAlchemyPayload::new, UpdateAlchemyPayload::snapshot);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
