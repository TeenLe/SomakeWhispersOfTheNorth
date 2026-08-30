package com.somake.wotn.effect;

import com.somake.wotn.entity.FenrirEntity;
import com.somake.wotn.registry.ModAttachments;
import com.somake.wotn.registry.ModEffects;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class FenrirFrostManager {
    public static final int STACK_DURATION_TICKS = FrostStackManager.STACK_DURATION_TICKS;
    public static final int FULL_FREEZE_TICKS = FrostStackManager.FREEZE_DURATION_TICKS;
    public static final int POST_THAW_IMMUNITY_TICKS = FrostStackManager.POST_THAW_IMMUNITY_TICKS;

    public static void onDamageApplied(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0F || !event.getEntity().isAlive()
                || !(event.getSource().getEntity() instanceof FenrirEntity fenrir)) {
            return;
        }
        applyStack(event.getEntity(), fenrir);
    }

    public static void applyStack(LivingEntity target, FenrirEntity source) {
        if (target.level().isClientSide() || !target.isAlive() || target == source || source.isAlliedTo(target)) {
            return;
        }

        long now = now(target);
        FenrirFrostState state = normalize(target, target.getData(ModAttachments.FENRIR_FROST.get()), now);
        if (state.awaitingThaw() || state.immuneUntil() > now || FreezeManager.isFrozen(target)) {
            return;
        }

        FrostStackManager.Result result = FrostStackManager.apply(
                target, source, ModEffects.FENRIR_FROSTBITE, state.stacks(), true);
        if (result.blocked()) return;
        if (result.frozen()) {
            target.setData(ModAttachments.FENRIR_FROST.get(), new FenrirFrostState(0, 0L, true, 0L));
            return;
        }

        long expiresAt = now + STACK_DURATION_TICKS;
        target.setData(ModAttachments.FENRIR_FROST.get(),
                new FenrirFrostState(result.stacks(), expiresAt, false, 0L));
        FrostStackManager.spawnStackFeedback(target, result.stacks(), SoundSource.HOSTILE);
    }

    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) {
            return;
        }
        FenrirFrostState state = living.getExistingDataOrNull(ModAttachments.FENRIR_FROST.get());
        if (state == null) {
            return;
        }

        long now = now(living);
        FenrirFrostState normalized = normalize(living, state, now);
        if (normalized.awaitingThaw() && !FreezeManager.isFrozen(living)) {
            living.setData(ModAttachments.FENRIR_FROST.get(),
                    new FenrirFrostState(0, 0L, false, now + POST_THAW_IMMUNITY_TICKS));
            return;
        }
        if (normalized.stacks() == 0 && !normalized.awaitingThaw() && normalized.immuneUntil() <= now) {
            living.removeData(ModAttachments.FENRIR_FROST.get());
        } else if (!normalized.equals(state)) {
            living.setData(ModAttachments.FENRIR_FROST.get(), normalized);
        }
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof LivingEntity living && !living.level().isClientSide()) {
            onEntityTickPost(new EntityTickEvent.Post(living));
        }
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getEntity().removeData(ModAttachments.FENRIR_FROST.get());
            event.getEntity().removeEffect(ModEffects.FENRIR_FROSTBITE);
        }
    }

    private static FenrirFrostState normalize(LivingEntity target, FenrirFrostState state, long now) {
        if (state.stacks() > 0 && FreezeManager.isFrozen(target)) {
            target.removeEffect(ModEffects.FENRIR_FROSTBITE);
            return new FenrirFrostState(0, 0L, true, 0L);
        }
        if (state.stacks() > 0 && state.stacksExpireAt() <= now) {
            target.removeEffect(ModEffects.FENRIR_FROSTBITE);
            return FenrirFrostState.EMPTY;
        }
        return state;
    }

    private static long now(LivingEntity entity) {
        return entity.level().getServer() == null ? entity.level().getGameTime()
                : entity.level().getServer().overworld().getGameTime();
    }

    private FenrirFrostManager() {
    }
}
