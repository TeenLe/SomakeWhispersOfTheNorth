package com.somake.wotn.alchemy;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.damage.AlchemyDamage;
import com.somake.wotn.registry.ModAttachments;
import com.somake.wotn.registry.ModAttributes;
import com.somake.wotn.registry.ModEffects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class JormungandrPotionEffects {
    private static final Identifier VENOM_DAMAGE = id("jormungandr_venom_damage");
    private static final Identifier POISON_RESISTANCE = id("jormungandr_poison_resistance");
    private static final Map<VenomKey, VenomState> VENOMS = new HashMap<>();
    private static final Map<VenomKey, Long> RUPTURE_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Map<UUID, Identifier>> CORROSION_MODIFIERS = new HashMap<>();

    public static void apply(LivingEntity entity, int tier, AlchemyPotionConfiguration configuration) {
        if (!(entity instanceof ServerPlayer player) || !configuration.isValidFor("jormungandr", tier)) return;
        player.setData(ModAttachments.JORMUNGANDR_EFFECT, new JormungandrEffectState(
                now(player) + configuration.durationTicks("jormungandr", tier), tier, configuration));
        player.removeEffect(ModEffects.JORMUNGANDR_VENOM);
        player.addEffect(new MobEffectInstance(ModEffects.JORMUNGANDR_VENOM,
                configuration.durationTicks("jormungandr", tier), tier - 1, false, false, true));
        reconcile(player);
        if (player.level() instanceof ServerLevel level) {
            AlchemyVfx.activation(level, player, AlchemyVfx.JORMUNGANDR, tier);
        }
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || (!event.getSource().is(AlchemyDamage.JORMUNGANDR_VENOM)
                && !event.getSource().is(AlchemyDamage.JORMUNGANDR_RUPTURE)
                && !event.getSource().is(net.neoforged.neoforge.common.NeoForgeMod.POISON_DAMAGE))) return;
        double resistance = Math.clamp(player.getAttributeValue(ModAttributes.POISON_RESISTANCE), 0.0D, 0.8D);
        event.setAmount(event.getAmount() * (float)(1.0D - resistance));
    }

    public static void onDamageApplied(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0F || AlchemyDamage.isSecondary(event.getSource())) return;
        if (event.getSource().getEntity() instanceof ServerPlayer player) addVenom(player, event.getEntity(), 1, 0);
    }

    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        List<Map.Entry<VenomKey, VenomState>> states = VENOMS.entrySet().stream()
                .filter(entry -> entry.getKey().target.equals(dead.getUUID())).toList();
        for (Map.Entry<VenomKey, VenomState> entry : states) {
            ServerPlayer owner = dead.level().getServer().getPlayerList().getPlayer(entry.getKey().owner);
            if (owner != null) propagate(owner, dead, entry.getValue());
            removeVenom(dead.level().getServer(), entry.getKey());
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        long now = event.getServer().overworld().getGameTime();
        if (now % 10L != 0L) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (active(player) == null) {
                player.setData(ModAttachments.JORMUNGANDR_EFFECT, JormungandrEffectState.INACTIVE);
                player.removeEffect(ModEffects.JORMUNGANDR_VENOM);
                removeModifiers(player);
            } else {
                JormungandrEffectState activeState = active(player);
                reconcileIndicator(player, activeState);
                reconcile(player);
                if (Math.floorMod(now / 10L + player.getId(), 2L) == 0L
                        && player.level() instanceof ServerLevel level) {
                    AlchemyVfx.aura(level, player, AlchemyVfx.JORMUNGANDR,
                            activeState.tier(), activeState.configuration().potency("jormungandr"), 0,
                            activeState.expiresAt(),
                            activeState.configuration().durationTicks("jormungandr", activeState.tier()));
                }
            }
        }
        for (VenomKey key : List.copyOf(VENOMS.keySet())) {
            VenomState state = VENOMS.get(key);
            if (state == null) continue;
            LivingEntity target = entity(event.getServer(), key.target);
            ServerPlayer owner = event.getServer().getPlayerList().getPlayer(key.owner);
            if (target == null || owner == null || !target.isAlive() || state.expiresAt <= now) {
                removeVenom(event.getServer(), key);
                continue;
            }
            if (state.nextTickAt <= now) {
                float damage = 0.4F * state.stacks * (1.0F + state.damageBonus);
                float healthBefore = target.getHealth();
                target.hurt(AlchemyDamage.source(target, AlchemyDamage.JORMUNGANDR_VENOM, owner), damage);
                if (target.getHealth() < healthBefore && target.level() instanceof ServerLevel level) {
                    AlchemyVfx.mark(level, target, AlchemyVfx.JORMUNGANDR, AlchemyVfx.GLYPH_JORMUNGANDR,
                            state.stacks, Math.max(1, state.stacks));
                }
                if (VENOMS.containsKey(key)) VENOMS.put(key, state.withNextTick(now + 20L));
            }
        }
        RUPTURE_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    public static void clear(ServerPlayer player) {
        player.setData(ModAttachments.JORMUNGANDR_EFFECT, JormungandrEffectState.INACTIVE);
        player.removeEffect(ModEffects.JORMUNGANDR_VENOM);
        removeModifiers(player);
        VENOMS.keySet().stream().filter(key -> key.owner.equals(player.getUUID())).toList()
                .forEach(key -> removeVenom(player.level().getServer(), key));
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            JormungandrEffectState state = active(player);
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

    private static void addVenom(ServerPlayer owner, LivingEntity target, int amount, int generation) {
        JormungandrEffectState effect = active(owner);
        if (effect == null || target == owner || owner.isAlliedTo(target) || !owner.canAttack(target)) return;
        VenomKey key = new VenomKey(owner.getUUID(), target.getUUID());
        long now = now(owner);
        float damageBonus = (float)Math.max(0.0D, owner.getAttributeValue(ModAttributes.VENOM_DAMAGE_BONUS));
        VenomState current = VENOMS.getOrDefault(key, new VenomState(0, now + 120L, now + 20L,
                effect.tier(), damageBonus,
                effect.configuration().special("jormungandr"), generation));
        int maximum = effect.configuration().maximumVenomStacks(effect.tier());
        VenomState updated = new VenomState(Math.min(maximum, current.stacks + amount), now + 120L,
                current.nextTickAt, effect.tier(), damageBonus,
                effect.configuration().special("jormungandr"), Math.max(current.generation, generation));
        VENOMS.put(key, updated);
        updateCorrosion(target, key, updated);
        if (updated.special == AlchemyPotionConfiguration.Special.SERPENT_RUPTURE
                && updated.stacks >= maximum && RUPTURE_COOLDOWNS.getOrDefault(key, 0L) <= now) {
            if (target.level() instanceof ServerLevel level) {
                AlchemyVfx.majorProc(level, target, AlchemyVfx.JORMUNGANDR, AlchemyVfx.GLYPH_RUPTURE, 2.6F);
            }
            target.hurt(AlchemyDamage.source(target, AlchemyDamage.JORMUNGANDR_RUPTURE, owner),
                    6.0F * (1.0F + updated.damageBonus));
            RUPTURE_COOLDOWNS.put(key, now + 100L);
            removeVenom(owner.level().getServer(), key);
        } else if (target.level() instanceof ServerLevel level) {
            AlchemyVfx.mark(level, target, AlchemyVfx.JORMUNGANDR, AlchemyVfx.GLYPH_JORMUNGANDR,
                    updated.stacks, maximum);
        }
    }

    private static void propagate(ServerPlayer owner, LivingEntity dead, VenomState venom) {
        if (venom.special != AlchemyPotionConfiguration.Special.SERPENTINE_MIASMA
                && venom.special != AlchemyPotionConfiguration.Special.WORLD_COIL) return;
        if (venom.special == AlchemyPotionConfiguration.Special.SERPENTINE_MIASMA && venom.generation > 0) return;
        if (venom.special == AlchemyPotionConfiguration.Special.WORLD_COIL && venom.generation > 1) return;
        if (!(dead.level() instanceof ServerLevel level)) return;
        double radius = venom.special == AlchemyPotionConfiguration.Special.WORLD_COIL ? 5.0D : 3.5D;
        int amount = venom.special == AlchemyPotionConfiguration.Special.WORLD_COIL ? venom.stacks : 1;
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, dead.getBoundingBox().inflate(radius),
                target -> target != owner && target != dead && target.isAlive()
                        && !owner.isAlliedTo(target) && owner.canAttack(target)).stream()
                .sorted(java.util.Comparator.comparingDouble(dead::distanceToSqr)).limit(5)
                .toList();
        if (!targets.isEmpty()) {
            AlchemyVfx.majorProc(level, dead, AlchemyVfx.JORMUNGANDR,
                    venom.special == AlchemyPotionConfiguration.Special.WORLD_COIL
                            ? AlchemyVfx.GLYPH_JORMUNGANDR : AlchemyVfx.GLYPH_RUPTURE,
                    (float)radius);
        }
        targets.forEach(target -> {
            AlchemyVfx.link(level, dead, target, AlchemyVfx.JORMUNGANDR,
                    venom.special == AlchemyPotionConfiguration.Special.WORLD_COIL ? 5 : 3);
            addVenom(owner, target, amount, venom.generation + 1);
        });
    }

    private static void updateCorrosion(LivingEntity target, VenomKey key, VenomState state) {
        if (state.special != AlchemyPotionConfiguration.Special.CORROSIVE_FANGS
                && state.special != AlchemyPotionConfiguration.Special.SERPENT_RUPTURE) return;
        Identifier modifierId = Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID,
                "jormungandr_corrosion_" + key.owner.toString().replace("-", ""));
        AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.addOrUpdateTransientModifier(new AttributeModifier(
                modifierId, -0.03D * state.stacks, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        CORROSION_MODIFIERS.computeIfAbsent(key.target, ignored -> new HashMap<>()).put(key.owner, modifierId);
    }

    private static void removeVenom(MinecraftServer server, VenomKey key) {
        LivingEntity target = entity(server, key.target);
        removeCorrosion(target, key);
        VENOMS.remove(key);
    }

    private static void removeCorrosion(LivingEntity target, VenomKey key) {
        Map<UUID, Identifier> modifiers = CORROSION_MODIFIERS.get(key.target);
        Identifier id = modifiers == null ? null : modifiers.remove(key.owner);
        if (target != null && id != null && target.getAttribute(Attributes.ARMOR) != null) {
            target.getAttribute(Attributes.ARMOR).removeModifier(id);
        }
        if (modifiers != null && modifiers.isEmpty()) CORROSION_MODIFIERS.remove(key.target);
    }

    private static LivingEntity entity(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(uuid) instanceof LivingEntity living) return living;
        }
        return null;
    }

    private static void reconcile(ServerPlayer player) {
        JormungandrEffectState state = active(player);
        if (state == null) return;
        add(player.getAttribute(ModAttributes.VENOM_DAMAGE_BONUS), VENOM_DAMAGE,
                state.configuration().venomDamageBonus(state.tier()));
        add(player.getAttribute(ModAttributes.POISON_RESISTANCE), POISON_RESISTANCE,
                state.configuration().poisonResistance(state.tier()));
    }

    private static void reconcileIndicator(ServerPlayer player, JormungandrEffectState state) {
        int remaining = (int)Math.min(Integer.MAX_VALUE, Math.max(1L, state.expiresAt() - now(player)));
        MobEffectInstance current = player.getEffect(ModEffects.JORMUNGANDR_VENOM);
        if (current == null || current.getAmplifier() != state.tier() - 1
                || Math.abs(current.getDuration() - remaining) > 20) {
            player.addEffect(new MobEffectInstance(ModEffects.JORMUNGANDR_VENOM,
                    remaining, state.tier() - 1, false, false, true));
        }
    }

    private static JormungandrEffectState active(ServerPlayer player) {
        JormungandrEffectState state = player.getData(ModAttachments.JORMUNGANDR_EFFECT);
        return state.expiresAt() > now(player) && player.hasEffect(ModEffects.JORMUNGANDR_VENOM) ? state : null;
    }

    private static void add(AttributeInstance instance, Identifier id, double amount) {
        if (instance != null) instance.addOrUpdateTransientModifier(new AttributeModifier(
                id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void removeModifiers(ServerPlayer player) {
        if (player.getAttribute(ModAttributes.VENOM_DAMAGE_BONUS) != null) {
            player.getAttribute(ModAttributes.VENOM_DAMAGE_BONUS).removeModifier(VENOM_DAMAGE);
        }
        if (player.getAttribute(ModAttributes.POISON_RESISTANCE) != null) {
            player.getAttribute(ModAttributes.POISON_RESISTANCE).removeModifier(POISON_RESISTANCE);
        }
    }

    private static long now(ServerPlayer player) {
        return player.level().getServer().overworld().getGameTime();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, path);
    }

    private record VenomKey(UUID owner, UUID target) {
    }

    private record VenomState(int stacks, long expiresAt, long nextTickAt, int tier, float damageBonus,
            AlchemyPotionConfiguration.Special special, int generation) {
        private VenomState withNextTick(long nextTickAt) {
            return new VenomState(stacks, expiresAt, nextTickAt, tier, damageBonus, special, generation);
        }
    }

    private JormungandrPotionEffects() {
    }
}
