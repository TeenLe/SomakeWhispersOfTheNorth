package com.somake.wotn.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.somake.wotn.particle.ParticleHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class GroundWaveEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Boolean> SUBMERGING = SynchedEntityData.defineId(GroundWaveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> WAVE_PHASE = SynchedEntityData.defineId(GroundWaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> INTENSITY = SynchedEntityData.defineId(GroundWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> NORMALIZED_SPEED = SynchedEntityData.defineId(GroundWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SIGNED_TURN = SynchedEntityData.defineId(GroundWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> VARIANT_SEED = SynchedEntityData.defineId(GroundWaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ANTICIPATING = SynchedEntityData.defineId(GroundWaveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<BlockState> SUPPORT_STATE = SynchedEntityData.defineId(GroundWaveEntity.class, EntityDataSerializers.BLOCK_STATE);
    private static final int EMERGE_TICKS = 13;
    private static final int SUBMERGE_TICKS = 11;
    private static final int DEFAULT_LIFETIME_TICKS = 20 * 12;

    private static final RawAnimation EMERGE_ANIMATION = RawAnimation.begin().thenPlay("animation.ground.emerge");
    private static final RawAnimation TRAVEL_ANIMATION = RawAnimation.begin().thenLoop("animation.ground.travel");
    private static final RawAnimation SUBMERGE_ANIMATION = RawAnimation.begin().thenPlay("animation.ground.submerge");
    private static final RawAnimation ANTICIPATE_ANIMATION = RawAnimation.begin().thenLoop("animation.ground.anticipate");

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private int lifetimeTicks = DEFAULT_LIFETIME_TICKS;
    private boolean submerging;
    private int submergeTicks;
    private Vec3 lastTrailPosition;
    private double trailDistance;

    public GroundWaveEntity(EntityType<? extends GroundWaveEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SUBMERGING, false);
        builder.define(WAVE_PHASE, FenrirEntity.PHASE_UNDERGROUND_ACCELERATING);
        builder.define(INTENSITY, 0.35F);
        builder.define(NORMALIZED_SPEED, 0.0F);
        builder.define(SIGNED_TURN, 0.0F);
        builder.define(VARIANT_SEED, 0);
        builder.define(ANTICIPATING, false);
        builder.define(SUPPORT_STATE, Blocks.DIRT.defaultBlockState());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
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

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;
        this.setNoGravity(true);

        Vec3 movement = this.getDeltaMovement();
        if (!this.isSubmerging() && movement.lengthSqr() > 1.0E-6D) {
            this.move(MoverType.SELF, movement);
            if (movement.horizontalDistanceSqr() > 1.0E-6D) {
                this.setYRot((float) Math.toDegrees(Math.atan2(movement.z, movement.x)) - 90.0F);
            }
        }

        if (!this.level().isClientSide() && this.isSubmerging()) {
            this.setDeltaMovement(Vec3.ZERO);
            if (++this.submergeTicks >= SUBMERGE_TICKS) {
                this.discard();
            }
            return;
        }

        if (!this.level().isClientSide() && this.tickCount >= this.lifetimeTicks) {
            this.startSubmerging();
        }

        if (this.level().isClientSide() && !this.isSubmerging() && this.tickCount > EMERGE_TICKS) {
            this.tickClientGroundEffects();
        }

        if (!this.level().isClientSide() && !this.isSubmerging() && this.tickCount > EMERGE_TICKS) {
            this.tickServerGroundAudio();
        }
    }

    private void tickServerGroundAudio() {
        float intensity = this.getVisualIntensity();
        int interval = Math.max(5, 11 - (int) (intensity * 5.0F));
        if ((this.tickCount + Math.floorMod(this.getVariantSeed(), interval)) % interval != 0) {
            return;
        }

        this.playSound(this.isAnticipatingEmergence() ? SoundEvents.RESPAWN_ANCHOR_AMBIENT : SoundEvents.GRAVEL_HIT,
                0.45F + intensity * 0.55F,
                (this.isAnticipatingEmergence() ? 0.48F : 0.62F) + this.random.nextFloat() * 0.1F);
        if (intensity > 0.72F && (this.tickCount & 1) == 0) {
            this.playSound(SoundEvents.STONE_BREAK, 0.25F + intensity * 0.35F, 0.55F + this.random.nextFloat() * 0.12F);
        }
    }

    private void tickClientGroundEffects() {
        float intensity = this.getVisualIntensity();
        Vec3 movement = this.getDeltaMovement();
        Vec3 forward = movement.horizontalDistanceSqr() < 1.0E-5D
                ? new Vec3(-Math.sin(this.getYRot() * Math.PI / 180.0D), 0.0D, Math.cos(this.getYRot() * Math.PI / 180.0D))
                : new Vec3(movement.x, 0.0D, movement.z).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        int dustCount = 1 + (int) (intensity * 2.0F);

        for (int i = 0; i < dustCount; i++) {
            double side = (this.random.nextDouble() - 0.5D) * (3.0D + intensity * 3.0D);
            double back = this.random.nextDouble() * 2.2D;
            Vec3 spawn = this.position().subtract(forward.scale(back)).add(right.scale(side));
            this.level().addParticle(ParticleTypes.DUST_PLUME, spawn.x, spawn.y + 0.08D, spawn.z,
                    -forward.x * 0.035D, 0.015D + this.random.nextDouble() * 0.035D, -forward.z * 0.035D);
        }

        if ((this.tickCount + Math.floorMod(this.getVariantSeed(), 4)) % Math.max(2, 5 - (int) (intensity * 3.0F)) == 0) {
            BlockPos sourcePos = BlockPos.containing(this.getX(), this.getY() - 0.1D, this.getZ());
            BlockParticleOption fragment = new BlockParticleOption(ParticleTypes.BLOCK, this.getSupportState());
            int fragments = 1 + (int) (intensity * 3.0F);
            for (int i = 0; i < fragments; i++) {
                double sideVelocity = (this.random.nextDouble() - 0.5D) * (0.18D + intensity * 0.18D);
                this.level().addParticle(fragment, this.getX(), this.getY() + 0.12D, this.getZ(),
                        right.x * sideVelocity - forward.x * 0.05D,
                        0.18D + this.random.nextDouble() * (0.22D + intensity * 0.22D),
                        right.z * sideVelocity - forward.z * 0.05D);
            }
        }

        if (this.lastTrailPosition == null) {
            this.lastTrailPosition = this.position();
            return;
        }
        this.trailDistance += this.position().distanceTo(this.lastTrailPosition);
        this.lastTrailPosition = this.position();
        double spacing = 0.8D - intensity * 0.4D;
        if (this.trailDistance >= spacing) {
            this.trailDistance %= spacing;
            ParticleHelper.spawnGroundWaveTrail(this.level(), this.getSupportState(), this.blockPosition().below(),
                    this.getX() - forward.x * 1.4D, this.getY(), this.getZ() - forward.z * 1.4D, intensity);
        }
    }

    public void setLifetimeTicks(int lifetimeTicks) {
        this.lifetimeTicks = Math.max(EMERGE_TICKS + SUBMERGE_TICKS, lifetimeTicks);
    }

    public void startSubmerging() {
        if (this.isSubmerging()) {
            return;
        }

        this.submerging = true;
        this.entityData.set(SUBMERGING, true);
        this.submergeTicks = 0;
        this.setDeltaMovement(Vec3.ZERO);
    }

    public boolean isSubmerging() {
        return this.entityData.get(SUBMERGING);
    }

    public void updateVisualState(int phase, float normalizedSpeed, float intensity, float signedTurn,
            BlockState supportState, boolean anticipating) {
        this.entityData.set(WAVE_PHASE, phase);
        this.entityData.set(NORMALIZED_SPEED, net.minecraft.util.Mth.clamp(normalizedSpeed, 0.0F, 1.0F));
        this.entityData.set(INTENSITY, net.minecraft.util.Mth.clamp(intensity, 0.0F, 1.0F));
        this.entityData.set(SIGNED_TURN, net.minecraft.util.Mth.clamp(signedTurn, -1.0F, 1.0F));
        this.entityData.set(SUPPORT_STATE, supportState.isAir() ? Blocks.DIRT.defaultBlockState() : supportState);
        this.entityData.set(ANTICIPATING, anticipating);
    }

    public void setVariantSeed(int seed) {
        this.entityData.set(VARIANT_SEED, seed);
    }

    public float getVisualIntensity() { return this.entityData.get(INTENSITY); }
    public float getNormalizedSpeed() { return this.entityData.get(NORMALIZED_SPEED); }
    public float getSignedTurn() { return this.entityData.get(SIGNED_TURN); }
    public int getVariantSeed() { return this.entityData.get(VARIANT_SEED); }
    public boolean isAnticipatingEmergence() { return this.entityData.get(ANTICIPATING); }
    public BlockState getSupportState() { return this.entityData.get(SUPPORT_STATE); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GroundWaveEntity>("Base", 2, state -> {
            if (this.isSubmerging()) {
                state.setControllerSpeed(1.0F);
                return state.setAndContinue(SUBMERGE_ANIMATION);
            }

            if (this.isAnticipatingEmergence()) {
                state.setControllerSpeed(1.0F);
                return state.setAndContinue(ANTICIPATE_ANIMATION);
            }

            if (state.renderState().getAnimatableAge() <= EMERGE_TICKS) {
                state.setControllerSpeed(1.0F);
                return state.setAndContinue(EMERGE_ANIMATION);
            }

            float variation = 0.96F + Math.floorMod(this.getVariantSeed(), 7) * 0.012F;
            state.setControllerSpeed((0.9F + this.getNormalizedSpeed() * 0.18F) * variation);
            return state.setAndContinue(TRAVEL_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }
}
