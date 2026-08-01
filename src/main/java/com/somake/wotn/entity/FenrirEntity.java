package com.somake.wotn.entity;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.constant.DataTickets;
import com.geckolib.util.GeckoLibUtil;
import com.somake.wotn.network.CameraShakeDispatcher;
import com.somake.wotn.particle.ParticleHelper;
import com.somake.wotn.particle.ImpactRingParticleData.RingBehavior;
import com.somake.wotn.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;

public class FenrirEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(FenrirEntity.class, EntityDataSerializers.INT);

    public static final int PHASE_SURFACE = 0;
    public static final int PHASE_BURROWING = 1;
    public static final int PHASE_UNDERGROUND_ACCELERATING = 2;
    public static final int PHASE_UNDERGROUND_HUNTING = 3;
    public static final int PHASE_UNDERGROUND_FINAL_CHARGE = 4;
    public static final int PHASE_EMERGING = 5;
    public static final int PHASE_RECOVERY = 6;
    public static final int PHASE_EMERGENCE_ANTICIPATION = 7;

    private static final int BURROW_DURATION = 20;
    private static final int ACCELERATION_DURATION = 18;
    private static final int MAX_UNDERGROUND_DURATION = 220;
    private static final int FINAL_CHARGE_MIN_DURATION = 14;
    private static final int FINAL_CHARGE_MAX_DURATION = 96;
    private static final int REQUIRED_PASSES = 2;
    private static final int PASS_COOLDOWN_TICKS = 12;
    private static final int EMERGE_IMPACT_TICK = 10;
    private static final int EMERGE_DURATION = 24;
    private static final int RECOVERY_DURATION = 26;
    private static final int EMERGENCE_ANTICIPATION_DURATION = 12;
    private static final int BURROW_COOLDOWN = 220;
    private static final double BURROW_START_SPEED = 0.12D;
    private static final double BURROW_HUNT_SPEED = 0.44D;
    private static final double BURROW_FINAL_SPEED = 0.58D;
    private static final double BURROW_ACCELERATION = 0.018D;
    private static final double PASS_RADIUS = 3.4D;
    private static final double PASS_ARM_DISTANCE = 7.0D;
    private static final double EMERGE_DAMAGE_RADIUS = 3.75D;

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.fenrir.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.fenrir.walk");
    private static final RawAnimation BURROW_ANIMATION = RawAnimation.begin().thenPlay("animation.fenrir.burrow");
    private static final RawAnimation EMERGE_ANIMATION = RawAnimation.begin().thenPlay("animation.fenrir.emerge");
    private static final RawAnimation RECOVERY_ANIMATION = RawAnimation.begin().thenPlay("animation.fenrir.recovery");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlay("animation.fenrir.die");
    private static final RawAnimation EMERGE_ANTICIPATION_ANIMATION = RawAnimation.begin().thenLoop("animation.fenrir.emerge_anticipation");

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(UUID.randomUUID(), this.getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);

    private int phaseTicks;
    private int burrowCooldown = 60;
    private boolean emergeImpactApplied;
    private UUID groundWaveUuid;
    private Vec3 burrowDirection = Vec3.ZERO;
    private Vec3 previousTargetPosition;
    private Vec3 lastSurfacePosition;
    private double burrowSpeed;
    private int undergroundTicks;
    private int burrowPasses;
    private int passCooldown;
    private int steeringBiasTicks;
    private int skillSelectionDelay = 20;
    private float steeringBiasDegrees;
    private float visualTurn;
    private boolean passArmed;

    public FenrirEntity(EntityType<? extends FenrirEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 100;
        this.bossEvent.setDarkenScreen(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 260.0D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.STEP_HEIGHT, 1.25D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, PHASE_SURFACE);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(PHASE, PHASE_SURFACE);
        this.noPhysics = false;
        this.setNoGravity(false);
        this.setInvisible(false);
        this.groundWaveUuid = null;
        this.burrowCooldown = 40;
        this.skillSelectionDelay = 20;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new FenrirBurrowGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 14.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide()) {
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
            if (this.burrowCooldown > 0 && this.getPhase() == PHASE_SURFACE) {
                this.burrowCooldown--;
            }
            if (this.skillSelectionDelay > 0 && this.getPhase() == PHASE_SURFACE) {
                this.skillSelectionDelay--;
            }
            if (this.getPhase() != PHASE_SURFACE) {
                this.phaseTicks++;
                this.tickBurrowSkill();
            }
        }
    }

    private void tickBurrowSkill() {
        switch (this.getPhase()) {
            case PHASE_BURROWING -> this.tickBurrowing();
            case PHASE_UNDERGROUND_ACCELERATING, PHASE_UNDERGROUND_HUNTING,
                    PHASE_UNDERGROUND_FINAL_CHARGE -> this.tickUnderground();
            case PHASE_EMERGENCE_ANTICIPATION -> this.tickEmergenceAnticipation();
            case PHASE_EMERGING -> this.tickEmerging();
            case PHASE_RECOVERY -> this.tickRecovery();
            default -> this.finishBurrowSkill();
        }
    }

    private void startBurrowSkill() {
        this.phaseTicks = 0;
        this.emergeImpactApplied = false;
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.entityData.set(PHASE, PHASE_BURROWING);
        this.playSound(SoundEvents.RAVAGER_ROAR, 1.4F, 0.78F);
    }

    private void tickBurrowing() {
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);

        if (this.phaseTicks == 8) {
            ParticleHelper.spawnGroundDebrisBurst(this.level(), this.getX(), this.getY(), this.getZ(), 1.5F, 36, 0.24F);
            this.playSound(SoundEvents.GRAVEL_BREAK, 1.3F, 0.62F);
        }

        if (this.phaseTicks >= BURROW_DURATION) {
            this.enterUnderground();
        }
    }

    private void enterUnderground() {
        this.phaseTicks = 0;
        this.undergroundTicks = 0;
        this.burrowPasses = 0;
        this.passCooldown = 0;
        this.passArmed = false;
        this.burrowSpeed = BURROW_START_SPEED;
        this.previousTargetPosition = null;
        this.lastSurfacePosition = this.findSurfacePosition(this.position());
        this.burrowDirection = this.getInitialBurrowDirection();
        this.randomizeSteeringBias();
        this.entityData.set(PHASE, PHASE_UNDERGROUND_ACCELERATING);
        this.setInvisible(true);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.spawnGroundWave();
    }

    private void tickUnderground() {
        this.getNavigation().stop();
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvisible(true);

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.beginEmergenceAnticipation();
            return;
        }

        this.undergroundTicks++;
        if (this.passCooldown > 0) {
            this.passCooldown--;
        }

        Vec3 oldPosition = this.position();
        Vec3 oldTargetPosition = this.previousTargetPosition == null ? target.position() : this.previousTargetPosition;
        Vec3 predictedTarget = this.getPredictedTargetPosition(target);
        Vec3 desiredDirection = this.horizontalDirection(predictedTarget.subtract(this.position()));
        float maxTurn = this.getBurrowMaxTurnDegrees();
        Vec3 previousDirection = this.burrowDirection;
        this.burrowDirection = this.rotateToward(this.burrowDirection, desiredDirection, maxTurn);
        double turnCross = previousDirection.x * this.burrowDirection.z - previousDirection.z * this.burrowDirection.x;
        double turnDot = Mth.clamp(previousDirection.dot(this.burrowDirection), -1.0D, 1.0D);
        float turnAngle = (float) Math.atan2(turnCross, turnDot);
        float normalizedTurn = Mth.clamp(turnAngle / (maxTurn * Mth.DEG_TO_RAD), -1.0F, 1.0F);
        this.visualTurn = Mth.lerp(0.35F, this.visualTurn, normalizedTurn);
        this.burrowSpeed = Mth.clamp(this.burrowSpeed + BURROW_ACCELERATION,
                BURROW_START_SPEED, this.getBurrowTargetSpeed());

        Vec3 movement = this.burrowDirection.scale(this.burrowSpeed);
        Vec3 nextPosition = this.position().add(movement);
        Vec3 nextSurfacePosition = this.findSurfacePosition(nextPosition);
        if (nextSurfacePosition == null) {
            this.beginEmerging();
            return;
        }
        this.lastSurfacePosition = nextSurfacePosition;
        this.setDeltaMovement(movement.x, 0.0D, movement.z);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.updateBurrowRotation();
        this.updateGroundWave();

        double targetDistanceSqr = this.horizontalDistanceSqr(this.position(), target.position());
        if (targetDistanceSqr >= PASS_ARM_DISTANCE * PASS_ARM_DISTANCE) {
            this.passArmed = true;
        }

        boolean passedTarget = this.detectTargetPass(oldPosition, this.position(), oldTargetPosition, target.position());
        this.previousTargetPosition = target.position();

        if (passedTarget) {
            this.passArmed = false;
            this.passCooldown = PASS_COOLDOWN_TICKS;
            this.burrowPasses++;
            this.playPassEffects();
            if (this.burrowPasses >= REQUIRED_PASSES) {
                this.startFinalCharge();
            }
        }

        if (this.getPhase() == PHASE_UNDERGROUND_ACCELERATING && this.phaseTicks >= ACCELERATION_DURATION) {
            this.phaseTicks = 0;
            this.entityData.set(PHASE, PHASE_UNDERGROUND_HUNTING);
        }

        if (this.getPhase() == PHASE_UNDERGROUND_FINAL_CHARGE) {
            boolean finalPass = passedTarget && this.phaseTicks >= FINAL_CHARGE_MIN_DURATION;
            if (finalPass || this.phaseTicks >= FINAL_CHARGE_MAX_DURATION) {
                this.beginEmergenceAnticipation();
                return;
            }
        }

        if (this.undergroundTicks >= MAX_UNDERGROUND_DURATION) {
            this.beginEmergenceAnticipation();
        }
    }

    private void beginEmergenceAnticipation() {
        if (this.getPhase() == PHASE_EMERGENCE_ANTICIPATION) {
            return;
        }
        this.phaseTicks = 0;
        this.entityData.set(PHASE, PHASE_EMERGENCE_ANTICIPATION);
        this.burrowSpeed = 0.0D;
        this.setDeltaMovement(Vec3.ZERO);
        GroundWaveEntity wave = this.getGroundWave();
        if (wave != null) {
            wave.updateVisualState(this.getPhase(), 0.0F, 1.0F, this.visualTurn, this.getSupportState(), true);
        }
        this.playSound(SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.2F, 0.55F);
    }

    private void tickEmergenceAnticipation() {
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.updateGroundWave();
        if (this.phaseTicks == 6 && this.level() instanceof ServerLevel serverLevel) {
            CameraShakeDispatcher.shake(serverLevel, this.lastSurfacePosition == null ? this.position() : this.lastSurfacePosition,
                    14.0F, 0.18F, 8);
        }
        if (this.phaseTicks >= EMERGENCE_ANTICIPATION_DURATION) {
            this.beginEmerging();
        }
    }

    private void startFinalCharge() {
        if (this.getPhase() == PHASE_UNDERGROUND_FINAL_CHARGE) {
            return;
        }
        this.phaseTicks = 0;
        this.passArmed = false;
        this.visualTurn = 0.0F;
        this.burrowSpeed = Math.max(this.burrowSpeed, BURROW_HUNT_SPEED);
        this.steeringBiasDegrees = 0.0F;
        this.steeringBiasTicks = FINAL_CHARGE_MAX_DURATION;
        this.entityData.set(PHASE, PHASE_UNDERGROUND_FINAL_CHARGE);
        this.playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 0.52F);
    }

    private Vec3 getInitialBurrowDirection() {
        LivingEntity target = this.getTarget();
        Vec3 desired = target == null ? this.getLookAngle() : target.position().subtract(this.position());
        Vec3 horizontal = this.horizontalDirection(desired);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            float yaw = (this.getYRot() + 90.0F) * Mth.DEG_TO_RAD;
            return new Vec3(Mth.cos(yaw), 0.0D, Mth.sin(yaw));
        }
        return horizontal;
    }

    private Vec3 getPredictedTargetPosition(LivingEntity target) {
        if (this.steeringBiasTicks-- <= 0 && this.getPhase() != PHASE_UNDERGROUND_FINAL_CHARGE) {
            this.randomizeSteeringBias();
        }

        double leadTicks = this.getPhase() == PHASE_UNDERGROUND_FINAL_CHARGE ? 5.0D : 8.0D;
        Vec3 targetVelocity = target.getDeltaMovement();
        Vec3 prediction = new Vec3(targetVelocity.x, 0.0D, targetVelocity.z).scale(leadTicks);
        if (prediction.horizontalDistanceSqr() > 16.0D) {
            prediction = prediction.normalize().scale(4.0D);
        }

        Vec3 predicted = target.position().add(prediction);
        if (this.steeringBiasDegrees == 0.0F) {
            return predicted;
        }

        Vec3 toTarget = this.horizontalDirection(predicted.subtract(this.position()));
        Vec3 biased = this.rotateHorizontal(toTarget, this.steeringBiasDegrees * Mth.DEG_TO_RAD);
        double distance = Math.sqrt(this.horizontalDistanceSqr(this.position(), predicted));
        return this.position().add(biased.scale(distance));
    }

    private void randomizeSteeringBias() {
        this.steeringBiasDegrees = Mth.lerp(this.random.nextFloat(), -10.0F, 10.0F);
        this.steeringBiasTicks = 14 + this.random.nextInt(15);
    }

    private double getBurrowTargetSpeed() {
        return this.getPhase() == PHASE_UNDERGROUND_FINAL_CHARGE ? BURROW_FINAL_SPEED : BURROW_HUNT_SPEED;
    }

    private float getBurrowMaxTurnDegrees() {
        if (this.getPhase() == PHASE_UNDERGROUND_FINAL_CHARGE) {
            return this.phaseTicks < 34 ? 5.0F : 2.4F;
        }
        if (this.getPhase() == PHASE_UNDERGROUND_ACCELERATING) {
            return 6.0F;
        }
        float speedRatio = (float) Mth.clamp(this.burrowSpeed / BURROW_HUNT_SPEED, 0.0D, 1.0D);
        return Mth.lerp(speedRatio, 5.0F, 2.8F);
    }

    private Vec3 rotateToward(Vec3 current, Vec3 desired, float maxTurnDegrees) {
        if (current.horizontalDistanceSqr() < 1.0E-4D) {
            return desired;
        }
        if (desired.horizontalDistanceSqr() < 1.0E-4D) {
            return current;
        }

        Vec3 normalizedCurrent = this.horizontalDirection(current);
        Vec3 normalizedDesired = this.horizontalDirection(desired);
        double cross = normalizedCurrent.x * normalizedDesired.z - normalizedCurrent.z * normalizedDesired.x;
        double dot = Mth.clamp(normalizedCurrent.dot(normalizedDesired), -1.0D, 1.0D);
        double angle = Math.atan2(cross, dot);
        double maxTurn = maxTurnDegrees * Mth.DEG_TO_RAD;
        return this.rotateHorizontal(normalizedCurrent, Mth.clamp(angle, -maxTurn, maxTurn));
    }

    private Vec3 rotateHorizontal(Vec3 direction, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(direction.x * cos - direction.z * sin, 0.0D,
                direction.x * sin + direction.z * cos).normalize();
    }

    private Vec3 horizontalDirection(Vec3 direction) {
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        return horizontal.lengthSqr() < 1.0E-4D ? Vec3.ZERO : horizontal.normalize();
    }

    private void updateBurrowRotation() {
        float yaw = (float) (Mth.atan2(this.burrowDirection.z, this.burrowDirection.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
    }

    private boolean detectTargetPass(Vec3 oldPosition, Vec3 newPosition, Vec3 oldTarget, Vec3 newTarget) {
        if (!this.passArmed || this.passCooldown > 0) {
            return false;
        }

        Vec3 relativeStart = new Vec3(oldPosition.x - oldTarget.x, 0.0D, oldPosition.z - oldTarget.z);
        Vec3 relativeEnd = new Vec3(newPosition.x - newTarget.x, 0.0D, newPosition.z - newTarget.z);
        Vec3 relativeDelta = relativeEnd.subtract(relativeStart);
        double deltaLengthSqr = relativeDelta.horizontalDistanceSqr();
        if (deltaLengthSqr < 1.0E-6D) {
            return false;
        }

        double closestTime = Mth.clamp(-relativeStart.dot(relativeDelta) / deltaLengthSqr, 0.0D, 1.0D);
        Vec3 closest = relativeStart.add(relativeDelta.scale(closestTime));
        boolean crossedForwardPlane = relativeStart.dot(this.burrowDirection) < 0.0D
                && relativeEnd.dot(this.burrowDirection) >= 0.0D;
        return crossedForwardPlane && closest.horizontalDistanceSqr() <= PASS_RADIUS * PASS_RADIUS;
    }

    private void playPassEffects() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ParticleHelper.spawnGroundDebrisBurst(this.level(), this.getX(), this.getY(), this.getZ(),
                1.1F + this.burrowPasses * 0.25F, 20 + this.burrowPasses * 8, 0.2F);
        CameraShakeDispatcher.shake(serverLevel, this.position(), 12.0F, 0.18F + this.burrowPasses * 0.08F, 6);
        this.playSound(SoundEvents.GRAVEL_BREAK, 1.1F, 0.58F + this.random.nextFloat() * 0.1F);
    }

    private double horizontalDistanceSqr(Vec3 first, Vec3 second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }

    private void beginEmerging() {
        this.stopGroundWave();
        this.phaseTicks = 0;
        this.emergeImpactApplied = false;
        this.entityData.set(PHASE, PHASE_EMERGING);
        Vec3 emergePosition = this.lastSurfacePosition == null ? this.findSurfacePosition(this.position()) : this.lastSurfacePosition;
        if (emergePosition != null) {
            this.setPos(emergePosition.x, emergePosition.y, emergePosition.z);
        }
        this.noPhysics = false;
        this.setNoGravity(false);
        this.setInvisible(false);
        this.setDeltaMovement(Vec3.ZERO);
        this.burrowDirection = Vec3.ZERO;
        this.previousTargetPosition = null;
        this.burrowSpeed = 0.0D;
        this.undergroundTicks = 0;
        this.burrowPasses = 0;
        this.passCooldown = 0;
        this.passArmed = false;
        this.skillSelectionDelay = 20;
        this.playSound(SoundEvents.GRAVEL_BREAK, 0.9F, 0.52F);
    }

    private void tickEmerging() {
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);

        if (!this.emergeImpactApplied && this.phaseTicks >= EMERGE_IMPACT_TICK) {
            this.emergeImpactApplied = true;
            this.performEmergeImpact();
        }

        if (this.phaseTicks >= EMERGE_DURATION) {
            this.phaseTicks = 0;
            this.entityData.set(PHASE, PHASE_RECOVERY);
        }
    }

    private void performEmergeImpact() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.1F, 0.68F);
        ParticleHelper.spawnGroundDebrisBurst(this.level(), this.getX(), this.getY(), this.getZ(), 2.2F, 48, 0.34F);

        List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(EMERGE_DAMAGE_RADIUS, 2.0D, EMERGE_DAMAGE_RADIUS),
                target -> target != this && target.isAlive() && !this.isAlliedTo(target)
                        && this.horizontalDistanceSqr(this.position(), target.position()) <= EMERGE_DAMAGE_RADIUS * EMERGE_DAMAGE_RADIUS);

        for (LivingEntity victim : victims) {
            if (!this.doHurtTarget(serverLevel, victim)) {
                continue;
            }

            Vec3 push = victim.position().subtract(this.position());
            Vec3 horizontal = new Vec3(push.x, 0.0D, push.z);
            if (horizontal.lengthSqr() > 1.0E-4D) {
                Vec3 knockback = horizontal.normalize().scale(1.25D);
                victim.push(knockback.x, 0.48D, knockback.z);
            } else {
                victim.push(0.0D, 0.48D, 0.0D);
            }
        }

        ParticleHelper.spawnImpactRing(this.level(), this.getX(), this.getY() + 0.12D, this.getZ(),
                0.72F, 0.84F, 0.92F, 0.95F, 1.2F, 4.8F, 18, RingBehavior.GROW);
        CameraShakeDispatcher.shake(serverLevel, this.position(), 22.0F, 0.75F, 12);
    }

    private void tickRecovery() {
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        if (this.phaseTicks >= RECOVERY_DURATION) {
            this.finishBurrowSkill();
        }
    }

    private void finishBurrowSkill() {
        this.stopGroundWave();
        this.phaseTicks = 0;
        this.burrowCooldown = BURROW_COOLDOWN;
        this.entityData.set(PHASE, PHASE_SURFACE);
        this.noPhysics = false;
        this.setNoGravity(false);
        this.setInvisible(false);
        this.setDeltaMovement(Vec3.ZERO);
    }

    private void spawnGroundWave() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        GroundWaveEntity wave = ModEntities.GROUND_WAVE.get().create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (wave == null) {
            return;
        }

        Vec3 surfacePosition = this.lastSurfacePosition == null ? this.findSurfacePosition(this.position()) : this.lastSurfacePosition;
        if (surfacePosition == null) {
            surfacePosition = this.position();
        }
        wave.setPos(surfacePosition.x, surfacePosition.y + 0.05D, surfacePosition.z);
        wave.setYRot(this.getYRot());
        wave.setLifetimeTicks(MAX_UNDERGROUND_DURATION + 30);
        wave.setVariantSeed(this.random.nextInt());
        wave.updateVisualState(this.getPhase(), 0.0F, 0.35F, 0.0F, this.getSupportState(), false);
        serverLevel.addFreshEntity(wave);
        this.groundWaveUuid = wave.getUUID();
    }

    private void updateGroundWave() {
        Vec3 surfacePosition = this.findSurfacePosition(this.position());
        if (surfacePosition != null) {
            this.lastSurfacePosition = surfacePosition;
        } else {
            surfacePosition = this.lastSurfacePosition;
        }

        GroundWaveEntity wave = this.getGroundWave();
        if (wave == null) {
            this.spawnGroundWave();
            wave = this.getGroundWave();
        }
        if (wave != null && surfacePosition != null) {
            Vec3 targetPosition = new Vec3(surfacePosition.x, surfacePosition.y + 0.05D, surfacePosition.z);
            Vec3 correction = targetPosition.subtract(wave.position());
            double verticalMovement = Mth.clamp(correction.y, -0.18D, 0.18D);
            wave.setDeltaMovement(correction.x, verticalMovement, correction.z);
            float normalizedSpeed = (float) Mth.clamp((this.burrowSpeed - BURROW_START_SPEED)
                    / (BURROW_FINAL_SPEED - BURROW_START_SPEED), 0.0D, 1.0D);
            float phaseIntensity = switch (this.getPhase()) {
                case PHASE_UNDERGROUND_ACCELERATING -> 0.52F;
                case PHASE_UNDERGROUND_HUNTING -> 0.76F;
                case PHASE_UNDERGROUND_FINAL_CHARGE -> 1.0F;
                case PHASE_EMERGENCE_ANTICIPATION -> 1.0F;
                default -> 0.35F;
            };
            float intensity = Mth.clamp(phaseIntensity * (0.62F + normalizedSpeed * 0.38F), 0.0F, 1.0F);
            wave.updateVisualState(this.getPhase(), normalizedSpeed, intensity, this.visualTurn,
                    this.getSupportState(), this.getPhase() == PHASE_EMERGENCE_ANTICIPATION);
        }

        int shakeCounter = this.getPhase() == PHASE_EMERGENCE_ANTICIPATION ? this.phaseTicks : this.undergroundTicks;
        if ((shakeCounter & 7) == 0 && this.level() instanceof ServerLevel serverLevel) {
            LivingEntity target = this.getTarget();
            if (target != null) {
                double distance = Math.sqrt(this.horizontalDistanceSqr(this.position(), target.position()));
                if (distance < 9.0D) {
                    float proximity = 1.0F - Mth.clamp((float) (distance / 9.0D), 0.0F, 1.0F);
                    CameraShakeDispatcher.shake(serverLevel, surfacePosition, 11.0F,
                            0.035F + proximity * 0.08F, 5);
                }
            }
        }
    }

    private BlockState getSupportState() {
        Vec3 surface = this.lastSurfacePosition == null ? this.position() : this.lastSurfacePosition;
        return this.level().getBlockState(BlockPos.containing(surface.x, surface.y - 0.1D, surface.z));
    }

    private Vec3 findSurfacePosition(Vec3 desiredPosition) {
        BlockPos center = BlockPos.containing(desiredPosition);
        int referenceY = center.getY();

        for (int verticalDistance = 0; verticalDistance <= 6; verticalDistance++) {
            int[] verticalOffsets = verticalDistance == 0 ? new int[] {0} : new int[] {-verticalDistance, verticalDistance};
            for (int verticalOffset : verticalOffsets) {
                BlockPos supportPos = new BlockPos(center.getX(), referenceY + verticalOffset - 1, center.getZ());
                if (this.level().getBlockState(supportPos).getCollisionShape(this.level(), supportPos).isEmpty()) {
                    continue;
                }

                Vec3 candidate = new Vec3(desiredPosition.x, supportPos.getY() + 1.0D, desiredPosition.z);
                if (this.isSafeEmergencePosition(candidate)) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private boolean isSafeEmergencePosition(Vec3 candidate) {
        Vec3 offset = candidate.subtract(this.position());
        return this.level().noCollision(this, this.getBoundingBox().move(offset));
    }

    private GroundWaveEntity getGroundWave() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.groundWaveUuid == null) {
            return null;
        }
        Entity entity = serverLevel.getEntity(this.groundWaveUuid);
        return entity instanceof GroundWaveEntity wave ? wave : null;
    }

    private void stopGroundWave() {
        GroundWaveEntity wave = this.getGroundWave();
        if (wave != null) {
            wave.startSubmerging();
        }
        this.groundWaveUuid = null;
    }

    public int getPhase() {
        return this.entityData.get(PHASE);
    }

    public boolean isUsingBurrowSkill() {
        return this.getPhase() != PHASE_SURFACE;
    }

    public boolean isUnderground() {
        int phase = this.getPhase();
        return phase == PHASE_UNDERGROUND_ACCELERATING
                || phase == PHASE_UNDERGROUND_HUNTING
                || phase == PHASE_UNDERGROUND_FINAL_CHARGE
                || phase == PHASE_EMERGENCE_ANTICIPATION;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if ((this.getPhase() == PHASE_BURROWING || this.isUnderground())
                && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<FenrirEntity>("Base", 2, state -> {
            if (state.getDataOrDefault(DataTickets.IS_DEAD_OR_DYING, false)) {
                return state.setAndContinue(DEATH_ANIMATION);
            }
            return switch (this.getPhase()) {
                case PHASE_BURROWING -> state.setAndContinue(BURROW_ANIMATION);
                case PHASE_EMERGING -> state.setAndContinue(EMERGE_ANIMATION);
                case PHASE_RECOVERY -> state.setAndContinue(RECOVERY_ANIMATION);
                case PHASE_EMERGENCE_ANTICIPATION -> state.setAndContinue(EMERGE_ANTICIPATION_ANIMATION);
                case PHASE_UNDERGROUND_ACCELERATING, PHASE_UNDERGROUND_HUNTING,
                        PHASE_UNDERGROUND_FINAL_CHARGE -> state.setAndContinue(IDLE_ANIMATION);
                default -> state.isMoving() ? state.setAndContinue(WALK_ANIMATION) : state.setAndContinue(IDLE_ANIMATION);
            };
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    private static final class FenrirBurrowGoal extends Goal {
        private final FenrirEntity fenrir;

        private FenrirBurrowGoal(FenrirEntity fenrir) {
            this.fenrir = fenrir;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.fenrir.getTarget();
            if (target == null || !target.isAlive() || this.fenrir.isUsingBurrowSkill()
                    || this.fenrir.burrowCooldown > 0 || this.fenrir.skillSelectionDelay > 0 || !this.fenrir.onGround()) {
                return false;
            }
            double distance = Math.sqrt(this.fenrir.distanceToSqr(target));
            float selectionChance = distance < 5.0D ? 0.22F : distance <= 18.0D ? 0.4F : 0.28F;
            this.fenrir.skillSelectionDelay = 20;
            return distance <= 36.0D && this.fenrir.random.nextFloat() < selectionChance;
        }

        @Override
        public boolean canContinueToUse() {
            return this.fenrir.isUsingBurrowSkill();
        }

        @Override
        public void start() {
            this.fenrir.startBurrowSkill();
        }
    }
}
