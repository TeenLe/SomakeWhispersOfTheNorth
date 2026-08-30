package com.somake.wotn.alchemy;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.damage.AlchemyDamage;
import com.somake.wotn.registry.ModAttachments;
import com.somake.wotn.registry.ModEffects;

import java.util.Comparator;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class FenrirPotionEffects {
    private static final Identifier ATTACK_DAMAGE = id("fenrir_attack_damage");
    private static final Identifier ATTACK_SPEED = id("fenrir_attack_speed");
    private static final Identifier FURY_ATTACK_SPEED = id("fenrir_fury_attack_speed");
    private static final Identifier FURY_MOVEMENT_SPEED = id("fenrir_fury_movement_speed");
    private static final Identifier FURY_KNOCKBACK = id("fenrir_fury_knockback_resistance");
    private static final Identifier CHASE_MOVEMENT_SPEED = id("fenrir_chase_movement_speed");

    public static void apply(LivingEntity entity, int tier, AlchemyPotionConfiguration configuration) {
        if (!(entity instanceof ServerPlayer player) || !configuration.isValidFor("fenrir", tier)) return;
        long now = now(player);
        player.setData(ModAttachments.FENRIR_EFFECT, new FenrirEffectState(
                now + configuration.durationTicks("fenrir", tier), tier, configuration,
                java.util.Optional.empty(), 0L, 0, 0L, 0L));
        player.removeEffect(ModEffects.FENRIR_BLOOD);
        player.addEffect(new MobEffectInstance(ModEffects.FENRIR_BLOOD,
                configuration.durationTicks("fenrir", tier), tier - 1, false, false, true));
        reconcile(player);
        if (player.level() instanceof ServerLevel level) {
            AlchemyVfx.activation(level, player, AlchemyVfx.FENRIR, tier);
        }
    }

    public static void onDamageApplied(LivingDamageEvent.Post event) {
        float damageDealt = event.getNewDamage();
        if (damageDealt <= 0.0F) return;
        LivingEntity target = event.getEntity();
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && AlchemyDamage.isMelee(event.getSource(), attacker)) {
            handleMeleeAttack(attacker, target, damageDealt);
        }
        if (target instanceof ServerPlayer defender && AlchemyDamage.isMelee(event.getSource(),
                event.getSource().getEntity() instanceof LivingEntity living ? living : defender)) {
            addFury(defender);
        }
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || !AlchemyDamage.isMelee(event.getSource(), attacker)) return;
        FenrirEffectState state = active(attacker);
        if (state == null || state.preyId().isEmpty()
                || !state.preyId().get().equals(event.getEntity().getUUID())) return;
        if (event.getEntity().getHealth() < event.getEntity().getMaxHealth() * 0.40F) {
            float multiplier = state.configuration().special("fenrir")
                    == AlchemyPotionConfiguration.Special.WILD_HUNT ? 1.25F : 1.20F;
            event.setAmount(event.getAmount() * multiplier);
        }
    }

    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        FenrirEffectState state = active(player);
        if (state == null || state.preyId().isEmpty() || !state.preyId().get().equals(dead.getUUID())) return;
            if (state.configuration().special("fenrir") == AlchemyPotionConfiguration.Special.WILD_HUNT) {
            player.heal(player.getMaxHealth() * 0.25F);
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 80, 0, false, false, true));
            LivingEntity next = nearestEnemy(player, dead.getBoundingBox().inflate(12.0D), dead);
            if (player.level() instanceof ServerLevel level) {
                AlchemyVfx.majorProc(level, dead, AlchemyVfx.FENRIR, AlchemyVfx.GLYPH_FENRIR, 2.0F);
                AlchemyVfx.link(level, dead, player, AlchemyVfx.FENRIR, 5);
            }
            if (next != null) {
                player.setData(ModAttachments.FENRIR_EFFECT, state.withPrey(next.getUUID(), now(player) + 400L));
                if (player.level() instanceof ServerLevel level) {
                    AlchemyVfx.link(level, player, next, AlchemyVfx.FENRIR, 4);
                    AlchemyVfx.mark(level, next, AlchemyVfx.FENRIR, AlchemyVfx.GLYPH_FENRIR, 1, 1);
                }
                return;
            }
        }
        player.setData(ModAttachments.FENRIR_EFFECT, state.withoutPrey());
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        long now = event.getServer().overworld().getGameTime();
        if (now % 5L != 0L) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            FenrirEffectState state = active(player);
            if (state == null) {
                player.setData(ModAttachments.FENRIR_EFFECT, FenrirEffectState.INACTIVE);
                player.removeEffect(ModEffects.FENRIR_BLOOD);
                removeModifiers(player);
                continue;
            }
            if (state.fury() > 0 && state.furyExpiresAt() <= now) {
                state = state.withFury(0, 0L);
                player.setData(ModAttachments.FENRIR_EFFECT, state);
            }
            if (state.preyId().isPresent() && state.preyExpiresAt() <= now) {
                state = state.withoutPrey();
                player.setData(ModAttachments.FENRIR_EFFECT, state);
            }
            applyChaseSpeed(player, state);
            reconcileIndicator(player, state);
            reconcile(player);
            if (Math.floorMod(now / 5L + player.getId(), 4L) == 0L
                    && player.level() instanceof ServerLevel level) {
                AlchemyVfx.aura(level, player, AlchemyVfx.FENRIR, state.tier(),
                        state.configuration().potency("fenrir"), state.fury(), state.expiresAt(),
                        state.configuration().durationTicks("fenrir", state.tier()));
            }
        }
    }

    public static void clear(ServerPlayer player) {
        player.setData(ModAttachments.FENRIR_EFFECT, FenrirEffectState.INACTIVE);
        player.removeEffect(ModEffects.FENRIR_BLOOD);
        removeModifiers(player);
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FenrirEffectState state = active(player);
            if (state == null) clear(player);
            else {
                reconcileIndicator(player, state);
                reconcile(player);
            }
        }
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.isWasDeath()) {
            clear(player);
        } else {
            reconcile(player);
        }
    }

    private static void handleMeleeAttack(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        FenrirEffectState state = active(attacker);
        if (state == null || target == attacker || attacker.isAlliedTo(target)) return;
        AlchemyPotionConfiguration.Special special = state.configuration().special("fenrir");
        if ((special == AlchemyPotionConfiguration.Special.PREDATORY_INSTINCT
                || special == AlchemyPotionConfiguration.Special.WILD_HUNT) && state.preyId().isEmpty()) {
            state = state.withPrey(target.getUUID(), now(attacker) + 400L);
            attacker.setData(ModAttachments.FENRIR_EFFECT, state);
            if (attacker.level() instanceof ServerLevel level) {
                AlchemyVfx.link(level, attacker, target, AlchemyVfx.FENRIR, 4);
                AlchemyVfx.mark(level, target, AlchemyVfx.FENRIR, AlchemyVfx.GLYPH_FENRIR, 1, 1);
            }
        } else if (state.preyId().isPresent() && state.preyId().get().equals(target.getUUID())) {
            state = state.withPrey(target.getUUID(), now(attacker) + 400L);
            attacker.setData(ModAttachments.FENRIR_EFFECT, state);
        }
        if (special == AlchemyPotionConfiguration.Special.FERAL_BLOOD
                || special == AlchemyPotionConfiguration.Special.CHAINBREAKER) {
            if (special == AlchemyPotionConfiguration.Special.CHAINBREAKER && state.fury() >= 5
                    && state.chainbreakerReadyAt() <= now(attacker)) {
                triggerChainbreaker(attacker, target, state, damageDealt);
            } else {
                addFury(attacker);
            }
        }
    }

    private static void addFury(ServerPlayer player) {
        FenrirEffectState state = active(player);
        if (state == null) return;
        AlchemyPotionConfiguration.Special special = state.configuration().special("fenrir");
        if (special != AlchemyPotionConfiguration.Special.FERAL_BLOOD
                && special != AlchemyPotionConfiguration.Special.CHAINBREAKER) return;
        int newFury = Math.min(5, state.fury() + 1);
        player.setData(ModAttachments.FENRIR_EFFECT,
                state.withFury(newFury, now(player) + 100L));
        reconcile(player);
        if (newFury > state.fury() && player.level() instanceof ServerLevel level) {
            AlchemyVfx.mark(level, player, AlchemyVfx.FENRIR,
                    newFury >= 5 ? AlchemyVfx.GLYPH_CHAINBREAKER : AlchemyVfx.GLYPH_FENRIR,
                    newFury, 5);
        }
    }

    private static void triggerChainbreaker(ServerPlayer player, LivingEntity primary, FenrirEffectState state,
            float mirroredDamage) {
        player.setData(ModAttachments.FENRIR_EFFECT, state.consumeFury(now(player) + 160L));
        reconcile(player);
        if (!(player.level() instanceof ServerLevel level)) return;
        AlchemyVfx.majorProc(level, primary, AlchemyVfx.FENRIR, AlchemyVfx.GLYPH_CHAINBREAKER, 3.0F);
        var nearbyTargets = level.getEntitiesOfClass(LivingEntity.class, primary.getBoundingBox().inflate(3.0D),
                target -> target != player && target != primary && target.isAlive()
                        && !player.isAlliedTo(target) && player.canAttack(target)).stream().limit(6).toList();
        nearbyTargets.forEach(target -> {
            target.hurt(AlchemyDamage.source(target, AlchemyDamage.CHAINBREAKER, player), mirroredDamage);
            pushAway(primary, target);
        });
        primary.hurt(AlchemyDamage.source(primary, AlchemyDamage.CHAINBREAKER, player), mirroredDamage);
        pushAway(player, primary);
    }

    private static void pushAway(Entity origin, LivingEntity target) {
        Vec3 direction = target.position().subtract(origin.position());
        if (direction.horizontalDistanceSqr() < 0.0001D) direction = new Vec3(0.0D, 0.0D, 1.0D);
        direction = direction.normalize();
        double resistance = Math.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
        target.push(direction.x * 0.6D * (1.0D - resistance), 0.10D * (1.0D - resistance),
                direction.z * 0.6D * (1.0D - resistance));
    }

    private static LivingEntity nearestEnemy(ServerPlayer player, AABB area, LivingEntity excluded) {
        if (!(player.level() instanceof ServerLevel level)) return null;
        return level.getEntitiesOfClass(LivingEntity.class, area,
                target -> target != player && target != excluded && target.isAlive()
                        && !player.isAlliedTo(target) && player.canAttack(target)).stream()
                .min(Comparator.comparingDouble((LivingEntity target) -> player.distanceToSqr(target))
                        .thenComparingInt(Entity::getId))
                .orElse(null);
    }

    private static void applyChaseSpeed(ServerPlayer player, FenrirEffectState state) {
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (state.preyId().isEmpty() || !(player.level() instanceof ServerLevel level)) {
            remove(movement, CHASE_MOVEMENT_SPEED);
            return;
        }
        Entity entity = level.getEntity(state.preyId().get());
        if (!(entity instanceof LivingEntity prey) || !prey.isAlive()) {
            remove(movement, CHASE_MOVEMENT_SPEED);
            return;
        }
        if (player.distanceToSqr(prey) <= 32.0D * 32.0D) {
            addModifier(movement, CHASE_MOVEMENT_SPEED, 0.20D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        } else {
            remove(movement, CHASE_MOVEMENT_SPEED);
        }
    }

    private static void reconcile(ServerPlayer player) {
        FenrirEffectState state = active(player);
        if (state == null) {
            removeModifiers(player);
            return;
        }
        addModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_DAMAGE,
                state.configuration().fenrirAttackDamageBonus(state.tier()),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED,
                state.configuration().fenrirAttackSpeedBonus(state.tier()),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        double fury = state.fury();
        addModifier(player.getAttribute(Attributes.ATTACK_SPEED), FURY_ATTACK_SPEED, fury * 0.04D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), FURY_MOVEMENT_SPEED, fury * 0.03D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addModifier(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), FURY_KNOCKBACK, fury * 0.02D,
                AttributeModifier.Operation.ADD_VALUE);
    }

    private static void reconcileIndicator(ServerPlayer player, FenrirEffectState state) {
        int remaining = (int)Math.min(Integer.MAX_VALUE, Math.max(1L, state.expiresAt() - now(player)));
        MobEffectInstance current = player.getEffect(ModEffects.FENRIR_BLOOD);
        if (current == null || current.getAmplifier() != state.tier() - 1
                || Math.abs(current.getDuration() - remaining) > 20) {
            player.addEffect(new MobEffectInstance(ModEffects.FENRIR_BLOOD,
                    remaining, state.tier() - 1, false, false, true));
        }
    }

    private static FenrirEffectState active(ServerPlayer player) {
        FenrirEffectState state = player.getData(ModAttachments.FENRIR_EFFECT);
        return state.expiresAt() > now(player) && player.hasEffect(ModEffects.FENRIR_BLOOD) ? state : null;
    }

    private static void removeModifiers(ServerPlayer player) {
        remove(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_DAMAGE);
        remove(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED);
        remove(player.getAttribute(Attributes.ATTACK_SPEED), FURY_ATTACK_SPEED);
        remove(player.getAttribute(Attributes.MOVEMENT_SPEED), FURY_MOVEMENT_SPEED);
        remove(player.getAttribute(Attributes.MOVEMENT_SPEED), CHASE_MOVEMENT_SPEED);
        remove(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), FURY_KNOCKBACK);
    }

    private static void addModifier(AttributeInstance instance, Identifier id, double amount,
            AttributeModifier.Operation operation) {
        if (instance == null) return;
        if (Math.abs(amount) < 0.000001D) {
            instance.removeModifier(id);
        } else {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static void remove(AttributeInstance instance, Identifier id) {
        if (instance != null) instance.removeModifier(id);
    }

    private static long now(ServerPlayer player) {
        return player.level().getServer().overworld().getGameTime();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, path);
    }

    private FenrirPotionEffects() {
    }
}
