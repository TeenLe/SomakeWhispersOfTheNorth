package com.somake.wotn.alchemy;

import com.somake.wotn.effect.FreezeManager;
import com.somake.wotn.effect.FrostStackManager;
import com.somake.wotn.damage.IceDamage;
import com.somake.wotn.damage.AlchemyDamage;
import com.somake.wotn.registry.ModAttachments;
import com.somake.wotn.registry.ModAttributes;
import com.somake.wotn.registry.ModEffects;
import com.somake.wotn.particle.ParticleHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class NiflheimPotionEffects {
    private static final Identifier ICE_DAMAGE_MODIFIER = Identifier.fromNamespaceAndPath(
            com.somake.wotn.WhispersOfTheNorth.MODID, "niflheim_ice_damage");
    private static final Identifier FIRE_RESISTANCE_MODIFIER = Identifier.fromNamespaceAndPath(
            com.somake.wotn.WhispersOfTheNorth.MODID, "niflheim_fire_resistance");
    private static final Map<StackKey, StackState> STACKS = new HashMap<>();
    private static final Map<StackKey, Long> FREEZE_IMMUNITIES = new HashMap<>();
    private static final Set<StackKey> AWAITING_THAW = new HashSet<>();
    private static final int WHITEOUT_STACK_INTERVAL_TICKS = 30;
    private static final int WHITEOUT_STACK_DURATION_TICKS = FrostStackManager.STACK_DURATION_TICKS;
    private static final float WHITEOUT_PULSE_DAMAGE = 1.0F;
    private static final float WHITEOUT_MINIMUM_APPLIED_DAMAGE = 0.5F;
    private static final int HIT_STACK_DURATION_TICKS = FrostStackManager.STACK_DURATION_TICKS;
    private static final float HIT_MINIMUM_APPLIED_DAMAGE = 2.0F;
    private static final int POST_FREEZE_IMMUNITY_TICKS = FrostStackManager.POST_THAW_IMMUNITY_TICKS;

    public static void apply(LivingEntity entity, int tier, AlchemyPotionConfiguration configuration) {
        if (!(entity instanceof ServerPlayer player) || !configuration.isValidFor("niflheim", tier)) return;
        long expiresAt = now(player) + configuration.durationTicks("niflheim", tier);
        NiflheimEffectState state = new NiflheimEffectState(expiresAt, tier,
                configuration.runes());
        player.setData(ModAttachments.NIFLHEIM_EFFECT, state);
        player.removeEffect(ModEffects.NIFLHEIM_ESSENCE);
        player.addEffect(new MobEffectInstance(ModEffects.NIFLHEIM_ESSENCE,
                configuration.durationTicks("niflheim", tier), tier - 1, false, false, true));
        applyAttributeModifiers(player, state);
        if (player.level() instanceof ServerLevel level) {
            AlchemyVfx.activation(level, player, AlchemyVfx.NIFLHEIM, tier);
        }
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            double resistance = Math.clamp(player.getAttributeValue(ModAttributes.FIRE_DAMAGE_RESISTANCE), 0.0D, 0.8D);
            event.setAmount(event.getAmount() * (float)(1.0D - resistance));
        }
        ServerPlayer owner = IceDamage.owner(event.getSource());
        if (owner != null) {
            double bonus = Math.max(-1.0D, owner.getAttributeValue(ModAttributes.ICE_DAMAGE_BONUS));
            event.setAmount(event.getAmount() * (float)(1.0D + bonus));
        }
    }

    public static void onDamageApplied(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0F || !(event.getEntity() instanceof LivingEntity target)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer owner)
                || event.getSource().getDirectEntity() == null
                || AlchemyDamage.isSecondary(event.getSource())
                || event.getNewDamage() < HIT_MINIMUM_APPLIED_DAMAGE
                || owner == target || owner.isAlliedTo(target)) return;
        NiflheimEffectState state = activeState(owner);
        if (state == null) return;
        switch (state.special()) {
            case RIME_MARK -> addHitStack(owner, target, state, false);
            case ABSOLUTE_ZERO -> addHitStack(owner, target, state, true);
            default -> {
            }
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        long now = event.getServer().overworld().getGameTime();
        if (now % 10L != 0L) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            NiflheimEffectState state = activeState(player);
            if (state == null) {
                player.removeEffect(ModEffects.NIFLHEIM_ESSENCE);
                player.setData(ModAttachments.NIFLHEIM_EFFECT, NiflheimEffectState.INACTIVE);
                removeAttributeModifiers(player);
                continue;
            }
            reconcileIndicator(player, state);
            reconcileAttributeModifiers(player, state);
            if (Math.floorMod(now / 10L + player.getId(), 2L) == 0L
                    && player.level() instanceof ServerLevel level) {
                AlchemyVfx.aura(level, player, AlchemyVfx.NIFLHEIM, state.tier(),
                        state.configuration().potency("niflheim"), 0, state.expiresAt(),
                        state.configuration().durationTicks("niflheim", state.tier()));
            }
            if (state.special() == AlchemyPotionConfiguration.Special.FROST_MIST
                    || state.special() == AlchemyPotionConfiguration.Special.WHITEOUT) {
                applyMist(player, state);
            }
        }
        Iterator<Map.Entry<StackKey, StackState>> iterator = STACKS.entrySet().iterator();
        Set<UUID> changedTargets = new HashSet<>();
        while (iterator.hasNext()) {
            Map.Entry<StackKey, StackState> entry = iterator.next();
            LivingEntity target = findLivingEntity(event.getServer(), entry.getKey().target);
            if (entry.getValue().expiresAt <= now || target != null && FreezeManager.isFrozen(target)) {
                changedTargets.add(entry.getKey().target);
                if (target != null && FreezeManager.isFrozen(target)) AWAITING_THAW.add(entry.getKey());
                iterator.remove();
            }
        }
        Iterator<StackKey> thawIterator = AWAITING_THAW.iterator();
        while (thawIterator.hasNext()) {
            StackKey key = thawIterator.next();
            LivingEntity target = findLivingEntity(event.getServer(), key.target);
            if (target == null) continue;
            if (!target.isAlive()) {
                thawIterator.remove();
            } else if (!FreezeManager.isFrozen(target)) {
                FREEZE_IMMUNITIES.put(key, now + POST_FREEZE_IMMUNITY_TICKS);
                thawIterator.remove();
            }
        }
        FREEZE_IMMUNITIES.entrySet().removeIf(entry -> entry.getValue() <= now);
        changedTargets.forEach(targetId -> syncMarkEffect(event.getServer(), targetId, now));
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        NiflheimEffectState state = activeState(player);
        if (state == null) {
            player.removeEffect(ModEffects.NIFLHEIM_ESSENCE);
            removeAttributeModifiers(player);
        } else {
            reconcileIndicator(player, state);
            applyAttributeModifiers(player, state);
        }
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.isWasDeath()) {
            player.setData(ModAttachments.NIFLHEIM_EFFECT, NiflheimEffectState.INACTIVE);
            player.removeEffect(ModEffects.NIFLHEIM_ESSENCE);
        }
        removeAttributeModifiers(player);
        Set<UUID> affectedTargets = new HashSet<>();
        STACKS.entrySet().removeIf(entry -> {
            boolean remove = entry.getKey().owner.equals(player.getUUID());
            if (remove) affectedTargets.add(entry.getKey().target);
            return remove;
        });
        FREEZE_IMMUNITIES.keySet().removeIf(key -> key.owner.equals(player.getUUID()));
        AWAITING_THAW.removeIf(key -> key.owner.equals(player.getUUID()));
        long stackNow = now(player);
        affectedTargets.forEach(targetId -> syncMarkEffect(player.level().getServer(), targetId, stackNow));
        if (event.isWasDeath()) return;
        NiflheimEffectState state = activeState(player);
        if (state != null) applyAttributeModifiers(player, state);
    }

    private static void applyMist(ServerPlayer player, NiflheimEffectState state) {
        if (!(player.level() instanceof ServerLevel level)) return;
        AlchemyPotionConfiguration configuration = state.configuration();
        double radius = configuration.mistRadius();
        AABB area = player.getBoundingBox().inflate(radius, 1.5D, radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                target -> target != player && target.isAlive() && !player.isAlliedTo(target)
                        && player.canAttack(target))) {
            if (state.special() == AlchemyPotionConfiguration.Special.WHITEOUT) {
                addMistStack(player, target, state);
            } else {
                int amplifier = Math.min(2, configuration.potency("niflheim") / 2);
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, amplifier,
                        false, false, true), player);
            }
        }
        if (Math.floorMod(now(player) / 10L + player.getId(), 2L) == 0L) {
            ParticleHelper.spawnImpactRing(level, player.getX(), player.getY() + 0.06D, player.getZ(),
                    0.35F, 0.9F, 1.0F, state.special() == AlchemyPotionConfiguration.Special.WHITEOUT ? 0.72F : 0.48F,
                    1.0F, (float)radius, 22, com.somake.wotn.particle.ImpactRingParticleData.RingBehavior.CONSTANT);
            ParticleHelper.spawnLayeredSnowflakes(level, ParticleHelper.SNOWFLAKE_MIST, 0.5F,
                    player.getX(), player.getY() + 0.8D, player.getZ(),
                    3 + state.potencyCatalysts(), radius * 0.7D, 0.45D, radius * 0.7D, 0.008D);
        }
    }

    private static void addMistStack(ServerPlayer owner, LivingEntity target, NiflheimEffectState state) {
        StackKey key = new StackKey(owner.getUUID(), target.getUUID(), true);
        long now = now(owner);
        if (FreezeManager.isFrozen(target)) {
            clearStack(target, key, now);
            AWAITING_THAW.add(key);
            return;
        }
        if (FREEZE_IMMUNITIES.getOrDefault(key, 0L) > now) {
            clearStack(target, key, now);
            return;
        }
        StackState current = STACKS.getOrDefault(key,
                new StackState(0, now + WHITEOUT_STACK_DURATION_TICKS, now));
        if (current.expiresAt <= now) {
            current = new StackState(0, now + WHITEOUT_STACK_DURATION_TICKS, now);
        }
        if (current.nextStackAt > now) return;
        float healthBefore = target.getHealth();
        target.hurt(target.damageSources().freeze(), WHITEOUT_PULSE_DAMAGE);
        float appliedDamage = healthBefore - target.getHealth();
        if (appliedDamage < WHITEOUT_MINIMUM_APPLIED_DAMAGE) {
            STACKS.put(key, new StackState(current.stacks,
                    now + WHITEOUT_STACK_DURATION_TICKS, now + WHITEOUT_STACK_INTERVAL_TICKS));
            return;
        }
        FrostStackManager.Result result = FrostStackManager.apply(
                target, owner, ModEffects.NIFLHEIM_MARK, current.stacks, true);
        if (result.blocked()) {
            clearStack(target, key, now);
            AWAITING_THAW.add(key);
            return;
        }
        int stacks = result.stacks();
        AlchemyVfx.mark((ServerLevel)target.level(), target, AlchemyVfx.NIFLHEIM,
                AlchemyVfx.GLYPH_NIFLHEIM, Math.max(1, stacks), FrostStackManager.MAX_STACKS);
        if (result.frozen()) {
            beginFreezeCycle(target, key);
            AlchemyVfx.majorProc((ServerLevel)target.level(), target, AlchemyVfx.NIFLHEIM,
                    AlchemyVfx.GLYPH_NIFLHEIM, 2.0F);
        } else {
            STACKS.put(key, new StackState(stacks, now + WHITEOUT_STACK_DURATION_TICKS,
                    now + WHITEOUT_STACK_INTERVAL_TICKS));
            FrostStackManager.spawnStackFeedback(target, stacks, net.minecraft.sounds.SoundSource.PLAYERS);
        }
        syncMarkEffect(target, now);
    }

    private static void addHitStack(ServerPlayer owner, LivingEntity target, NiflheimEffectState state,
            boolean freezeAtThreshold) {
        StackKey key = new StackKey(owner.getUUID(), target.getUUID(), false);
        long now = now(owner);
        if (FreezeManager.isFrozen(target)) {
            clearStack(target, key, now);
            AWAITING_THAW.add(key);
            return;
        }
        if (FREEZE_IMMUNITIES.getOrDefault(key, 0L) > now) {
            clearStack(target, key, now);
            return;
        }
        StackState current = STACKS.getOrDefault(key,
                new StackState(0, now + HIT_STACK_DURATION_TICKS, now));
        if (current.expiresAt <= now) current = new StackState(0, now + HIT_STACK_DURATION_TICKS, now);
        FrostStackManager.Result result = FrostStackManager.apply(
                target, owner, ModEffects.NIFLHEIM_MARK, current.stacks, freezeAtThreshold);
        if (result.blocked()) {
            clearStack(target, key, now);
            AWAITING_THAW.add(key);
            return;
        }
        int stacks = result.stacks();
        if (target.level() instanceof ServerLevel level) {
            AlchemyVfx.mark(level, target, AlchemyVfx.NIFLHEIM, AlchemyVfx.GLYPH_NIFLHEIM,
                    Math.max(1, stacks), FrostStackManager.MAX_STACKS);
        }
        if (result.frozen()) {
            beginFreezeCycle(target, key);
            AlchemyVfx.majorProc((ServerLevel)target.level(), target, AlchemyVfx.NIFLHEIM,
                    AlchemyVfx.GLYPH_NIFLHEIM, 2.0F);
        } else {
            STACKS.put(key, new StackState(stacks, now + HIT_STACK_DURATION_TICKS, now));
            FrostStackManager.spawnStackFeedback(target, stacks, net.minecraft.sounds.SoundSource.PLAYERS);
        }
        syncMarkEffect(target, now);
    }

    private static void clearStack(LivingEntity target, StackKey key, long now) {
        if (STACKS.remove(key) != null) syncMarkEffect(target, now);
    }

    private static void beginFreezeCycle(LivingEntity target, StackKey triggeringKey) {
        UUID targetId = target.getUUID();
        Set<StackKey> affectedKeys = new HashSet<>();
        STACKS.keySet().removeIf(key -> {
            boolean matches = key.target.equals(targetId);
            if (matches) affectedKeys.add(key);
            return matches;
        });
        affectedKeys.add(triggeringKey);
        AWAITING_THAW.addAll(affectedKeys);
        target.removeEffect(ModEffects.NIFLHEIM_MARK);
    }

    private static void syncMarkEffect(LivingEntity target, long now) {
        int stacks = 0;
        long expiresAt = 0L;
        for (Map.Entry<StackKey, StackState> entry : STACKS.entrySet()) {
            if (!entry.getKey().target.equals(target.getUUID()) || entry.getValue().expiresAt <= now) continue;
            stacks = Math.max(stacks, entry.getValue().stacks);
            expiresAt = Math.max(expiresAt, entry.getValue().expiresAt);
        }
        target.removeEffect(ModEffects.NIFLHEIM_MARK);
        if (stacks > 0) {
            target.addEffect(new MobEffectInstance(ModEffects.NIFLHEIM_MARK,
                    (int)Math.max(1L, expiresAt - now), stacks - 1, false, false, true));
        }
    }

    private static void syncMarkEffect(net.minecraft.server.MinecraftServer server, UUID targetId, long now) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(targetId) instanceof LivingEntity target) {
                syncMarkEffect(target, now);
                return;
            }
        }
    }

    private static LivingEntity findLivingEntity(net.minecraft.server.MinecraftServer server, UUID targetId) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(targetId) instanceof LivingEntity target) return target;
        }
        return null;
    }

    private static NiflheimEffectState activeState(ServerPlayer player) {
        NiflheimEffectState state = player.getData(ModAttachments.NIFLHEIM_EFFECT);
        if (state.expiresAt() <= now(player) || !player.hasEffect(ModEffects.NIFLHEIM_ESSENCE)) return null;
        return state;
    }

    private static void reconcileAttributeModifiers(ServerPlayer player, NiflheimEffectState state) {
        AlchemyPotionConfiguration configuration = state.configuration();
        double iceBonus = configuration.iceDamageMultiplier(state.tier()) - 1.0D;
        double fireResistance = configuration.fireDamageReduction(state.tier());
        AttributeInstance ice = player.getAttribute(ModAttributes.ICE_DAMAGE_BONUS);
        AttributeInstance fire = player.getAttribute(ModAttributes.FIRE_DAMAGE_RESISTANCE);
        if (ice != null && !modifierMatches(ice, ICE_DAMAGE_MODIFIER, iceBonus)) {
            ice.addOrUpdateTransientModifier(new AttributeModifier(ICE_DAMAGE_MODIFIER, iceBonus,
                    AttributeModifier.Operation.ADD_VALUE));
        }
        if (fire != null && !modifierMatches(fire, FIRE_RESISTANCE_MODIFIER, fireResistance)) {
            fire.addOrUpdateTransientModifier(new AttributeModifier(FIRE_RESISTANCE_MODIFIER, fireResistance,
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applyAttributeModifiers(ServerPlayer player, NiflheimEffectState state) {
        removeAttributeModifiers(player);
        reconcileAttributeModifiers(player, state);
    }

    private static void reconcileIndicator(ServerPlayer player, NiflheimEffectState state) {
        int remaining = (int)Math.min(Integer.MAX_VALUE, Math.max(1L, state.expiresAt() - now(player)));
        MobEffectInstance current = player.getEffect(ModEffects.NIFLHEIM_ESSENCE);
        if (current == null || current.getAmplifier() != state.tier() - 1
                || Math.abs(current.getDuration() - remaining) > 20) {
            player.addEffect(new MobEffectInstance(ModEffects.NIFLHEIM_ESSENCE,
                    remaining, state.tier() - 1, false, false, true));
        }
    }

    private static void removeAttributeModifiers(ServerPlayer player) {
        AttributeInstance ice = player.getAttribute(ModAttributes.ICE_DAMAGE_BONUS);
        AttributeInstance fire = player.getAttribute(ModAttributes.FIRE_DAMAGE_RESISTANCE);
        if (ice != null) ice.removeModifier(ICE_DAMAGE_MODIFIER);
        if (fire != null) fire.removeModifier(FIRE_RESISTANCE_MODIFIER);
    }

    private static boolean modifierMatches(AttributeInstance instance, Identifier id, double expected) {
        AttributeModifier modifier = instance.getModifier(id);
        return modifier != null && Math.abs(modifier.amount() - expected) < 0.000001D;
    }

    private static long now(ServerPlayer player) {
        return player.level().getServer().overworld().getGameTime();
    }

    private record StackKey(UUID owner, UUID target, boolean mist) {
    }

    private record StackState(int stacks, long expiresAt, long nextStackAt) {
    }

    private NiflheimPotionEffects() {
    }
}
