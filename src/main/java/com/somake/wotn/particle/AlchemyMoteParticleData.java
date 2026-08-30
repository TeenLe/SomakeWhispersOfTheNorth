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

public record AlchemyMoteParticleData(
        Identifier sprite,
        int mode,
        int targetEntityId,
        int lifetime,
        float scale,
        float red,
        float green,
        float blue,
        float alpha,
        float radius,
        float angularSpeed,
        float verticalOffset,
        float phase,
        boolean emissive) implements ParticleOptions {

    public static final int ORBIT = 0;
    public static final int FREE = 1;
    public static final int CONVERGE = 2;

    public static final MapCodec<AlchemyMoteParticleData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("sprite").forGetter(AlchemyMoteParticleData::sprite),
            Codec.intRange(0, 2).fieldOf("mode").forGetter(AlchemyMoteParticleData::mode),
            Codec.INT.fieldOf("target_entity_id").forGetter(AlchemyMoteParticleData::targetEntityId),
            Codec.intRange(2, 200).fieldOf("lifetime").forGetter(AlchemyMoteParticleData::lifetime),
            Codec.floatRange(0.01F, 4.0F).fieldOf("scale").forGetter(AlchemyMoteParticleData::scale),
            Codec.floatRange(0.0F, 1.0F).fieldOf("red").forGetter(AlchemyMoteParticleData::red),
            Codec.floatRange(0.0F, 1.0F).fieldOf("green").forGetter(AlchemyMoteParticleData::green),
            Codec.floatRange(0.0F, 1.0F).fieldOf("blue").forGetter(AlchemyMoteParticleData::blue),
            Codec.floatRange(0.0F, 1.0F).fieldOf("alpha").forGetter(AlchemyMoteParticleData::alpha),
            Codec.floatRange(0.0F, 12.0F).fieldOf("radius").forGetter(AlchemyMoteParticleData::radius),
            Codec.floatRange(-2.0F, 2.0F).fieldOf("angular_speed").forGetter(AlchemyMoteParticleData::angularSpeed),
            Codec.floatRange(-8.0F, 8.0F).fieldOf("vertical_offset").forGetter(AlchemyMoteParticleData::verticalOffset),
            Codec.FLOAT.fieldOf("phase").forGetter(AlchemyMoteParticleData::phase),
            Codec.BOOL.fieldOf("emissive").forGetter(AlchemyMoteParticleData::emissive))
            .apply(instance, AlchemyMoteParticleData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyMoteParticleData> STREAM_CODEC = StreamCodec.of(
            (buffer, data) -> {
                buffer.writeIdentifier(data.sprite());
                buffer.writeVarInt(data.mode());
                buffer.writeVarInt(data.targetEntityId());
                buffer.writeVarInt(data.lifetime());
                buffer.writeFloat(data.scale());
                buffer.writeFloat(data.red());
                buffer.writeFloat(data.green());
                buffer.writeFloat(data.blue());
                buffer.writeFloat(data.alpha());
                buffer.writeFloat(data.radius());
                buffer.writeFloat(data.angularSpeed());
                buffer.writeFloat(data.verticalOffset());
                buffer.writeFloat(data.phase());
                buffer.writeBoolean(data.emissive());
            },
            buffer -> new AlchemyMoteParticleData(
                    buffer.readIdentifier(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readBoolean()));

    @Override
    public ParticleType<AlchemyMoteParticleData> getType() {
        return ModParticles.ALCHEMY_MOTE.get();
    }

    public static final class Type extends ParticleType<AlchemyMoteParticleData> {
        public Type(boolean overrideLimiter) {
            super(overrideLimiter);
        }

        @Override
        public MapCodec<AlchemyMoteParticleData> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, AlchemyMoteParticleData> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
