package com.somake.wotn.client.particle;

import com.somake.wotn.particle.SnowflakeParticleData;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class SnowflakeParticle extends SingleQuadParticle {
    private final SpriteSet sprites;
    private final float baseAlpha;
    private final float fadeOutFraction;
    private final boolean emissive;
    private final float spinSpeed;
    private final float swayPhase;
    private final double swayX;
    private final double swayZ;

    private SnowflakeParticle(ClientLevel level, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed,
            SnowflakeParticleData data, SpriteSet sprites) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;
        this.baseAlpha = Mth.clamp(data.alpha(), 0.0F, 1.0F);
        this.fadeOutFraction = Mth.clamp(data.fadeOutFraction(), 0.05F, 1.0F);
        this.emissive = data.emissive();
        this.hasPhysics = false;
        this.gravity = Mth.clamp(data.gravity(), -2.0F, 2.0F);
        this.friction = Mth.clamp(data.drag(), 0.0F, 1.0F);
        this.setLifetime(Math.max(2, data.lifetime()));
        this.setColor(
                Mth.clamp(data.red(), 0.0F, 1.0F),
                Mth.clamp(data.green(), 0.0F, 1.0F),
                Mth.clamp(data.blue(), 0.0F, 1.0F));
        this.setAlpha(this.baseAlpha);
        this.quadSize = Math.max(0.01F, data.scale()) * (0.86F + this.random.nextFloat() * 0.28F);
        this.setSize(this.quadSize, this.quadSize);
        this.setParticleSpeed(xSpeed, ySpeed, zSpeed);
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.spinSpeed = (this.random.nextBoolean() ? 1.0F : -1.0F)
                * (0.025F + this.random.nextFloat() * 0.045F);
        this.swayPhase = this.random.nextFloat() * Mth.TWO_PI;
        double swayAngle = this.random.nextDouble() * Math.PI * 2.0D;
        this.swayX = Math.cos(swayAngle);
        this.swayZ = Math.sin(swayAngle);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();
        if (!this.isAlive()) {
            return;
        }

        this.roll += this.spinSpeed;
        double sway = Math.sin(this.swayPhase + this.age * 0.42F) * 0.0025D;
        this.xd += this.swayX * sway;
        this.zd += this.swayZ * sway;
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float partialTick) {
        float progress = Mth.clamp((this.age + partialTick) / this.lifetime, 0.0F, 1.0F);
        float fadeStart = 1.0F - this.fadeOutFraction;
        float fade = progress <= fadeStart
                ? 1.0F
                : Mth.clamp((1.0F - progress) / this.fadeOutFraction, 0.0F, 1.0F);
        fade = fade * fade * (3.0F - 2.0F * fade);
        this.setAlpha(this.baseAlpha * fade);
        super.extract(renderState, camera, partialTick);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return this.emissive ? 15728880 : super.getLightCoords(partialTick);
    }

    public static final class Provider implements ParticleProvider<SnowflakeParticleData> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SnowflakeParticleData data, ClientLevel level,
                double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
            return new SnowflakeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, data, this.sprites);
        }
    }
}
