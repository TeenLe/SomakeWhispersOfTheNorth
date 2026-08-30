package com.somake.wotn.effect;

import com.somake.wotn.particle.ParticleHelper;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class FrostStackManager {
    public static final int MAX_STACKS = 5;
    public static final int STACK_DURATION_TICKS = 120;
    public static final int FREEZE_DURATION_TICKS = FreezeManager.DEFAULT_DURATION_TICKS;
    public static final int POST_THAW_IMMUNITY_TICKS = 40;

    public static Result apply(
            LivingEntity target,
            LivingEntity source,
            Holder<MobEffect> buildupEffect,
            int currentStacks,
            boolean freezeAtMaximum) {
        if (target.level().isClientSide() || !target.isAlive() || FreezeManager.isFrozen(target)) {
            return new Result(Math.clamp(currentStacks, 0, MAX_STACKS), false, true);
        }

        int normalizedStacks = Math.clamp(currentStacks, 0, MAX_STACKS);
        if (freezeAtMaximum && normalizedStacks >= MAX_STACKS) {
            boolean frozen = FreezeManager.freeze(target, source, FREEZE_DURATION_TICKS);
            if (frozen) {
                target.removeEffect(buildupEffect);
                return new Result(0, true, false);
            }
        }

        int stacks = Math.min(MAX_STACKS, normalizedStacks + 1);
        target.removeEffect(buildupEffect);
        target.addEffect(new MobEffectInstance(
                buildupEffect,
                STACK_DURATION_TICKS,
                stacks - 1,
                false,
                false,
                true), source);
        return new Result(stacks, false, false);
    }

    public static void spawnStackFeedback(LivingEntity target, int stacks, SoundSource soundSource) {
        if (!(target.level() instanceof ServerLevel level)) return;
        int normalizedStacks = Math.clamp(stacks, 1, MAX_STACKS);
        ParticleHelper.spawnLayeredSnowflakes(level, ParticleHelper.SNOWFLAKE_AURA,
                normalizedStacks == MAX_STACKS ? 0.52F : 0.38F,
                target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                2 + normalizedStacks * 2, target.getBbWidth() * 0.28D,
                target.getBbHeight() * 0.2D, target.getBbWidth() * 0.28D, 0.025D);
        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT,
                soundSource, normalizedStacks == MAX_STACKS ? 0.7F : 0.45F,
                0.78F + normalizedStacks * 0.11F);
    }

    public record Result(int stacks, boolean frozen, boolean blocked) {
    }

    private FrostStackManager() {
    }
}
