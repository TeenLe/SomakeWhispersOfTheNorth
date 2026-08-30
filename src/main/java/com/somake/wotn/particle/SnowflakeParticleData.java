package com.somake.wotn.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.somake.wotn.registry.ModParticles;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record SnowflakeParticleData(
        int lifetime,
        float scale,
        float red,
        float green,
        float blue,
        float alpha,
        float gravity,
        float drag,
        float fadeOutFraction,
        boolean emissive) implements ParticleOptions {

    public static final SnowflakeParticleData DEFAULT = new SnowflakeParticleData(
            24, 0.12F,
            0.62F, 0.94F, 1.0F, 0.95F,
            0.025F, 0.94F, 0.4F, true);

    public static final MapCodec<SnowflakeParticleData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(2, 1200).fieldOf("lifetime").forGetter(SnowflakeParticleData::lifetime),
            Codec.floatRange(0.01F, 8.0F).fieldOf("scale").forGetter(SnowflakeParticleData::scale),
            Codec.floatRange(0.0F, 1.0F).fieldOf("red").forGetter(SnowflakeParticleData::red),
            Codec.floatRange(0.0F, 1.0F).fieldOf("green").forGetter(SnowflakeParticleData::green),
            Codec.floatRange(0.0F, 1.0F).fieldOf("blue").forGetter(SnowflakeParticleData::blue),
            Codec.floatRange(0.0F, 1.0F).fieldOf("alpha").forGetter(SnowflakeParticleData::alpha),
            Codec.floatRange(-2.0F, 2.0F).fieldOf("gravity").forGetter(SnowflakeParticleData::gravity),
            Codec.floatRange(0.0F, 1.0F).fieldOf("drag").forGetter(SnowflakeParticleData::drag),
            Codec.floatRange(0.05F, 1.0F).fieldOf("fade_out_fraction").forGetter(SnowflakeParticleData::fadeOutFraction),
            Codec.BOOL.fieldOf("emissive").forGetter(SnowflakeParticleData::emissive))
            .apply(instance, SnowflakeParticleData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SnowflakeParticleData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                buf.writeVarInt(data.lifetime());
                buf.writeFloat(data.scale());
                buf.writeFloat(data.red());
                buf.writeFloat(data.green());
                buf.writeFloat(data.blue());
                buf.writeFloat(data.alpha());
                buf.writeFloat(data.gravity());
                buf.writeFloat(data.drag());
                buf.writeFloat(data.fadeOutFraction());
                buf.writeBoolean(data.emissive());
            },
            buf -> new SnowflakeParticleData(
                    buf.readVarInt(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readBoolean()));

    public SnowflakeParticleData withLifetime(int value) {
        return new SnowflakeParticleData(value, this.scale, this.red, this.green, this.blue, this.alpha,
                this.gravity, this.drag, this.fadeOutFraction, this.emissive);
    }

    public SnowflakeParticleData withScale(float value) {
        return new SnowflakeParticleData(this.lifetime, value, this.red, this.green, this.blue, this.alpha,
                this.gravity, this.drag, this.fadeOutFraction, this.emissive);
    }

    public SnowflakeParticleData withColor(float red, float green, float blue) {
        return new SnowflakeParticleData(this.lifetime, this.scale, red, green, blue, this.alpha,
                this.gravity, this.drag, this.fadeOutFraction, this.emissive);
    }

    public SnowflakeParticleData withAlpha(float value) {
        return new SnowflakeParticleData(this.lifetime, this.scale, this.red, this.green, this.blue, value,
                this.gravity, this.drag, this.fadeOutFraction, this.emissive);
    }

    public SnowflakeParticleData withMotion(float gravity, float drag) {
        return new SnowflakeParticleData(this.lifetime, this.scale, this.red, this.green, this.blue, this.alpha,
                gravity, drag, this.fadeOutFraction, this.emissive);
    }

    public SnowflakeParticleData withFadeOut(float fraction) {
        return new SnowflakeParticleData(this.lifetime, this.scale, this.red, this.green, this.blue, this.alpha,
                this.gravity, this.drag, fraction, this.emissive);
    }

    public SnowflakeParticleData withEmissive(boolean value) {
        return new SnowflakeParticleData(this.lifetime, this.scale, this.red, this.green, this.blue, this.alpha,
                this.gravity, this.drag, this.fadeOutFraction, value);
    }

    @Override
    public ParticleType<SnowflakeParticleData> getType() {
        return ModParticles.SNOWFLAKE.get();
    }

    public static final class Type extends ParticleType<SnowflakeParticleData> {
        public Type(boolean overrideLimiter) {
            super(overrideLimiter);
        }

        @Override
        public MapCodec<SnowflakeParticleData> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, SnowflakeParticleData> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
