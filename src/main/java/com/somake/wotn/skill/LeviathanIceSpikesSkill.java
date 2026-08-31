package com.somake.wotn.skill;

import com.somake.wotn.effect.FreezeManager;
import com.somake.wotn.entity.IceSpikeEntity;
import com.somake.wotn.network.CameraShakeDispatcher;
import com.somake.wotn.network.LeviathanIceSpikesCooldownPayload;
import com.somake.wotn.network.IceSpikesSlamPayload;
import com.somake.wotn.registry.ModItems;
import com.somake.wotn.registry.ModDataComponents;
import com.somake.wotn.skilltree.LeviathanSkillTree;
import com.somake.wotn.skilltree.WeaponSkillData;
import com.somake.wotn.registry.ModSounds;
import com.somake.wotn.particle.ParticleHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Iterator;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class LeviathanIceSpikesSkill {
    public static final int COOLDOWN_TICKS = 20 * 8;
    private static final int SLAM_IMPACT_TICK = 9;
    private static final Map<UUID, Long> READY_AT = new HashMap<>();
    private static final Map<UUID, PendingCast> PENDING_CASTS = new HashMap<>();
    private static final double[][] ROWS = {
            {2.0D, 0.0D},
            {4.0D, -0.75D, 0.75D},
            {6.0D, -1.5D, 0.0D, 1.5D},
            {8.0D, -2.25D, -0.75D, 0.75D, 2.25D}
    };
    private static final float[] ROW_SCALES = {0.72F, 0.88F, 1.04F, 1.20F};

    public static void activate(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getServer().overworld().getGameTime();
        long readyAt = READY_AT.getOrDefault(player.getUUID(), 0L);
        if (!isHoldingAxe(player) || !isEquipped(getAxe(player), 2)
                || !WeaponSkillData.progress(getAxe(player)).isUnlocked(LeviathanSkillTree.ICE_SPIKES)
                || !player.isAlive() || player.isSpectator()) {
            sync(player, Math.max(0, (int) (readyAt - now)), true);
            return;
        }
        if (now < readyAt) {
            sync(player, (int) (readyAt - now), true);
            return;
        }
        READY_AT.remove(player.getUUID());

        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0D, look.z);
        if (forward.lengthSqr() < 1.0E-6D) {
            sync(player, 0, true);
            return;
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        List<SpikePlacement> placements = collectPlacements(level, player, forward, right);
        if (placements.isEmpty()) {
            sync(player, 0, true);
            return;
        }

        HumanoidArm slamArm = getAxeArm(player);
        long startTime = level.getGameTime();
        UUID weaponId = WeaponSkillData.ensureIdentity(getAxe(player));
        PENDING_CASTS.put(player.getUUID(), new PendingCast(level.dimension(), player.getUUID(), weaponId,
                startTime + SLAM_IMPACT_TICK, forward, List.copyOf(placements)));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new IceSpikesSlamPayload(player.getId(), startTime, slamArm == HumanoidArm.RIGHT));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AXE_SCRAPE,
                SoundSource.PLAYERS, 0.42F, 0.72F);
        player.swing(slamArm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, true);
        READY_AT.put(player.getUUID(), now + COOLDOWN_TICKS);
        sync(player, COOLDOWN_TICKS, false);
    }

    private static net.minecraft.world.item.ItemStack getAxe(ServerPlayer player) {
        return player.getMainHandItem().is(ModItems.LEVIATHAN_AXE.get())
                ? player.getMainHandItem() : player.getOffhandItem();
    }

    private static boolean isEquipped(net.minecraft.world.item.ItemStack axe, int skillId) {
        return axe.getOrDefault(ModDataComponents.LEVIATHAN_PRIMARY_SKILL.get(), 3) == skillId
                || axe.getOrDefault(ModDataComponents.LEVIATHAN_SECONDARY_SKILL.get(), 0) == skillId;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        Iterator<PendingCast> iterator = PENDING_CASTS.values().iterator();
        while (iterator.hasNext()) {
            PendingCast cast = iterator.next();
            ServerLevel level = server.getLevel(cast.dimension);
            if (level == null) {
                iterator.remove();
                continue;
            }
            if (level.getGameTime() < cast.impactTime) continue;
            iterator.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(cast.playerId);
            if (player == null || !player.isAlive() || player.isSpectator() || player.level() != level
                    || !isHoldingAxe(player)) continue;
            net.minecraft.world.item.ItemStack weapon = findHeldAxe(player, cast.weaponId);
            if (weapon.isEmpty() || !isEquipped(weapon, 2)
                    || !WeaponSkillData.progress(weapon).isUnlocked(LeviathanSkillTree.ICE_SPIKES)) continue;
            executeImpact(level, player, weapon, cast.forward, cast.placements);
        }
    }

    private static void executeImpact(ServerLevel level, ServerPlayer player,
            net.minecraft.world.item.ItemStack weapon, Vec3 forward,
            List<SpikePlacement> placements) {
        float yaw = (float) Math.toDegrees(Math.atan2(forward.x, forward.z));
        IceSpikeEntity damageController = null;
        for (SpikePlacement placement : placements) {
            IceSpikeEntity spike = IceSpikeEntity.create(level, placement.position.x, placement.position.y,
                    placement.position.z, yaw, placement.delay, level.getRandom().nextInt(), placement.scale);
            level.addFreshEntity(spike);
            if (damageController == null) damageController = spike;
            spawnRowEffects(level, placement);
        }
        if (damageController != null) {
            List<AABB> damageAreas = placements.stream().map(p -> new AABB(
                    p.position.x - 0.9D, p.position.y - 0.4D, p.position.z - 0.9D,
                    p.position.x + 0.9D, p.position.y + 2.8D, p.position.z + 0.9D)).toList();
            damageController.configureCastDamage(player, weapon, damageAreas, forward, 0);
            damageController.applyCastDamageNow();
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FREEZE.get(),
                SoundSource.PLAYERS, FreezeManager.SOUND_VOLUME, 0.82F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.POWDER_SNOW_BREAK,
                SoundSource.PLAYERS, 0.65F, 0.7F);
        CameraShakeDispatcher.shake(level, placements.get(placements.size() / 2).position,
                13.0F, 0.13F, 7);
        Vec3 groundImpact = findSurface(level, player, player.position().add(forward.scale(0.85D)), player.getY());
        if (groundImpact != null) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                            level.getBlockState(net.minecraft.core.BlockPos.containing(groundImpact).below())),
                    groundImpact.x, groundImpact.y + 0.08D, groundImpact.z,
                    12, 0.38D, 0.15D, 0.38D, 0.12D);
            ParticleHelper.spawnLayeredSnowflakes(level, ParticleHelper.SNOWFLAKE_BURST, 0.34F,
                    groundImpact.x, groundImpact.y + 0.12D, groundImpact.z,
                    18, 0.45D, 0.22D, 0.45D, 0.1D);
        }
    }

    private static net.minecraft.world.item.ItemStack findHeldAxe(ServerPlayer player, UUID weaponId) {
        if (weaponId.equals(player.getMainHandItem().get(ModDataComponents.WEAPON_INSTANCE_ID.get()))) {
            return player.getMainHandItem();
        }
        if (weaponId.equals(player.getOffhandItem().get(ModDataComponents.WEAPON_INSTANCE_ID.get()))) {
            return player.getOffhandItem();
        }
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    private static List<SpikePlacement> collectPlacements(ServerLevel level, ServerPlayer player,
            Vec3 forward, Vec3 right) {
        List<SpikePlacement> placements = new ArrayList<>();
        double referenceY = player.getY();
        for (int row = 0; row < ROWS.length; row++) {
            double distance = ROWS[row][0];
            Vec3 center = player.position().add(forward.scale(distance));
            Vec3 centerSurface = findSurface(level, player, center, referenceY);
            if (centerSurface == null) break;
            referenceY = centerSurface.y;
            for (int column = 1; column < ROWS[row].length; column++) {
                double side = ROWS[row][column];
                Vec3 desired = center.add(right.scale(side));
                Vec3 surface = Math.abs(side) < 0.01D ? centerSurface : findSurface(level, player, desired, referenceY);
                if (surface != null) {
                    placements.add(new SpikePlacement(surface, row * 2, ROW_SCALES[row]));
                }
            }
        }
        return placements;
    }

    private static Vec3 findSurface(ServerLevel level, ServerPlayer player, Vec3 desired, double referenceY) {
        BlockHitResult hit = level.clip(new ClipContext(
                new Vec3(desired.x, referenceY + 2.25D, desired.z),
                new Vec3(desired.x, referenceY - 3.5D, desired.z),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || level.getBlockState(hit.getBlockPos()).isAir()
                || level.getBlockState(hit.getBlockPos()).getCollisionShape(level, hit.getBlockPos()).isEmpty()) return null;
        Vec3 surface = hit.getLocation();
        if (Math.abs(surface.y - referenceY) > 2.25D) return null;
        AABB visualSpace = new AABB(surface.x - 0.95D, surface.y + 0.05D, surface.z - 0.95D,
                surface.x + 0.95D, surface.y + 3.2D, surface.z + 0.95D);
        return level.noCollision(player, visualSpace) ? surface : null;
    }

    private static void spawnRowEffects(ServerLevel level, SpikePlacement placement) {
        ParticleHelper.spawnLayeredSnowflakes(level, ParticleHelper.SNOWFLAKE_BURST, 0.4F,
                placement.position.x, placement.position.y + 0.15D,
                placement.position.z, 5, 0.3D, 0.18D, 0.3D, 0.07D);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                placement.position.x, placement.position.y + 0.08D, placement.position.z,
                3, 0.22D, 0.12D, 0.22D, 0.08D);
    }

    private static boolean isHoldingAxe(ServerPlayer player) {
        return player.getMainHandItem().is(ModItems.LEVIATHAN_AXE.get())
                || player.getOffhandItem().is(ModItems.LEVIATHAN_AXE.get());
    }

    private static HumanoidArm getAxeArm(ServerPlayer player) {
        boolean mainHand = player.getMainHandItem().is(ModItems.LEVIATHAN_AXE.get());
        return mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
    }

    private static void sync(ServerPlayer player, int remaining, boolean denied) {
        PacketDistributor.sendToPlayer(player,
                new LeviathanIceSpikesCooldownPayload(remaining, COOLDOWN_TICKS, denied));
    }

    private record SpikePlacement(Vec3 position, int delay, float scale) {}
    private record PendingCast(ResourceKey<Level> dimension, UUID playerId, UUID weaponId, long impactTime,
            Vec3 forward, List<SpikePlacement> placements) {}
    private LeviathanIceSpikesSkill() {}
}
