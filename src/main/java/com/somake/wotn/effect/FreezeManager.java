package com.somake.wotn.effect;

import com.somake.wotn.registry.ModEffects;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class FreezeManager {
    public static final int DEFAULT_DURATION_TICKS = 60;
    public static final float SOUND_VOLUME = 0.28F;
    private static final Map<LivingEntity, FrozenState> FROZEN_STATES = new WeakHashMap<>();

    public static void freeze(LivingEntity target, LivingEntity source) {
        if (target.level().isClientSide() || !target.isAlive()) {
            return;
        }

        boolean newlyFrozen = !FROZEN_STATES.containsKey(target);
        FROZEN_STATES.computeIfAbsent(target, ignored -> FrozenState.capture(target));
        target.addEffect(new MobEffectInstance(ModEffects.FROZEN, DEFAULT_DURATION_TICKS, 0, false, false, false), source);
        if (newlyFrozen) {
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    com.somake.wotn.registry.ModSounds.FREEZE.get(), SoundSource.HOSTILE, SOUND_VOLUME, 0.9F);
            if (target.level() instanceof ServerLevel serverLevel) {
                LeviathanAxeEffects.spawnFreeze(serverLevel, target);
            }
        }
        lock(target);
    }

    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) {
            return;
        }

        if (isFrozen(living)) {
            FROZEN_STATES.computeIfAbsent(living, ignored -> FrozenState.capture(living));
            lock(living);
        } else {
            thaw(living);
        }
    }

    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) {
            return;
        }

        if (isFrozen(living)) {
            lock(living);
        } else {
            thaw(living);
        }
    }

    private static boolean isFrozen(LivingEntity living) {
        return living.hasEffect(ModEffects.FROZEN);
    }

    private static void lock(LivingEntity living) {
        FrozenState state = FROZEN_STATES.get(living);
        if (state == null) {
            return;
        }

        living.setPos(state.position.x, state.position.y, state.position.z);
        living.setDeltaMovement(Vec3.ZERO);
        living.setTicksFrozen(living.getTicksRequiredToFreeze());
        living.setXRot(state.xRot);
        living.setYRot(state.yRot);
        living.setJumping(false);
        living.setSpeed(0.0F);
        if (living instanceof Mob mob) {
            mob.stopInPlace();
        }
    }

    private static void thaw(LivingEntity living) {
        FrozenState state = FROZEN_STATES.remove(living);
        if (state == null) {
            return;
        }

        living.setDeltaMovement(Vec3.ZERO);
        living.setTicksFrozen(0);
        living.setSpeed(state.speed);
        if (living instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        living.level().playSound(null, living.getX(), living.getY(), living.getZ(),
                com.somake.wotn.registry.ModSounds.UNFREEZE.get(), SoundSource.HOSTILE, SOUND_VOLUME, 1.0F);
        if (living.level() instanceof ServerLevel serverLevel) {
            LeviathanAxeEffects.spawnShatter(serverLevel, living, SoundSource.HOSTILE);
        }
    }

    private record FrozenState(Vec3 position, float xRot, float yRot, float speed) {
        private static FrozenState capture(LivingEntity living) {
            return new FrozenState(living.position(), living.getXRot(), living.getYRot(), living.getSpeed());
        }
    }

    private FreezeManager() {
    }
}
