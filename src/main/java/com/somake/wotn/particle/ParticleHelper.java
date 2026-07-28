package com.somake.wotn.particle;

import com.somake.wotn.particle.ImpactRingParticleData.RingBehavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public final class ParticleHelper {
    private ParticleHelper() {
    }

    public static RingParams ring(float red, float green, float blue, float scale, float radius) {
        return RingParams.of(red, green, blue, scale, radius);
    }

    public static void spawnImpactRing(Level level, double x, double y, double z,
            float red, float green, float blue,
            float scale, float radius, int duration) {
        spawnImpactRing(level, x, y, z, 0.0F, (float) (Math.PI / 2.0), red, green, blue, 1.0F, scale, radius, duration, false, RingBehavior.GROW);
    }

    public static void spawnImpactRing(Level level, double x, double y, double z,
            float red, float green, float blue, float alpha,
            float scale, float radius, int duration,
            RingBehavior behavior) {
        spawnImpactRing(level, x, y, z, 0.0F, (float) (Math.PI / 2.0), red, green, blue, alpha, scale, radius, duration, false, behavior);
    }

    public static void spawnImpactRing(Level level, double x, double y, double z,
            float yaw, float pitch,
            float red, float green, float blue, float alpha,
            float scale, float radius, int duration,
            boolean facesCamera, RingBehavior behavior) {
        ParticleOptions particle = new ImpactRingParticleData(yaw, pitch, duration, red, green, blue, alpha, scale, radius, facesCamera, behavior);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            return;
        }

        level.addParticle(particle, x, y, z, 0.0D, 0.0D, 0.0D);
    }

    public static void spawnGroundImpact(Level level, Entity entity,
            float forwardOffset, float sideOffset, double yOffset,
            float red, float green, float blue, float alpha,
            float scale, float radius, int duration,
            boolean spawnDebris, float debrisRadius, int debrisCount) {
        spawnGroundImpact(level, entity, forwardOffset, sideOffset, yOffset, red, green, blue, alpha, scale, radius, duration,
                false, RingBehavior.GROW, spawnDebris, debrisRadius, debrisCount);
    }

    public static void spawnGroundImpact(Level level, Entity entity,
            float forwardOffset, float sideOffset, double yOffset,
            float red, float green, float blue, float alpha,
            float scale, float radius, int duration,
            boolean facesCamera, RingBehavior behavior,
            boolean spawnDebris, float debrisRadius, int debrisCount) {
        float bodyYaw = entity.getYRot() * Mth.DEG_TO_RAD;
        float cos = Mth.cos(bodyYaw);
        float sin = Mth.sin(bodyYaw);
        double theta = bodyYaw + (Math.PI / 2.0D);
        double forwardX = Math.cos(theta);
        double forwardZ = Math.sin(theta);

        double spawnX = entity.getX() + forwardOffset * forwardX + cos * sideOffset;
        double spawnY = entity.getY() + yOffset;
        double spawnZ = entity.getZ() + forwardOffset * forwardZ + sin * sideOffset;

        if (spawnDebris) {
            spawnGroundDebris(level, spawnX, entity.getY(), spawnZ, debrisRadius, debrisCount);
        }

        spawnImpactRing(level, spawnX, spawnY, spawnZ, 0.0F, (float) (Math.PI / 2.0), red, green, blue, alpha, scale, radius, duration, facesCamera, behavior);
    }

    public static void spawnGroundDebris(Level level, double x, double y, double z, float spreadRadius, int count) {
        int particleCount = Math.max(1, count);
        for (int i = 0; i < particleCount; i++) {
            float angle = (float) ((Math.PI * 2.0D * i) / particleCount);
            double px = x + spreadRadius * Mth.sin(angle);
            double pz = z + spreadRadius * Mth.cos(angle);
            BlockPos hitPos = BlockPos.containing(px, y, pz);
            BlockState blockState = level.getBlockState(hitPos.below());
            if (blockState.getRenderShape() == RenderShape.INVISIBLE) {
                continue;
            }

            ParticleOptions debris = new BlockParticleOption(ParticleTypes.BLOCK, blockState);
            double motionX = level.getRandom().nextGaussian() * 0.07D;
            double motionY = 0.35D + level.getRandom().nextDouble() * 0.45D;
            double motionZ = level.getRandom().nextGaussian() * 0.07D;
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(debris, px, y + 0.15D, pz, 1, motionX, motionY, motionZ, 0.0D);
            } else {
                level.addParticle(debris, px, y + 0.15D, pz, motionX, motionY, motionZ);
            }
        }
    }

    public static void spawnGroundDebrisBurst(Level level, double x, double y, double z,
            float spreadRadius, int count, float outwardStrength) {
        int particleCount = Math.max(1, count);
        for (int i = 0; i < particleCount; i++) {
            float angle = (float) ((Math.PI * 2.0D * i) / particleCount + level.getRandom().nextGaussian() * 0.12D);
            float distance = spreadRadius * (0.35F + level.getRandom().nextFloat() * 0.65F);
            double directionX = Mth.sin(angle);
            double directionZ = Mth.cos(angle);
            double px = x + distance * directionX;
            double pz = z + distance * directionZ;
            BlockPos hitPos = BlockPos.containing(px, y, pz);
            BlockState blockState = level.getBlockState(hitPos.below());
            if (blockState.getRenderShape() == RenderShape.INVISIBLE) {
                continue;
            }

            ParticleOptions debris = new BlockParticleOption(ParticleTypes.BLOCK, blockState);
            double horizontalSpeed = outwardStrength * (0.65D + level.getRandom().nextDouble() * 0.7D);
            double motionX = directionX * horizontalSpeed + level.getRandom().nextGaussian() * 0.035D;
            double motionY = 0.45D + level.getRandom().nextDouble() * 0.55D;
            double motionZ = directionZ * horizontalSpeed + level.getRandom().nextGaussian() * 0.035D;
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(debris, px, y + 0.15D, pz, 0, motionX, motionY, motionZ, 1.0D);
            } else {
                level.addParticle(debris, px, y + 0.15D, pz, motionX, motionY, motionZ);
            }
        }
    }
}
