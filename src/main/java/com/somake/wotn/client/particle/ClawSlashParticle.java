package com.somake.wotn.client.particle;

import com.somake.wotn.particle.ClawSlashParticleData;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;

public final class ClawSlashParticle extends SingleQuadParticle {
    private static final int LIFETIME = 6;
    private static final float START_HALF_SIZE = 0.72F;
    private static final float END_HALF_SIZE = 1.2F;
    private static final float BASE_ALPHA = 0.78F;
    private final float yaw;

    private ClawSlashParticle(ClientLevel level, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed,
            ClawSlashParticleData data, SpriteSet sprites) {
        super(level, x, y, z, sprites.first());
        this.yaw = data.yaw();
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 0.86F;
        this.setLifetime(LIFETIME);
        this.setParticleSpeed(xSpeed, ySpeed, zSpeed);
        this.setColor(0.48F, 0.92F, 1.0F);
        this.setAlpha(BASE_ALPHA);
        this.quadSize = START_HALF_SIZE;
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return 15728880;
    }

    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float partialTick) {
        float progress = Mth.clamp((this.age + partialTick) / this.lifetime, 0.0F, 1.0F);
        float inverse = 1.0F - progress;
        float growth = 1.0F - inverse * inverse * inverse;
        float fade = inverse * inverse * (3.0F - 2.0F * inverse);
        this.quadSize = Mth.lerp(growth, START_HALF_SIZE, END_HALF_SIZE);
        this.setAlpha(BASE_ALPHA * fade);
        Quaternionf rotation = new Quaternionf().rotateY(this.yaw);
        this.extractRotatedQuad(renderState, camera, rotation, partialTick);
        this.extractRotatedQuad(renderState, camera,
                new Quaternionf(rotation).rotateY(Mth.PI), partialTick);
    }

    public static final class Provider implements ParticleProvider<ClawSlashParticleData> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ClawSlashParticleData data, ClientLevel level,
                double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                RandomSource random) {
            return new ClawSlashParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, data, this.sprites);
        }
    }
}
