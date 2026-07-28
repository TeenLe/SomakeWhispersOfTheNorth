package com.somake.wotn.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.somake.wotn.registry.ModParticles;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record ImpactRingParticleData(
        float yaw,
        float pitch,
        int duration,
        float red,
        float green,
        float blue,
        float alpha,
        float scale,
        float radius,
        boolean facesCamera,
        RingBehavior behavior) implements ParticleOptions {

    public static final MapCodec<ImpactRingParticleData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("yaw").forGetter(ImpactRingParticleData::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(ImpactRingParticleData::pitch),
            Codec.INT.fieldOf("duration").forGetter(ImpactRingParticleData::duration),
            Codec.FLOAT.fieldOf("red").forGetter(ImpactRingParticleData::red),
            Codec.FLOAT.fieldOf("green").forGetter(ImpactRingParticleData::green),
            Codec.FLOAT.fieldOf("blue").forGetter(ImpactRingParticleData::blue),
            Codec.FLOAT.fieldOf("alpha").forGetter(ImpactRingParticleData::alpha),
            Codec.FLOAT.fieldOf("scale").forGetter(ImpactRingParticleData::scale),
            Codec.FLOAT.fieldOf("radius").forGetter(ImpactRingParticleData::radius),
            Codec.BOOL.fieldOf("faces_camera").forGetter(ImpactRingParticleData::facesCamera),
            Codec.STRING.fieldOf("behavior").forGetter(data -> data.behavior().name()))
            .apply(instance, (yaw, pitch, duration, red, green, blue, alpha, scale, radius, facesCamera, behavior) -> new ImpactRingParticleData(
                    yaw,
                    pitch,
                    duration,
                    red,
                    green,
                    blue,
                    alpha,
                    scale,
                    radius,
                    facesCamera,
                    RingBehavior.valueOf(behavior))));

    public static final StreamCodec<RegistryFriendlyByteBuf, ImpactRingParticleData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                buf.writeFloat(data.yaw());
                buf.writeFloat(data.pitch());
                buf.writeVarInt(data.duration());
                buf.writeFloat(data.red());
                buf.writeFloat(data.green());
                buf.writeFloat(data.blue());
                buf.writeFloat(data.alpha());
                buf.writeFloat(data.scale());
                buf.writeFloat(data.radius());
                buf.writeBoolean(data.facesCamera());
                buf.writeEnum(data.behavior());
            },
            buf -> new ImpactRingParticleData(
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readVarInt(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readBoolean(),
                    buf.readEnum(RingBehavior.class)));

    @Override
    public ParticleType<ImpactRingParticleData> getType() {
        return ModParticles.IMPACT_RING.get();
    }

    public enum RingBehavior {
        GROW,
        SHRINK,
        CONSTANT,
        GROW_THEN_SHRINK
    }

    public static final class Type extends ParticleType<ImpactRingParticleData> {
        public Type(boolean overrideLimiter) {
            super(overrideLimiter);
        }

        @Override
        public MapCodec<ImpactRingParticleData> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, ImpactRingParticleData> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
