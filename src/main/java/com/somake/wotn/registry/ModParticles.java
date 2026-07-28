package com.somake.wotn.registry;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.client.particle.ImpactRingParticle;
import com.somake.wotn.particle.ImpactRingParticleData;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, WhispersOfTheNorth.MODID);

    public static final DeferredHolder<ParticleType<?>, ImpactRingParticleData.Type> IMPACT_RING = PARTICLE_TYPES.register("impact_ring",
            () -> new ImpactRingParticleData.Type(false));

    private ModParticles() {
    }
}
