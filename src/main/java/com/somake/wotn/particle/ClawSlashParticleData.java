package com.somake.wotn.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.somake.wotn.registry.ModParticles;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record ClawSlashParticleData(float yaw) implements ParticleOptions {
    public static final MapCodec<ClawSlashParticleData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("yaw").forGetter(ClawSlashParticleData::yaw))
            .apply(instance, ClawSlashParticleData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClawSlashParticleData> STREAM_CODEC = StreamCodec.of(
            (buffer, data) -> buffer.writeFloat(data.yaw()),
            buffer -> new ClawSlashParticleData(buffer.readFloat()));

    @Override
    public ParticleType<ClawSlashParticleData> getType() {
        return ModParticles.CLAW_SLASH.get();
    }

    public static final class Type extends ParticleType<ClawSlashParticleData> {
        public Type(boolean overrideLimiter) {
            super(overrideLimiter);
        }

        @Override
        public MapCodec<ClawSlashParticleData> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, ClawSlashParticleData> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
