package com.somake.wotn.client.particle;

import com.mojang.math.Axis;
import com.somake.wotn.particle.ImpactRingParticleData;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public class ImpactRingParticle extends SingleQuadParticle {
    private final SpriteSet sprites;
    private final ImpactRingParticleData.RingBehavior behavior;
    private final boolean facesCamera;
    private final float yaw;
    private final float pitch;
    private final float targetRadius;
    private final float sizeMultiplier;
    private final float baseAlpha;

    public ImpactRingParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
            float yaw, float pitch, int duration, float red, float green, float blue, float alpha,
            float sizeMultiplier, float targetRadius, boolean facesCamera,
            ImpactRingParticleData.RingBehavior behavior, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd, sprites.first());
        this.sprites = sprites;
        this.behavior = behavior;
        this.facesCamera = facesCamera;
        this.yaw = yaw;
        this.pitch = pitch;
        this.targetRadius = targetRadius;
        this.sizeMultiplier = sizeMultiplier;
        this.baseAlpha = alpha;
        this.hasPhysics = false;
        this.setLifetime(Math.max(1, duration));
        this.setColor(red, green, blue);
        this.setAlpha(alpha);
        this.setSize(1.0F, 1.0F);
        this.setParticleSpeed(0.0D, 0.0D, 0.0D);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.bySprite(this.sprite);
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return 15728880;
    }

    @Override
    public ParticleRenderType getGroup() {
        return ParticleRenderType.SINGLE_QUADS;
    }

    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float partialTick) {
        float progress = Mth.clamp(((float) this.age + partialTick) / (float) this.lifetime, 0.0F, 1.0F);
        this.quadSize = Math.max(0.01F, this.targetRadius * this.sizeMultiplier * computeBehaviorScale(progress));
        this.setAlpha(this.baseAlpha * (0.95F * (1.0F - progress) + 0.05F));

        Quaternionf rotation = new Quaternionf();
        if (this.facesCamera) {
            rotation.set((Quaternionfc) camera.rotation());
            if (this.roll != 0.0F) {
                float rollValue = Mth.lerp(partialTick, this.oRoll, this.roll);
                rotation.mul((Quaternionfc) Axis.ZP.rotation(rollValue));
            }
        } else {
            rotation.rotateY(this.yaw);
            rotation.rotateX(this.pitch);
        }

        this.extractRotatedQuad(renderState, camera, rotation, partialTick);
        this.extractRotatedQuad(renderState, camera, new Quaternionf(rotation).rotateY((float) Math.PI), partialTick);
    }

    private float computeBehaviorScale(float progress) {
        return switch (this.behavior) {
            case GROW -> progress;
            case SHRINK -> 1.0F - progress;
            case CONSTANT -> 1.0F;
            case GROW_THEN_SHRINK -> progress <= 0.5F ? progress * 2.0F : (1.0F - progress) * 2.0F;
        };
    }

    public static class Provider implements ParticleProvider<ImpactRingParticleData> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ImpactRingParticleData data, ClientLevel level, double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
            return new ImpactRingParticle(level, x, y, z, xSpeed, ySpeed, zSpeed,
                    data.yaw(), data.pitch(), data.duration(), data.red(), data.green(), data.blue(), data.alpha(),
                    data.scale(), data.radius(), data.facesCamera(), data.behavior(), this.sprites);
        }
    }
}
