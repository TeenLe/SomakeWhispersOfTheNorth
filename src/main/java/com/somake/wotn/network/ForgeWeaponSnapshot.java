package com.somake.wotn.network;

import com.somake.wotn.skilltree.WeaponSkillProgress;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record ForgeWeaponSnapshot(UUID weaponId, int inventorySlot, ItemStack stack,
        WeaponSkillProgress progress, int primarySkill, int secondarySkill) {
    public static final StreamCodec<RegistryFriendlyByteBuf, ForgeWeaponSnapshot> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUUID(value.weaponId());
                buf.writeVarInt(value.inventorySlot());
                ItemStack.STREAM_CODEC.encode(buf, value.stack());
                WeaponSkillProgress.STREAM_CODEC.encode(buf, value.progress());
                buf.writeVarInt(value.primarySkill());
                buf.writeVarInt(value.secondarySkill());
            }, buf -> new ForgeWeaponSnapshot(buf.readUUID(), buf.readVarInt(),
                    ItemStack.STREAM_CODEC.decode(buf), WeaponSkillProgress.STREAM_CODEC.decode(buf),
                    buf.readVarInt(), buf.readVarInt()));
}
