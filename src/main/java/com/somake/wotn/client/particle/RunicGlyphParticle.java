package com.somake.wotn.client.particle;

import com.mojang.math.Axis;
import com.somake.wotn.particle.RunicGlyphParticleData;

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
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public final class RunicGlyphParticle extends SingleQuadParticle {
    private final int targetEntityId;
    private final float verticalOffset;
    private final float baseScale;
    private final float baseAlpha;
    private final float spin;
    private final boolean horizontal;

    private RunicGlyphParticle(ClientLevel level, double x, double y, double z,
            RunicGlyphParticleData data) {
        super(level, x, y, z, Minecraft.getInstance().getAtlasManager().get(
                new SpriteId(TextureAtlas.LOCATION_PARTICLES, data.glyph())));
        this.targetEntityId = data.targetEntityId();
        this.verticalOffset = data.verticalOffset();
        this.baseScale = data.scale();
        this.baseAlpha = data.alpha();
        this.spin = data.spin();
        this.horizontal = data.horizontal();
        this.hasPhysics = false;
        this.setLifetime(data.lifetime());
        this.setColor(data.red(), data.green(), data.blue());
        this.setAlpha(data.alpha());
        this.quadSize = data.scale();
        this.roll = 0.0F;
        this.oRoll = 0.0F;
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
        if (target != null) {
            this.setPos(target.getX(), target.getY() + this.verticalOffset, target.getZ());
        }
        this.roll += this.spin;
    }

    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float partialTick) {
        float progress = Mth.clamp((this.age + partialTick) / this.lifetime, 0.0F, 1.0F);
        float envelope = Mth.sin(progress * Mth.PI);
        this.setAlpha(this.baseAlpha * Mth.clamp(envelope * 1.35F, 0.0F, 1.0F));
        this.quadSize = this.baseScale * (0.72F + envelope * 0.34F);

        Quaternionf rotation = new Quaternionf();
        if (this.horizontal) {
            rotation.rotateX(Mth.HALF_PI);
            rotation.rotateZ(Mth.lerp(partialTick, this.oRoll, this.roll));
        } else {
            rotation.set((Quaternionfc)camera.rotation());
            rotation.mul((Quaternionfc)Axis.ZP.rotation(Mth.lerp(partialTick, this.oRoll, this.roll)));
        }
        this.extractRotatedQuad(renderState, camera, rotation, partialTick);
        this.extractRotatedQuad(renderState, camera, new Quaternionf(rotation).rotateY(Mth.PI), partialTick);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return 15728880;
    }

    public static final class Provider implements ParticleProvider<RunicGlyphParticleData> {
        public Provider(SpriteSet ignored) {
        }

        @Override
        public Particle createParticle(RunicGlyphParticleData data, ClientLevel level,
                double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                RandomSource random) {
            return new RunicGlyphParticle(level, x, y, z, data);
        }
    }
}
