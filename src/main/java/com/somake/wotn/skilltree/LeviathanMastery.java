package com.somake.wotn.skilltree;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LeviathanMastery {
    private static final long TARGET_REWARD_INTERVAL = 20L * 8L;
    private static final Map<UUID, Map<UUID, Long>> LAST_TARGET_REWARDS = new HashMap<>();

    public static void awardForHostileHit(ServerPlayer player, ItemStack weapon, LivingEntity target, int amount) {
        if (!(target instanceof Monster) || target.isAlliedTo(player) || amount <= 0) return;
        UUID weaponId = WeaponSkillData.ensureIdentity(weapon);
        long now = player.level().getGameTime();
        Map<UUID, Long> playerRewards = LAST_TARGET_REWARDS.computeIfAbsent(
                weaponId, ignored -> new HashMap<>());
        long lastReward = playerRewards.getOrDefault(target.getUUID(), Long.MIN_VALUE / 2);
        if (now - lastReward < TARGET_REWARD_INTERVAL) return;
        playerRewards.put(target.getUUID(), now);
        if (playerRewards.size() > 64) {
            playerRewards.entrySet().removeIf(entry -> now - entry.getValue() > TARGET_REWARD_INTERVAL * 2L);
        }
        award(player, weapon, amount);
    }

    public static void award(ServerPlayer player, ItemStack weapon, int amount) {
        if (amount <= 0 || !WeaponSkillData.isEligible(weapon)) return;
        WeaponSkillProgress current = WeaponSkillData.progress(weapon);
        WeaponSkillProgress.MasteryGain normalized = current.addMasteryXp(0);
        WeaponSkillProgress.MasteryGain gain = normalized.progress().addMasteryXp(amount);
        if (gain.progress().equals(current)) return;
        WeaponSkillData.setProgress(weapon, gain.progress());
        if (gain.levelsGained() > 0) signalLevelUp(player, gain.progress().masteryLevel(), gain.pointsGained());
    }

    private static void signalLevelUp(ServerPlayer player, int level, int points) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                Component.translatable("message.wotn.mastery_level_up", level, points)
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true));
        ((net.minecraft.server.level.ServerLevel) player.level()).playSound(null,
                player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.8F, 1.15F);
        ((net.minecraft.server.level.ServerLevel) player.level()).sendParticles(player, ParticleTypes.SNOWFLAKE,
                false, false,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                28, 0.5D, 0.8D, 0.5D, 0.08D);
    }

    private LeviathanMastery() {
    }
}
