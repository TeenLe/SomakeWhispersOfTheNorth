package com.somake.wotn.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record AlchemyRuneSnapshot(String id, ItemStack stack, int slots, int owned) {
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyRuneSnapshot> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUtf(value.id(), 64);
                ItemStack.STREAM_CODEC.encode(buf, value.stack());
                buf.writeVarInt(value.slots());
                buf.writeVarInt(value.owned());
            }, buf -> new AlchemyRuneSnapshot(buf.readUtf(64), ItemStack.STREAM_CODEC.decode(buf),
                    buf.readVarInt(), buf.readVarInt()));

    public AlchemyRuneSnapshot {
        stack = stack.copyWithCount(1);
    }
}
