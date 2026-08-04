package com.somake.wotn.registry;

import com.mojang.serialization.Codec;
import com.somake.wotn.WhispersOfTheNorth;

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

    private ModDataComponents() {
    }
}
