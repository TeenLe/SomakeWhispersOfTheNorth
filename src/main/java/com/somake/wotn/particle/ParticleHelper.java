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
import net.minecraft.world.phys.Vec3;

public final class ParticleHelper {
    private ParticleHelper() {
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

    public static void spawnExpandingGroundDebris(Level level, double x, double y, double z,
            float waveRadius, int count, float outwardStrength) {
        int particleCount = Math.max(1, count);
        float angleOffset = level.getRandom().nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < particleCount; i++) {
            float angle = angleOffset + Mth.TWO_PI * i / particleCount
                    + (level.getRandom().nextFloat() - 0.5F) * 0.28F;
            float radiusJitter = 0.82F + level.getRandom().nextFloat() * 0.36F;
            double directionX = Mth.sin(angle);
            double directionZ = Mth.cos(angle);
            double px = x + waveRadius * radiusJitter * directionX;
            double pz = z + waveRadius * radiusJitter * directionZ;
            BlockPos hitPos = BlockPos.containing(px, y, pz);
            BlockState blockState = level.getBlockState(hitPos.below());
            if (blockState.getRenderShape() == RenderShape.INVISIBLE) {
                continue;
            }

            ParticleOptions debris = new BlockParticleOption(ParticleTypes.BLOCK, blockState);
            double horizontalSpeed = outwardStrength * (0.75D + level.getRandom().nextDouble() * 0.5D);
            double motionX = directionX * horizontalSpeed + level.getRandom().nextGaussian() * 0.025D;
            double motionY = 0.28D + level.getRandom().nextDouble() * 0.38D;
            double motionZ = directionZ * horizontalSpeed + level.getRandom().nextGaussian() * 0.025D;
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(debris, px, y + 0.12D, pz, 0, motionX, motionY, motionZ, 1.0D);
            } else {
                level.addParticle(debris, px, y + 0.12D, pz, motionX, motionY, motionZ);
            }
        }
    }

    public static void spawnGroundWaveTrail(Level level, BlockState blockState, BlockPos sourcePos,
            double x, double y, double z, float intensity) {
        if (!level.isClientSide() || blockState.isAir() || blockState.getRenderShape() == RenderShape.INVISIBLE) {
            return;
        }

        BlockParticleOption dust = new BlockParticleOption(ParticleTypes.DUST_PILLAR, blockState);
        int dustCount = 1 + Mth.floor(Mth.clamp(intensity, 0.0F, 1.0F) * 2.0F);
        for (int i = 0; i < dustCount; i++) {
            level.addParticle(dust,
                    x + level.getRandom().nextGaussian() * 0.45D,
                    y + 0.05D,
                    z + level.getRandom().nextGaussian() * 0.45D,
                    level.getRandom().nextGaussian() * 0.012D,
                    0.01D + level.getRandom().nextDouble() * 0.025D,
                    level.getRandom().nextGaussian() * 0.012D);
        }

        if (level.getRandom().nextFloat() < 0.3F + intensity * 0.35F) {
            BlockParticleOption fragment = new BlockParticleOption(ParticleTypes.BLOCK, blockState);
            Vec3 velocity = new Vec3(level.getRandom().nextGaussian() * 0.04D,
                    0.08D + level.getRandom().nextDouble() * 0.12D,
                    level.getRandom().nextGaussian() * 0.04D);
            level.addParticle(fragment, x, y + 0.08D, z, velocity.x, velocity.y, velocity.z);
        }
    }
}
