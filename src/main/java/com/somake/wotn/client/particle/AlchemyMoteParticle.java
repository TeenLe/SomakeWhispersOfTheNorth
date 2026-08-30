package com.somake.wotn.client.particle;

import com.somake.wotn.particle.AlchemyMoteParticleData;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

public final class AlchemyMoteParticle extends SingleQuadParticle {
    private final int mode;
    private final int targetEntityId;
    private final float baseScale;
    private final float baseAlpha;
    private final float radius;
    private final float angularSpeed;
    private final float verticalOffset;
    private final float phase;
    private final boolean emissive;

    private AlchemyMoteParticle(ClientLevel level, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed,
            AlchemyMoteParticleData data) {
        super(level, x, y, z, Minecraft.getInstance().getAtlasManager().get(
                new SpriteId(TextureAtlas.LOCATION_PARTICLES, data.sprite())));
        this.mode = data.mode();
        this.targetEntityId = data.targetEntityId();
        this.baseScale = data.scale();
        this.baseAlpha = data.alpha();
        this.radius = data.radius();
        this.angularSpeed = data.angularSpeed();
        this.verticalOffset = data.verticalOffset();
        this.phase = data.phase();
        this.emissive = data.emissive();
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 0.94F;
        this.setLifetime(data.lifetime());
        this.setColor(data.red(), data.green(), data.blue());
        this.setAlpha(data.alpha());
        this.quadSize = data.scale();
        this.setParticleSpeed(xSpeed, ySpeed, zSpeed);
        this.roll = data.phase();
        this.oRoll = this.roll;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        if (++this.age >= this.lifetime) {
            this.remove();
            return;
        }

        Entity target = this.targetEntityId <= 0 ? null : this.level.getEntity(this.targetEntityId);
        if (this.mode == AlchemyMoteParticleData.ORBIT && target != null) {
            float angle = this.phase + this.age * this.angularSpeed;
            double pulse = 1.0D + Math.sin(this.age * 0.23F + this.phase) * 0.08D;
            double orbitRadius = this.radius * pulse;
            double bodyHeight = Math.max(0.4D, target.getBbHeight());
            this.setPos(target.getX() + Math.cos(angle) * orbitRadius,
                    target.getY() + bodyHeight * 0.5D + this.verticalOffset
                            + Math.sin(this.age * 0.19F + this.phase) * bodyHeight * 0.18D,
                    target.getZ() + Math.sin(angle) * orbitRadius);
        } else if (this.mode == AlchemyMoteParticleData.CONVERGE && target != null) {
            double targetY = target.getY() + target.getBbHeight() * 0.55D + this.verticalOffset;
            double pull = 0.08D + this.age / (double)this.lifetime * 0.12D;
            this.xd += (target.getX() - this.x) * pull;
            this.yd += (targetY - this.y) * pull;
            this.zd += (target.getZ() - this.z) * pull;
            this.xd *= 0.72D;
            this.yd *= 0.72D;
            this.zd *= 0.72D;
            this.move(this.xd, this.yd, this.zd);
        } else {
            this.yd += 0.002D;
            this.move(this.xd, this.yd, this.zd);
            this.xd *= this.friction;
            this.yd *= this.friction;
            this.zd *= this.friction;
        }
        this.roll += 0.06F + this.angularSpeed * 0.25F;
    }

    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float partialTick) {
        float progress = Mth.clamp((this.age + partialTick) / this.lifetime, 0.0F, 1.0F);
        float fadeIn = Mth.clamp(progress / 0.14F, 0.0F, 1.0F);
        float fadeOut = Mth.clamp((1.0F - progress) / 0.35F, 0.0F, 1.0F);
        this.setAlpha(this.baseAlpha * fadeIn * fadeOut);
        this.quadSize = this.baseScale * (0.82F + Mth.sin(progress * Mth.PI) * 0.28F);
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

    public static final class Provider implements ParticleProvider<AlchemyMoteParticleData> {
        public Provider(SpriteSet ignored) {
        }

        @Override
        public Particle createParticle(AlchemyMoteParticleData data, ClientLevel level,
                double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                RandomSource random) {
            return new AlchemyMoteParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, data);
        }
    }
}
