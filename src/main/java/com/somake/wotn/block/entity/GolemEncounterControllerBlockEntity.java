package com.somake.wotn.block.entity;

import java.util.UUID;

import com.somake.wotn.entity.GolemEntity;
import com.somake.wotn.registry.ModBlockEntities;
import com.somake.wotn.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public final class GolemEncounterControllerBlockEntity extends BlockEntity {
    private static final int WAITING = 0;
    private static final int ACTIVE = 1;
    private static final int DEFEATED = 2;

    private int encounterState = WAITING;
    private UUID golemUuid;
    private int triggerRadius = 12;
    private double spawnOffsetX = 0.5D;
    private double spawnOffsetY = 1.0D;
    private double spawnOffsetZ = 0.5D;
    private int tickCooldown;
    private int missingGolemTicks;

    public GolemEncounterControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GOLEM_ENCOUNTER_CONTROLLER.get(), pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state,
            GolemEncounterControllerBlockEntity controller) {
        if (controller.encounterState == DEFEATED || controller.tickCooldown-- > 0) return;
        controller.tickCooldown = 10;
        if (controller.encounterState == WAITING) {
            if (controller.hasPlayerInRange(level)) controller.spawnGolem(level);
            return;
        }
        if (controller.golemUuid != null && level.getEntity(controller.golemUuid) instanceof GolemEntity golem
                && golem.isAlive()) {
            controller.missingGolemTicks = 0;
            return;
        }
        GolemEntity nearbyGolem = level.getEntitiesOfClass(GolemEntity.class,
                new AABB(pos).inflate(controller.triggerRadius * 2.0D), GolemEntity::isAlive)
                .stream().min(java.util.Comparator.comparingDouble(golem -> golem.distanceToSqr(pos.getCenter())))
                .orElse(null);
        if (nearbyGolem != null) {
            controller.golemUuid = nearbyGolem.getUUID();
            controller.missingGolemTicks = 0;
            controller.setChanged();
            return;
        }
        if (++controller.missingGolemTicks >= 6) {
            controller.encounterState = DEFEATED;
            controller.golemUuid = null;
            controller.setChanged();
        }
    }

    private boolean hasPlayerInRange(ServerLevel level) {
        AABB trigger = new AABB(worldPosition).inflate(triggerRadius);
        return !level.getEntitiesOfClass(Player.class, trigger,
                player -> !player.isSpectator() && player.isAlive()).isEmpty();
    }

    private void spawnGolem(ServerLevel level) {
        GolemEntity golem = ModEntities.GOLEM.get().create(level, EntitySpawnReason.STRUCTURE);
        if (golem == null) return;
        golem.snapTo(worldPosition.getX() + spawnOffsetX, worldPosition.getY() + spawnOffsetY,
                worldPosition.getZ() + spawnOffsetZ, 0.0F, 0.0F);
        golem.setPersistenceRequired();
        if (!level.addFreshEntity(golem)) return;
        golemUuid = golem.getUUID();
        encounterState = ACTIVE;
        missingGolemTicks = 0;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("EncounterState", encounterState);
        output.putInt("TriggerRadius", triggerRadius);
        output.putDouble("SpawnOffsetX", spawnOffsetX);
        output.putDouble("SpawnOffsetY", spawnOffsetY);
        output.putDouble("SpawnOffsetZ", spawnOffsetZ);
        if (golemUuid != null) output.store("GolemUuid", UUIDUtil.CODEC, golemUuid);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        encounterState = Math.max(WAITING, Math.min(DEFEATED, input.getIntOr("EncounterState", WAITING)));
        triggerRadius = Math.max(2, Math.min(64, input.getIntOr("TriggerRadius", 12)));
        spawnOffsetX = input.getDoubleOr("SpawnOffsetX", 0.5D);
        spawnOffsetY = input.getDoubleOr("SpawnOffsetY", 1.0D);
        spawnOffsetZ = input.getDoubleOr("SpawnOffsetZ", 0.5D);
        golemUuid = input.read("GolemUuid", UUIDUtil.CODEC).orElse(null);
    }
}
