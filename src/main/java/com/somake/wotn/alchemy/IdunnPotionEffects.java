package com.somake.wotn.alchemy;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.registry.ModAttachments;
import com.somake.wotn.registry.ModAttributes;
import com.somake.wotn.registry.ModEffects;

import java.util.Comparator;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class IdunnPotionEffects {
    private static final Identifier MAX_HEALTH = id("idunn_max_health");
    private static final Identifier HEALING = id("idunn_healing_received");

    public static void apply(LivingEntity entity, int tier, AlchemyPotionConfiguration configuration) {
        if (!(entity instanceof ServerPlayer player) || !configuration.isValidFor("idunn", tier)) return;
        float previousMaximum = player.getMaxHealth();
        long now = now(player);
        AlchemyPotionConfiguration.Special special = configuration.special("idunn");
        player.setData(ModAttachments.IDUNN_EFFECT, new IdunnEffectState(
                now + configuration.durationTicks("idunn", tier), tier, configuration,
                special == AlchemyPotionConfiguration.Special.RENEWAL_SEED
                        || special == AlchemyPotionConfiguration.Special.IDUNNS_PROMISE,
                now + 80L, 0));
        player.removeEffect(ModEffects.IDUNN_ELIXIR);
        player.addEffect(new MobEffectInstance(ModEffects.IDUNN_ELIXIR,
                configuration.durationTicks("idunn", tier), tier - 1, false, false, true));
        reconcile(player);
        float gained = player.getMaxHealth() - previousMaximum;
        if (gained > 0.0F) player.setHealth(player.getHealth() + gained);
        if (player.level() instanceof ServerLevel level) {
            AlchemyVfx.activation(level, player, AlchemyVfx.IDUNN, tier);
        }
    }

    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        double bonus = Math.max(0.0D, player.getAttributeValue(ModAttributes.HEALING_RECEIVED_BONUS));
        event.setAmount(event.getAmount() * (float)(1.0D + bonus));
    }

    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        IdunnEffectState state = active(player);
        if (state == null || !state.emergencyAvailable()
                || state.configuration().special("idunn") != AlchemyPotionConfiguration.Special.IDUNNS_PROMISE
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || Math.max(0.0F, event.getNewDamage() - player.getAbsorptionAmount()) < player.getHealth()) return;
        event.setNewDamage(0.0F);
        player.setData(ModAttachments.IDUNN_EFFECT, state.consumeEmergency());
        player.setHealth(player.getMaxHealth() * 0.30F);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 0, false, false, true));
        if (player.level() instanceof ServerLevel level) {
            AlchemyVfx.majorProc(level, player, AlchemyVfx.IDUNN, AlchemyVfx.GLYPH_IDUNN, 3.0F);
        }
    }

    public static void onDamageApplied(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getNewDamage() <= 0.0F) return;
        IdunnEffectState state = active(player);
        if (state == null || !state.emergencyAvailable()
                || state.configuration().special("idunn") != AlchemyPotionConfiguration.Special.RENEWAL_SEED
                || player.getHealth() <= 0.0F || player.getHealth() > player.getMaxHealth() * 0.30F) return;
        player.setData(ModAttachments.IDUNN_EFFECT, state.consumeEmergency());
        player.heal(player.getMaxHealth() * 0.20F);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, false, false, true));
        if (player.level() instanceof ServerLevel level) {
            AlchemyVfx.majorProc(level, player, AlchemyVfx.IDUNN, AlchemyVfx.GLYPH_BLOOM, 2.2F);
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        long now = event.getServer().overworld().getGameTime();
        if (now % 10L != 0L) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            IdunnEffectState state = active(player);
            if (state == null) {
                clear(player);
                continue;
            }
            reconcile(player);
            reconcileIndicator(player, state);
            if (Math.floorMod(now / 10L + player.getId(), 2L) == 0L
                    && player.level() instanceof ServerLevel level) {
                AlchemyVfx.aura(level, player, AlchemyVfx.IDUNN, state.tier(),
                        state.configuration().potency("idunn"), 0, state.expiresAt(),
                        state.configuration().durationTicks("idunn", state.tier()));
            }
            AlchemyPotionConfiguration.Special special = state.configuration().special("idunn");
            if ((special == AlchemyPotionConfiguration.Special.ORCHARDS_GRACE
                    || special == AlchemyPotionConfiguration.Special.GOLDEN_BLOOM)
                    && state.nextPulseAt() <= now) {
                pulse(player, state, special == AlchemyPotionConfiguration.Special.GOLDEN_BLOOM);
            }
        }
    }

    public static void clear(ServerPlayer player) {
        player.setData(ModAttachments.IDUNN_EFFECT, IdunnEffectState.INACTIVE);
        player.removeEffect(ModEffects.IDUNN_ELIXIR);
        remove(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH);
        remove(player.getAttribute(ModAttributes.HEALING_RECEIVED_BONUS), HEALING);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            IdunnEffectState state = active(player);
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

    private static void pulse(ServerPlayer player, IdunnEffectState state, boolean evolved) {
        if (!(player.level() instanceof ServerLevel level)) return;
        boolean goldenPulse = evolved && (state.pulseCount() + 1) % 3 == 0;
        AlchemyVfx.majorProc(level, player, AlchemyVfx.IDUNN,
                goldenPulse ? AlchemyVfx.GLYPH_BLOOM : AlchemyVfx.GLYPH_IDUNN, 5.0F);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(5.0D), target -> target.isAlive() && isAlly(player, target))) {
            float healthBefore = target.getHealth();
            target.heal(1.0F);
            if (target.getHealth() > healthBefore) {
                AlchemyVfx.link(level, player, target, AlchemyVfx.IDUNN, 3);
            }
            if (goldenPulse) {
                removeOneHarmfulEffect(target);
                target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0, false, false, true));
                AlchemyVfx.mark(level, target, AlchemyVfx.IDUNN, AlchemyVfx.GLYPH_BLOOM, 1, 1);
            }
        }
        player.setData(ModAttachments.IDUNN_EFFECT, state.afterPulse(now(player) + 80L));
    }

    private static boolean isAlly(ServerPlayer owner, LivingEntity target) {
        if (target == owner || owner.isAlliedTo(target)) return true;
        return target instanceof TamableAnimal tamable && tamable.isOwnedBy(owner);
    }

    private static void removeOneHarmfulEffect(LivingEntity target) {
        target.getActiveEffects().stream()
                .filter(instance -> instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                .min(Comparator.comparingInt(MobEffectInstance::getDuration))
                .ifPresent(instance -> target.removeEffect(instance.getEffect()));
    }

    private static void reconcile(ServerPlayer player) {
        IdunnEffectState state = active(player);
        if (state == null) return;
        add(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH,
                state.configuration().idunnMaxHealthBonus(state.tier()), AttributeModifier.Operation.ADD_VALUE);
        add(player.getAttribute(ModAttributes.HEALING_RECEIVED_BONUS), HEALING,
                state.configuration().idunnHealingBonus(state.tier()), AttributeModifier.Operation.ADD_VALUE);
    }

    private static void reconcileIndicator(ServerPlayer player, IdunnEffectState state) {
        int remaining = (int)Math.min(Integer.MAX_VALUE, Math.max(1L, state.expiresAt() - now(player)));
        MobEffectInstance current = player.getEffect(ModEffects.IDUNN_ELIXIR);
        if (current == null || current.getAmplifier() != state.tier() - 1
                || Math.abs(current.getDuration() - remaining) > 20) {
            player.addEffect(new MobEffectInstance(ModEffects.IDUNN_ELIXIR,
                    remaining, state.tier() - 1, false, false, true));
        }
    }

    private static IdunnEffectState active(ServerPlayer player) {
        IdunnEffectState state = player.getData(ModAttachments.IDUNN_EFFECT);
        return state.expiresAt() > now(player) && player.hasEffect(ModEffects.IDUNN_ELIXIR) ? state : null;
    }

    private static void add(AttributeInstance instance, Identifier id, double amount,
            AttributeModifier.Operation operation) {
        if (instance != null) instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
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

    private IdunnPotionEffects() {
    }
}
