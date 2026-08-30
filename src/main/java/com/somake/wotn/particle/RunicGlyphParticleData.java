package com.somake.wotn.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.somake.wotn.registry.ModParticles;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record RunicGlyphParticleData(
        Identifier glyph,
        int targetEntityId,
        int lifetime,
        float scale,
        float red,
        float green,
        float blue,
        float alpha,
        float verticalOffset,
        float spin,
        boolean horizontal) implements ParticleOptions {

    public static final MapCodec<RunicGlyphParticleData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("glyph").forGetter(RunicGlyphParticleData::glyph),
            Codec.INT.fieldOf("target_entity_id").forGetter(RunicGlyphParticleData::targetEntityId),
            Codec.intRange(2, 200).fieldOf("lifetime").forGetter(RunicGlyphParticleData::lifetime),
            Codec.floatRange(0.01F, 12.0F).fieldOf("scale").forGetter(RunicGlyphParticleData::scale),
            Codec.floatRange(0.0F, 1.0F).fieldOf("red").forGetter(RunicGlyphParticleData::red),
            Codec.floatRange(0.0F, 1.0F).fieldOf("green").forGetter(RunicGlyphParticleData::green),
            Codec.floatRange(0.0F, 1.0F).fieldOf("blue").forGetter(RunicGlyphParticleData::blue),
            Codec.floatRange(0.0F, 1.0F).fieldOf("alpha").forGetter(RunicGlyphParticleData::alpha),
            Codec.floatRange(-8.0F, 8.0F).fieldOf("vertical_offset").forGetter(RunicGlyphParticleData::verticalOffset),
            Codec.floatRange(-2.0F, 2.0F).fieldOf("spin").forGetter(RunicGlyphParticleData::spin),
            Codec.BOOL.fieldOf("horizontal").forGetter(RunicGlyphParticleData::horizontal))
            .apply(instance, RunicGlyphParticleData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RunicGlyphParticleData> STREAM_CODEC = StreamCodec.of(
            (buffer, data) -> {
                buffer.writeIdentifier(data.glyph());
                buffer.writeVarInt(data.targetEntityId());
                buffer.writeVarInt(data.lifetime());
                buffer.writeFloat(data.scale());
                buffer.writeFloat(data.red());
                buffer.writeFloat(data.green());
                buffer.writeFloat(data.blue());
                buffer.writeFloat(data.alpha());
                buffer.writeFloat(data.verticalOffset());
                buffer.writeFloat(data.spin());
                buffer.writeBoolean(data.horizontal());
            },
            buffer -> new RunicGlyphParticleData(
                    buffer.readIdentifier(), buffer.readVarInt(), buffer.readVarInt(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readBoolean()));

    @Override
    public ParticleType<RunicGlyphParticleData> getType() {
        return ModParticles.RUNIC_GLYPH.get();
    }

    public static final class Type extends ParticleType<RunicGlyphParticleData> {
        public Type(boolean overrideLimiter) {
            super(overrideLimiter);
        }

        @Override
        public MapCodec<RunicGlyphParticleData> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, RunicGlyphParticleData> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
