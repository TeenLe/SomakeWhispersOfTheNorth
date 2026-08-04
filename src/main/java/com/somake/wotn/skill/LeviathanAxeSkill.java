package com.somake.wotn.skill;

import com.somake.wotn.entity.LeviathanAxeEntity;
import com.somake.wotn.effect.LeviathanAxeEffects;
import com.somake.wotn.network.LeviathanAxeCooldownPayload;
import com.somake.wotn.registry.ModItems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class LeviathanAxeSkill {
    public static final int COOLDOWN_TICKS = 20 * 8;
    private static final Map<UUID, Long> READY_AT = new HashMap<>();

    public static void activate(ServerPlayer player) {
        InteractionHand hand = findAxeHand(player);
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        long readyAt = READY_AT.getOrDefault(player.getUUID(), 0L);
        if (now >= readyAt) {
            READY_AT.remove(player.getUUID());
        }
        if (now < readyAt) {
            sync(player, (int) Math.min(Integer.MAX_VALUE, readyAt - now));
            return;
        }
        if (hand == null || !player.isAlive() || player.isSpectator()) {
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.9F,
                0.75F + player.getRandom().nextFloat() * 0.15F);
        LeviathanAxeEntity axe = new LeviathanAxeEntity(level, player, stack);
        axe.setReturnHand(hand);
        axe.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.65F, 0.35F);
        level.addFreshEntity(axe);
        LeviathanAxeEffects.spawnThrow(level, player, axe.getDeltaMovement());
        player.setItemInHand(hand, ItemStack.EMPTY);
        player.swing(hand, true);

        READY_AT.put(player.getUUID(), now + COOLDOWN_TICKS);
        sync(player, COOLDOWN_TICKS);
    }

    private static InteractionHand findAxeHand(ServerPlayer player) {
        if (player.getMainHandItem().is(ModItems.LEVIATHAN_AXE.get())) {
            return InteractionHand.MAIN_HAND;
        }
        return player.getOffhandItem().is(ModItems.LEVIATHAN_AXE.get()) ? InteractionHand.OFF_HAND : null;
    }

    private static void sync(ServerPlayer player, int remainingTicks) {
        PacketDistributor.sendToPlayer(player, new LeviathanAxeCooldownPayload(remainingTicks, COOLDOWN_TICKS));
    }

    private LeviathanAxeSkill() {
    }
}
