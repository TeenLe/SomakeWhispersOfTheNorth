package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ActivateLeviathanSlotPayload(int slot) implements CustomPacketPayload {
    public static final Type<ActivateLeviathanSlotPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "activate_leviathan_slot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ActivateLeviathanSlotPayload> STREAM_CODEC =
            StreamCodec.of((buf, value) -> buf.writeVarInt(value.slot()),
                    buf -> new ActivateLeviathanSlotPayload(buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
