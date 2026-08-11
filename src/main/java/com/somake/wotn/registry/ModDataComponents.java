package com.somake.wotn.registry;

import com.mojang.serialization.Codec;
import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.skilltree.WeaponSkillProgress;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(
            Registries.DATA_COMPONENT_TYPE, WhispersOfTheNorth.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> ICE_IMBUED_UNTIL =
            COMPONENTS.registerComponentType("ice_imbued_until",
                    builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LEVIATHAN_SECONDARY_SKILL =
            COMPONENTS.registerComponentType("leviathan_secondary_skill",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LEVIATHAN_PRIMARY_SKILL =
            COMPONENTS.registerComponentType("leviathan_primary_skill",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WeaponSkillProgress>> WEAPON_SKILL_PROGRESS =
            COMPONENTS.registerComponentType("weapon_skill_progress",
                    builder -> builder.persistent(WeaponSkillProgress.CODEC)
                            .networkSynchronized(WeaponSkillProgress.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> WEAPON_INSTANCE_ID =
            COMPONENTS.registerComponentType("weapon_instance_id",
                    builder -> builder.persistent(UUIDUtil.CODEC)
                            .networkSynchronized(net.minecraft.network.codec.StreamCodec.of(
                                    (net.minecraft.network.RegistryFriendlyByteBuf buf, UUID value) -> buf.writeUUID(value),
                                    (net.minecraft.network.RegistryFriendlyByteBuf buf) -> buf.readUUID())));

    private ModDataComponents() {
    }
}
