package com.somake.wotn.effect;

import com.somake.wotn.network.CameraShakeDispatcher;
import com.somake.wotn.particle.ImpactRingParticleData.RingBehavior;
import com.somake.wotn.particle.ParticleHelper;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class LeviathanAxeEffects {
    private static final BlockParticleOption ICE_FRAGMENT = new BlockParticleOption(
            ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState());

    public static void spawnThrow(ServerLevel level, Entity source, Vec3 direction) {
        Vec3 origin = source.getEyePosition().add(direction.normalize().scale(0.65D));
        level.sendParticles(ParticleTypes.SNOWFLAKE, origin.x, origin.y, origin.z,
                8, 0.18D, 0.18D, 0.18D, 0.025D);
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.28F, 1.55F);
    }

    public static void spawnImpact(ServerLevel level, Vec3 position, Vec3 incoming, boolean blockImpact) {
        Vec3 direction = incoming.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : incoming.normalize();
        float yaw = (float) Mth.atan2(direction.x, direction.z);
        float pitch = (float) -Mth.atan2(direction.y, direction.horizontalDistance());
        ParticleHelper.spawnImpactRing(level, position.x, position.y, position.z,
                yaw, pitch, 0.28F, 0.88F, 1.0F, 0.82F,
                0.58F, blockImpact ? 2.7F : 2.2F, blockImpact ? 12 : 10,
                false, RingBehavior.GROW);
        spawnIceShards(level, position, direction, blockImpact ? 9 : 7, blockImpact ? 0.42F : 0.34F);
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL, position.x, position.y, position.z,
                blockImpact ? 10 : 7, 0.3D, 0.25D, 0.3D, 0.08D);
        level.sendParticles(ParticleTypes.SNOWFLAKE, position.x, position.y, position.z,
                blockImpact ? 32 : 24,
                blockImpact ? 0.62D : 0.5D,
                blockImpact ? 0.52D : 0.42D,
                blockImpact ? 0.62D : 0.5D,
                blockImpact ? 0.18D : 0.14D);
        CameraShakeDispatcher.shake(level, position, blockImpact ? 13.0F : 10.0F,
                blockImpact ? 0.16F : 0.11F, blockImpact ? 7 : 5);
        level.playSound(null, position.x, position.y, position.z, SoundEvents.GLASS_HIT,
                SoundSource.PLAYERS, 0.42F, blockImpact ? 0.72F : 0.9F);
    }

    public static void spawnFreeze(ServerLevel level, Entity target) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        ParticleHelper.spawnImpactRing(level, center.x, center.y, center.z,
                0.22F, 0.82F, 1.0F, 0.5F, 0.48F,
                Math.max(1.1F, target.getBbWidth() * 1.15F), 9, RingBehavior.GROW_THEN_SHRINK);
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z,
                10, target.getBbWidth() * 0.45D, target.getBbHeight() * 0.4D,
                target.getBbWidth() * 0.45D, 0.02D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.HOSTILE, 0.24F, 1.7F);
    }

    public static void spawnImbuedHit(ServerLevel level, Entity target, Vec3 direction, boolean projectile) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
        Vec3 normal = direction.lengthSqr() > 1.0E-6D ? direction.normalize() : new Vec3(0.0D, 0.15D, 0.0D);
        spawnIceShards(level, center, normal, projectile ? 9 : 6, projectile ? 0.38F : 0.3F);
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z,
                projectile ? 22 : 14, target.getBbWidth() * 0.38D, target.getBbHeight() * 0.24D,
                target.getBbWidth() * 0.38D, projectile ? 0.13D : 0.09D);
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL, center.x, center.y, center.z,
                projectile ? 7 : 4, 0.2D, 0.18D, 0.2D, 0.055D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_HIT,
                SoundSource.PLAYERS, 0.34F, projectile ? 1.15F : 1.35F);
    }

    public static void spawnShatter(ServerLevel level, Entity target, SoundSource source) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.45D, 0.0D);
        spawnIceShards(level, center, new Vec3(0.0D, 0.35D, 0.0D), 9, 0.3F);
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL, center.x, center.y, center.z,
                10, target.getBbWidth() * 0.45D, target.getBbHeight() * 0.3D,
                target.getBbWidth() * 0.45D, 0.08D);
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z,
                12, target.getBbWidth() * 0.55D, target.getBbHeight() * 0.4D,
                target.getBbWidth() * 0.55D, 0.04D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GLASS_BREAK,
                source, 0.34F, 1.25F);
    }

    private static void spawnIceShards(ServerLevel level, Vec3 position, Vec3 normal, int count, float strength) {
        var randomSource = level.getRandom();
        for (int i = 0; i < count; i++) {
            Vec3 random = new Vec3(randomSource.nextGaussian(), Math.abs(randomSource.nextGaussian()) + 0.25D,
                    randomSource.nextGaussian()).normalize();
            Vec3 velocity = random.scale(strength * (0.55D + randomSource.nextDouble() * 0.65D))
                    .add(normal.scale(strength * 0.35D));
            level.sendParticles(ICE_FRAGMENT, position.x, position.y, position.z,
                    0, velocity.x, velocity.y, velocity.z, 1.0D);
        }
    }

    private LeviathanAxeEffects() {
    }
}
