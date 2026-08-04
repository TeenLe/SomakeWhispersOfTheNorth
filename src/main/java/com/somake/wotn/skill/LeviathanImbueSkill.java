package com.somake.wotn.skill;

import com.somake.wotn.network.LeviathanImbueStatePayload;
import com.somake.wotn.registry.ModDataComponents;
import com.somake.wotn.registry.ModItems;
import com.somake.wotn.registry.ModSounds;
import com.somake.wotn.effect.FreezeManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import com.somake.wotn.effect.LeviathanAxeEffects;
import net.neoforged.neoforge.network.PacketDistributor;

public final class LeviathanImbueSkill {
    public static final int DURATION_TICKS = 20 * 8;
    public static final int COOLDOWN_TICKS = 20 * 20;
    public static final float BONUS_DAMAGE = 6.0F;
    private static final Map<UUID, Long> READY_AT = new HashMap<>();

    public static void activate(ServerPlayer player) {
        ItemStack axe = findAxe(player);
        if (axe.isEmpty() || !player.isAlive() || player.isSpectator()) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        long now = level.getServer().overworld().getGameTime();
        long readyAt = READY_AT.getOrDefault(player.getUUID(), 0L);
        if (now < readyAt) {
            sync(player, getRemaining(axe, level), (int) (readyAt - now), true);
            return;
        }

        long activeUntil = now + DURATION_TICKS;
        axe.set(ModDataComponents.ICE_IMBUED_UNTIL.get(), activeUntil);
        READY_AT.put(player.getUUID(), now + COOLDOWN_TICKS);
        level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getEyeY() - 0.45D, player.getZ(),
                28, 0.5D, 0.55D, 0.5D, 0.11D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.6F, 1.25F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.POWDER_SNOW_BREAK,
                SoundSource.PLAYERS, 0.55F, 0.85F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FREEZE.get(),
                SoundSource.PLAYERS, FreezeManager.SOUND_VOLUME, 1.0F);
        sync(player, DURATION_TICKS, COOLDOWN_TICKS, false);
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        var source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player) {
            return;
        }
        ItemStack weapon = source.getWeaponItem();
        if (weapon != null && weapon.is(ModItems.LEVIATHAN_AXE.get()) && isActive(weapon, player.level())) {
            event.setAmount(event.getAmount() + BONUS_DAMAGE);
        }
    }

    public static void onDamageApplied(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0F) {
            return;
        }
        var source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player) {
            return;
        }
        ItemStack weapon = source.getWeaponItem();
        if (weapon != null && weapon.is(ModItems.LEVIATHAN_AXE.get()) && isActive(weapon, player.level())
                && player.level() instanceof ServerLevel serverLevel) {
            LeviathanAxeEffects.spawnImbuedHit(serverLevel, event.getEntity(), player.getLookAngle(), false);
        }
    }

    public static boolean isActive(ItemStack stack, net.minecraft.world.level.Level level) {
        Long until = stack.get(ModDataComponents.ICE_IMBUED_UNTIL.get());
        if (until == null || level.getServer() == null) {
            return false;
        }
        return until > level.getServer().overworld().getGameTime();
    }

    private static int getRemaining(ItemStack stack, ServerLevel level) {
        Long until = stack.get(ModDataComponents.ICE_IMBUED_UNTIL.get());
        return until == null ? 0 : (int) Math.max(0L, until - level.getServer().overworld().getGameTime());
    }

    private static ItemStack findAxe(ServerPlayer player) {
        if (player.getMainHandItem().is(ModItems.LEVIATHAN_AXE.get())) {
            return player.getMainHandItem();
        }
        return player.getOffhandItem().is(ModItems.LEVIATHAN_AXE.get()) ? player.getOffhandItem() : ItemStack.EMPTY;
    }

    private static void sync(ServerPlayer player, int active, int cooldown, boolean denied) {
        PacketDistributor.sendToPlayer(player, new LeviathanImbueStatePayload(active, cooldown, denied));
    }

    private LeviathanImbueSkill() {
    }
}
