package com.somake.wotn.entity;

import com.somake.wotn.effect.FreezeManager;
import com.somake.wotn.effect.LeviathanAxeEffects;
import com.somake.wotn.registry.ModEntities;
import com.somake.wotn.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FrozenBlockEntity extends Entity {
    private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(FrozenBlockEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(FrozenBlockEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DEPTH = SynchedEntityData.defineId(FrozenBlockEntity.class,
            EntityDataSerializers.FLOAT);

    public FrozenBlockEntity(EntityType<? extends FrozenBlockEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static FrozenBlockEntity create(Level level, BlockPos blockPos) {
        BlockState state = level.getBlockState(blockPos);
        VoxelShape shape = state.getShape(level, blockPos, CollisionContext.empty());
        AABB bounds = shape.isEmpty() ? new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D) : shape.bounds();

        FrozenBlockEntity frozen = new FrozenBlockEntity(ModEntities.FROZEN_BLOCK.get(), level);
        frozen.entityData.set(WIDTH, (float) Math.max(0.08D, bounds.getXsize()));
        frozen.entityData.set(HEIGHT, (float) Math.max(0.08D, bounds.getYsize()));
        frozen.entityData.set(DEPTH, (float) Math.max(0.08D, bounds.getZsize()));
        frozen.setPos(
                blockPos.getX() + (bounds.minX + bounds.maxX) * 0.5D,
                blockPos.getY() + bounds.minY,
                blockPos.getZ() + (bounds.minZ + bounds.maxZ) * 0.5D);
        return frozen;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(WIDTH, 1.0F);
        builder.define(HEIGHT, 1.0F);
        builder.define(DEPTH, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (!this.level().isClientSide() && this.tickCount >= FreezeManager.DEFAULT_DURATION_TICKS) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.UNFREEZE.get(),
                    SoundSource.BLOCKS, FreezeManager.SOUND_VOLUME, 1.0F);
            if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                LeviathanAxeEffects.spawnShatter(serverLevel, this, SoundSource.BLOCKS);
            }
            this.discard();
        }
    }

    public float getFrozenWidth() {
        return this.entityData.get(WIDTH);
    }

    public float getFrozenHeight() {
        return this.entityData.get(HEIGHT);
    }

    public float getFrozenDepth() {
        return this.entityData.get(DEPTH);
    }

    public void refreshDuration() {
        this.tickCount = 0;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }
}
