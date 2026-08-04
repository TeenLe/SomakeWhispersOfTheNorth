package com.somake.wotn.registry;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.effect.FrozenEffect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(
            BuiltInRegistries.MOB_EFFECT, WhispersOfTheNorth.MODID);

    public static final DeferredHolder<MobEffect, FrozenEffect> FROZEN = MOB_EFFECTS.register(
            "frozen", FrozenEffect::new);

    private ModEffects() {
    }
}
