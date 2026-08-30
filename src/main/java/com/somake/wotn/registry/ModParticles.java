package com.somake.wotn.registry;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.particle.ImpactRingParticleData;
import com.somake.wotn.particle.SnowflakeParticleData;
import com.somake.wotn.particle.ClawSlashParticleData;
import com.somake.wotn.particle.AlchemyMoteParticleData;
import com.somake.wotn.particle.RunicGlyphParticleData;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, WhispersOfTheNorth.MODID);

    public static final DeferredHolder<ParticleType<?>, ImpactRingParticleData.Type> IMPACT_RING = PARTICLE_TYPES.register("impact_ring",
            () -> new ImpactRingParticleData.Type(false));
    public static final DeferredHolder<ParticleType<?>, SnowflakeParticleData.Type> SNOWFLAKE = PARTICLE_TYPES.register("snowflake",
            () -> new SnowflakeParticleData.Type(false));
    public static final DeferredHolder<ParticleType<?>, ClawSlashParticleData.Type> CLAW_SLASH = PARTICLE_TYPES.register(
            "claw_slash", () -> new ClawSlashParticleData.Type(false));
    public static final DeferredHolder<ParticleType<?>, AlchemyMoteParticleData.Type> ALCHEMY_MOTE = PARTICLE_TYPES.register(
            "alchemy_mote", () -> new AlchemyMoteParticleData.Type(false));
    public static final DeferredHolder<ParticleType<?>, RunicGlyphParticleData.Type> RUNIC_GLYPH = PARTICLE_TYPES.register(
            "runic_glyph", () -> new RunicGlyphParticleData.Type(true));

    private ModParticles() {
    }
}
