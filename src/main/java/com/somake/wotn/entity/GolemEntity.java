package com.somake.wotn.entity;

import java.util.EnumSet;
import java.util.List;

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
import com.somake.wotn.registry.ModSounds;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GolemEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> ATTACK_STATE = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHARGE_PHASE = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);

    private static final int ATTACK_STATE_IDLE = 0;
    private static final int ATTACK_STATE_PUNCH_COMBO = 1;
    private static final int ATTACK_STATE_GROUND_SLAM = 2;
    private static final int ATTACK_STATE_SEISMIC_CHARGE = 3;

    private static final int SPAWN_ANIMATION_TICKS = 26;
    private static final int PUNCH_COMBO_DURATION = 30;
    private static final int GROUND_SLAM_DURATION = 45;
    private static final int CHARGE_PHASE_PREPARE = 0;
    private static final int CHARGE_PHASE_RUN = 1;
    private static final int CHARGE_PHASE_IMPACT = 2;
    private static final int CHARGE_PHASE_MISS = 3;
    private static final int CHARGE_PHASE_WALL_STUN = 4;
    private static final int CHARGE_PHASE_WALL_RECOVER = 5;
    private static final int CHARGE_PREPARE_DURATION = 12;
    private static final int CHARGE_RUN_DURATION = 22;
    private static final int CHARGE_IMPACT_RECOVERY_DURATION = 14;
    private static final int CHARGE_MISS_RECOVERY_DURATION = 12;
    private static final int CHARGE_WALL_STUN_DURATION = 60;
    private static final int CHARGE_WALL_RECOVER_DURATION = 14;
    private static final double CHARGE_START_SPEED = 0.28D;
    private static final double CHARGE_MAX_SPEED = 0.72D;
    private static final double CHARGE_ACCELERATION = 0.035D;
    private static final int GROUND_SLAM_FIRST_HIT_TICK = 14;
    private static final int GROUND_SLAM_SECOND_HIT_TICK = 25;
    private static final int GROUND_SLAM_STRONG_HIT_TICK = 36;
    private static final float GROUND_SLAM_IMPACT_FORWARD_OFFSET = 2.0F;
    private static final float GROUND_SLAM_IMPACT_SIDE_OFFSET = 0.0F;
    private static final float GROUND_SLAM_LIGHT_SHAKE_RADIUS = 14.0F;
    private static final float GROUND_SLAM_LIGHT_SHAKE_MAGNITUDE = 0.25F;
    private static final int GROUND_SLAM_LIGHT_SHAKE_DURATION = 6;
    private static final float GROUND_SLAM_STRONG_SHAKE_RADIUS = 20.0F;
    private static final float GROUND_SLAM_STRONG_SHAKE_MAGNITUDE = 0.78F;
    private static final int GROUND_SLAM_STRONG_SHAKE_DURATION = 10;
    private static final int GROUND_SLAM_DEBRIS_WAVE_DURATION = 9;
    private static final float GROUND_SLAM_DEBRIS_WAVE_START_RADIUS = 1.8F;
    private static final float GROUND_SLAM_DEBRIS_WAVE_END_RADIUS = 5.8F;
    private static final int GROUND_SLAM_DEBRIS_WAVE_PARTICLES_PER_TICK = 9;
    private static final float GROUND_SLAM_DEBRIS_WAVE_OUTWARD_STRENGTH = 0.16F;

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.golem.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.golem.walk");
    private static final RawAnimation ATTACK_SWING_ANIMATION = RawAnimation.begin().thenPlay("animation.golem.attack.swing");
    private static final RawAnimation ATTACK_PUNCH_ANIMATION = RawAnimation.begin().thenPlay("animation.golem.attack.punch");
    private static final RawAnimation ATTACK_SLAM_ANIMATION = RawAnimation.begin().thenPlay("animation.golem.attack.slam");
    private static final RawAnimation ATTACK_CHARGE_PREPARE_ANIMATION = RawAnimation.begin().thenPlay("animation.golem.attack.charge.prepare");
    private static final RawAnimation ATTACK_CHARGE_RUN_ANIMATION = RawAnimation.begin().thenLoop("animation.golem.attack.charge.run");
    private static final RawAnimation ATTACK_CHARGE_IMPACT_ANIMATION = RawAnimation.begin().thenPlay("animation.golem.attack.charge.impact");
    private static final RawAnimation ATTACK_CHARGE_MISS_ANIMATION = RawAnimation.begin().thenPlay("animation.golem.attack.charge.miss");
    private static final RawAnimation ATTACK_CHARGE_WALL_STUN_ANIMATION = RawAnimation.begin().thenLoop("animation.golem.attack.charge.wall_stun");
    private static final RawAnimation ATTACK_CHARGE_WALL_RECOVER_ANIMATION = RawAnimation.begin().thenPlay("animation.golem.attack.charge.wall_recover");
    private static final RawAnimation SPAWN_ANIMATION = RawAnimation.begin().thenPlay("animation.golem.spawn");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlay("animation.golem.die");

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    private int attackTicks;
    private int comboCooldown;
    private int slamCooldown;
    private int chargeCooldown;
    private boolean chargeImpactTriggered;
    private Vec3 strongSlamImpactPosition;
    private Vec3 chargeDirection;
    private double chargeSpeed;
    private int chargePhaseTicks;
    private int armoredHitSoundCooldown;

    public GolemEntity(EntityType<? extends GolemEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 140.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACK_STATE, ATTACK_STATE_IDLE);
        builder.define(CHARGE_PHASE, CHARGE_PHASE_PREPARE);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if (ATTACK_STATE.equals(key)) {
            this.attackTicks = 0;
            this.chargeImpactTriggered = false;
            this.strongSlamImpactPosition = null;
        } else if (CHARGE_PHASE.equals(key)) {
            this.chargePhaseTicks = 0;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide()) {
            if (this.comboCooldown > 0) {
                this.comboCooldown--;
            }

            if (this.slamCooldown > 0) {
                this.slamCooldown--;
            }

            if (this.chargeCooldown > 0) {
                this.chargeCooldown--;
            }

            if (this.armoredHitSoundCooldown > 0) {
                this.armoredHitSoundCooldown--;
            }
        }

        if (this.getAttackState() != ATTACK_STATE_IDLE) {
            this.attackTicks++;

            if (!this.level().isClientSide()) {
                this.tickActiveAttack();
            }
        } else {
            this.attackTicks = 0;
            this.chargeImpactTriggered = false;
            this.strongSlamImpactPosition = null;
        }

    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new GolemPunchComboGoal(this));
        this.goalSelector.addGoal(3, new GolemGroundSlamGoal(this));
        this.goalSelector.addGoal(4, new GolemSeismicChargeGoal(this));
        this.goalSelector.addGoal(5, new GolemAdvanceGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GolemEntity>("Base", 0, state -> {
            if (state.getDataOrDefault(DataTickets.IS_DEAD_OR_DYING, false)) {
                state.setControllerSpeed(1.0F);
                return state.setAndContinue(DEATH_ANIMATION);
            }

            if (this.getAttackState() == ATTACK_STATE_PUNCH_COMBO) {
                state.setControllerSpeed(1.0F);
                return state.setAndContinue(ATTACK_PUNCH_ANIMATION);
            }

            if (this.getAttackState() == ATTACK_STATE_GROUND_SLAM) {
                state.setControllerSpeed(1.0F);
                return state.setAndContinue(ATTACK_SLAM_ANIMATION);
            }

            if (this.getAttackState() == ATTACK_STATE_SEISMIC_CHARGE) {
                state.setControllerSpeed(1.0F);
                return switch (this.getChargePhase()) {
                    case CHARGE_PHASE_RUN -> state.setAndContinue(ATTACK_CHARGE_RUN_ANIMATION);
                    case CHARGE_PHASE_IMPACT -> state.setAndContinue(ATTACK_CHARGE_IMPACT_ANIMATION);
                    case CHARGE_PHASE_MISS -> state.setAndContinue(ATTACK_CHARGE_MISS_ANIMATION);
                    case CHARGE_PHASE_WALL_STUN -> state.setAndContinue(ATTACK_CHARGE_WALL_STUN_ANIMATION);
                    case CHARGE_PHASE_WALL_RECOVER -> state.setAndContinue(ATTACK_CHARGE_WALL_RECOVER_ANIMATION);
                    default -> state.setAndContinue(ATTACK_CHARGE_PREPARE_ANIMATION);
                };
            }

            if (this.getAttackState() != ATTACK_STATE_IDLE) {
                state.setControllerSpeed(1.0F);
                return state.setAndContinue(ATTACK_SWING_ANIMATION);
            }

            if (state.renderState().getAnimatableAge() <= SPAWN_ANIMATION_TICKS) {
                state.setControllerSpeed(1.0F);
                return state.setAndContinue(SPAWN_ANIMATION);
            }

            if (state.isMoving()) {
                state.setControllerSpeed(0.9F);
                return state.setAndContinue(WALK_ANIMATION);
            }

            state.setControllerSpeed(1.0F);
            return state.setAndContinue(IDLE_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }

    private int getAttackState() {
        return this.entityData.get(ATTACK_STATE);
    }

    private boolean isIdle() {
        return this.getAttackState() == ATTACK_STATE_IDLE;
    }

    private int getChargePhase() {
        return this.entityData.get(CHARGE_PHASE);
    }

    public boolean isWallStunned() {
        return this.getAttackState() == ATTACK_STATE_SEISMIC_CHARGE
                && this.getChargePhase() == CHARGE_PHASE_WALL_STUN;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!this.isWallStunned() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (source.getEntity() != null && this.armoredHitSoundCooldown <= 0) {
                this.playSound(SoundEvents.ANVIL_LAND, 0.9F, 2.0F);
                this.armoredHitSoundCooldown = 4;
            }

            return false;
        }

        return super.hurtServer(level, source, amount);
    }

    private void setChargePhase(int phase) {
        this.chargePhaseTicks = 0;
        this.entityData.set(CHARGE_PHASE, phase);
    }

    private void startAttackState(int attackState) {
        this.attackTicks = 0;
        this.chargeImpactTriggered = false;
        this.strongSlamImpactPosition = null;
        this.chargeDirection = null;
        this.chargeSpeed = 0.0D;
        this.chargePhaseTicks = 0;
        this.entityData.set(ATTACK_STATE, attackState);
        this.getNavigation().stop();

        if (attackState == ATTACK_STATE_PUNCH_COMBO) {
            this.comboCooldown = 60;
        } else if (attackState == ATTACK_STATE_GROUND_SLAM) {
            this.slamCooldown = 72;
        } else if (attackState == ATTACK_STATE_SEISMIC_CHARGE) {
            this.chargeCooldown = 96;
            this.setChargePhase(CHARGE_PHASE_PREPARE);
        }
    }

    private void finishAttackState() {
        this.attackTicks = 0;
        this.chargeImpactTriggered = false;
        this.strongSlamImpactPosition = null;
        this.chargeDirection = null;
        this.chargeSpeed = 0.0D;
        this.chargePhaseTicks = 0;
        this.entityData.set(ATTACK_STATE, ATTACK_STATE_IDLE);
    }

    private void tickActiveAttack() {
        LivingEntity target = this.getTarget();

        if (target != null && target.isAlive()
                && (this.getAttackState() != ATTACK_STATE_SEISMIC_CHARGE || this.getChargePhase() == CHARGE_PHASE_PREPARE)) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.faceTargetHorizontally(target, 24.0F);
        }

        this.getNavigation().stop();

        switch (this.getAttackState()) {
            case ATTACK_STATE_PUNCH_COMBO -> this.tickPunchCombo();
            case ATTACK_STATE_GROUND_SLAM -> this.tickGroundSlam();
            case ATTACK_STATE_SEISMIC_CHARGE -> this.tickSeismicCharge();
            default -> this.finishAttackState();
        }
    }

    private void tickPunchCombo() {
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.55D, 1.0D, 0.55D));

        if (this.attackTicks == 4) {
            this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.15F, 0.9F + this.random.nextFloat() * 0.1F);
        }

        if (this.attackTicks == 7) {
            this.stepForward(0.2D);
        }

        if (this.attackTicks == 8) {
            this.swing(InteractionHand.MAIN_HAND);
            this.performFrontalAttack(3.5D, -0.25D, 0.7D, 0.16D);
            this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.9F);
        }

        if (this.attackTicks == 14) {
            this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.35F, 0.66F + this.random.nextFloat() * 0.08F);
            this.spawnConfiguredImpactRing(1.2F, 0.0F, 0.08D, 0.84F, 0.9F, 0.84F, 0.58F, 0.5F, 1.05F, 8,
                    RingBehavior.GROW_THEN_SHRINK, false, 0.0F, 0);
        }

        if (this.attackTicks == 18) {
            this.stepForward(0.34D);
        }

        if (this.attackTicks == 19) {
            this.swing(InteractionHand.MAIN_HAND);
            boolean hitTarget = this.performFrontalDirectionalAttack(4.25D, 0.2D, 1.75D, 0.32D);
            this.spawnConfiguredImpactRing(3.25F, 0.0F, 0.10D, 0.94F, 0.98F, 0.94F, 0.95F, 1.0F, 2.8F, 13,
                    RingBehavior.GROW, true, 1.05F, 26);
            this.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.25F, 0.68F);
            if (hitTarget) {
                this.chargeCooldown = Math.min(this.chargeCooldown, 24);
                if (this.level() instanceof ServerLevel serverLevel) {
                    CameraShakeDispatcher.shake(serverLevel, this.getOffsetGroundPosition(3.25F, 0.0F, 0.0D),
                            10.0F, 0.18F, 5);
                }
            }
        }

        if (this.attackTicks >= PUNCH_COMBO_DURATION) {
            this.finishAttackState();
        }
    }

    private void tickGroundSlam() {
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.5D, 1.0D, 0.5D));

        if (this.attackTicks == 6) {
            this.playSound(SoundEvents.IRON_GOLEM_DAMAGE, 1.0F, 0.62F);
        }

        if (this.attackTicks == GROUND_SLAM_FIRST_HIT_TICK) {
            this.performGroundSlamHit(GROUND_SLAM_IMPACT_FORWARD_OFFSET, GROUND_SLAM_IMPACT_SIDE_OFFSET, 2.25D, 0.82D, 0.28D,
                    0.78F, 0.90F, 0.78F, 0.88F, 0.85F, 1.85F, 11,
                    RingBehavior.GROW, true, 0.75F, 18, 0.9F, 0.72F);
            this.shakeGroundSlamImpact(GROUND_SLAM_LIGHT_SHAKE_RADIUS, GROUND_SLAM_LIGHT_SHAKE_MAGNITUDE,
                    GROUND_SLAM_LIGHT_SHAKE_DURATION);
        }

        if (this.attackTicks == GROUND_SLAM_SECOND_HIT_TICK) {
            this.performGroundSlamHit(GROUND_SLAM_IMPACT_FORWARD_OFFSET, GROUND_SLAM_IMPACT_SIDE_OFFSET, 2.75D, 0.95D, 0.32D,
                    0.84F, 0.95F, 0.84F, 0.94F, 0.95F, 2.45F, 13,
                    RingBehavior.GROW, true, 0.95F, 24, 1.05F, 0.68F);
            this.shakeGroundSlamImpact(GROUND_SLAM_LIGHT_SHAKE_RADIUS, GROUND_SLAM_LIGHT_SHAKE_MAGNITUDE,
                    GROUND_SLAM_LIGHT_SHAKE_DURATION);
        }

        if (this.attackTicks == GROUND_SLAM_STRONG_HIT_TICK) {
            this.performGroundSlamHit(GROUND_SLAM_IMPACT_FORWARD_OFFSET, GROUND_SLAM_IMPACT_SIDE_OFFSET, 3.35D, 1.12D, 0.38D,
                    0.92F, 1.0F, 0.92F, 1.0F, 1.08F, 3.1F, 16,
                    RingBehavior.GROW, false, 1.15F, 30, 1.18F, 0.62F);
            Vec3 impactPosition = this.getOffsetGroundPosition(
                    GROUND_SLAM_IMPACT_FORWARD_OFFSET, GROUND_SLAM_IMPACT_SIDE_OFFSET, 0.0D);
            this.strongSlamImpactPosition = impactPosition;
            ParticleHelper.spawnGroundDebrisBurst(this.level(), impactPosition.x, impactPosition.y, impactPosition.z,
                    1.65F, 48, 0.28F);
            this.shakeGroundSlamImpact(GROUND_SLAM_STRONG_SHAKE_RADIUS, GROUND_SLAM_STRONG_SHAKE_MAGNITUDE,
                    GROUND_SLAM_STRONG_SHAKE_DURATION);
            this.spawnConfiguredImpactRing(GROUND_SLAM_IMPACT_FORWARD_OFFSET, GROUND_SLAM_IMPACT_SIDE_OFFSET, 0.14D, 0.70F, 0.84F, 0.70F, 0.42F, 1.22F, 4.9F, 20,
                    RingBehavior.GROW_THEN_SHRINK, false, 0.0F, 0);
        }

        this.tickStrongSlamDebrisWave();

        if (this.attackTicks >= GROUND_SLAM_DURATION) {
            this.finishAttackState();
        }
    }

    private void tickSeismicCharge() {
        this.chargePhaseTicks++;

        switch (this.getChargePhase()) {
            case CHARGE_PHASE_RUN -> this.tickChargeRun();
            case CHARGE_PHASE_IMPACT -> this.tickChargeImpactRecovery();
            case CHARGE_PHASE_MISS -> this.tickChargeMissRecovery();
            case CHARGE_PHASE_WALL_STUN -> this.tickChargeWallStun();
            case CHARGE_PHASE_WALL_RECOVER -> this.tickChargeWallRecover();
            default -> this.tickChargePreparation();
        }
    }

    private void tickChargePreparation() {
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.35D, 1.0D, 0.35D));

        if (this.chargePhaseTicks == 4) {
            this.playSound(SoundEvents.RAVAGER_ROAR, 1.15F, 0.78F);
            this.spawnConfiguredImpactRing(0.0F, 0.0F, 0.08D, 0.82F, 0.9F, 0.82F, 0.72F, 0.72F, 1.65F, 10,
                    RingBehavior.GROW_THEN_SHRINK, true, 0.7F, 18);
        }

        if (this.chargePhaseTicks < CHARGE_PREPARE_DURATION) {
            return;
        }

        LivingEntity target = this.getTarget();
        Vec3 direction = target == null ? this.getHorizontalForward()
                : new Vec3(target.getX() - this.getX(), 0.0D, target.getZ() - this.getZ());
        if (direction == null || direction.lengthSqr() < 1.0E-4D || !this.onGround()) {
            this.startChargeMiss();
            return;
        }

        this.chargeDirection = direction.normalize();
        this.chargeSpeed = CHARGE_START_SPEED;
        this.setYRot((float) (Mth.atan2(this.chargeDirection.z, this.chargeDirection.x) * Mth.RAD_TO_DEG) - 90.0F);
        this.setYBodyRot(this.getYRot());
        this.setYHeadRot(this.getYRot());
        this.setChargePhase(CHARGE_PHASE_RUN);
        this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.1F, 0.65F);
    }

    private void tickChargeRun() {
        if (this.chargeDirection == null || !this.onGround()) {
            this.startChargeMiss();
            return;
        }

        this.chargeSpeed = Math.min(CHARGE_MAX_SPEED, this.chargeSpeed + CHARGE_ACCELERATION);
        Vec3 movement = this.chargeDirection.scale(this.chargeSpeed);
        this.setDeltaMovement(movement.x, this.getDeltaMovement().y, movement.z);
        this.setYRot((float) (Mth.atan2(this.chargeDirection.z, this.chargeDirection.x) * Mth.RAD_TO_DEG) - 90.0F);
        this.setYBodyRot(this.getYRot());
        this.setYHeadRot(this.getYRot());

        LivingEntity hitTarget = this.findChargeCollisionTarget();
        if (hitTarget != null) {
            this.triggerChargeImpact(hitTarget, false);
            return;
        }

        if (this.horizontalCollision) {
            this.startChargeWallStun();
            return;
        }

        if ((this.chargePhaseTicks & 1) == 0) {
            Vec3 trailPosition = this.position().subtract(this.chargeDirection.scale(0.65D));
            ParticleHelper.spawnGroundDebrisBurst(this.level(), trailPosition.x, this.getY(), trailPosition.z,
                    0.55F, 8, 0.12F);
            this.playSound(SoundEvents.IRON_GOLEM_STEP, 0.85F, 0.72F + this.random.nextFloat() * 0.12F);
        }

        if (this.chargePhaseTicks >= CHARGE_RUN_DURATION) {
            this.startChargeMiss();
        }
    }

    private LivingEntity findChargeCollisionTarget() {
        if (this.chargeDirection == null) {
            return null;
        }

        return this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(0.32D, 0.15D, 0.32D),
                target -> {
                    if (!this.isValidAttackTarget(target)) {
                        return false;
                    }

                    Vec3 offset = target.position().subtract(this.position());
                    Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
                    return horizontal.lengthSqr() < 1.0E-4D || this.chargeDirection.dot(horizontal.normalize()) >= 0.15D;
                }).stream()
                .min((first, second) -> Double.compare(
                        this.chargeDirection.dot(first.position().subtract(this.position())),
                        this.chargeDirection.dot(second.position().subtract(this.position()))))
                .orElse(null);
    }

    private void triggerChargeImpact(LivingEntity hitTarget, boolean hitWall) {
        this.chargeImpactTriggered = true;
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        Vec3 direction = this.chargeDirection == null ? Vec3.ZERO : this.chargeDirection;
        Vec3 impactPosition = hitTarget == null
                ? this.position().add(direction.scale(this.getBbWidth() * 0.65D))
                : hitTarget.position();

        if (hitTarget != null) {
            this.damageAndLaunchInDirection(hitTarget, direction, 1.65D, 0.38D);
        }

        this.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.35F, hitWall ? 0.62F : 0.78F);
        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), hitWall ? 1.2F : 0.95F, hitWall ? 0.72F : 0.92F);
        ParticleHelper.spawnGroundDebrisBurst(this.level(), impactPosition.x, this.getY(), impactPosition.z,
                hitWall ? 1.55F : 1.1F, hitWall ? 38 : 28, hitWall ? 0.34F : 0.26F);
        ParticleHelper.spawnImpactRing(this.level(), impactPosition.x, this.getY() + 0.14D, impactPosition.z,
                0.88F, 0.96F, 0.88F, 0.92F, hitWall ? 1.15F : 0.95F, hitWall ? 3.4F : 2.7F,
                hitWall ? 15 : 12, RingBehavior.GROW);

        if (this.level() instanceof ServerLevel serverLevel) {
            CameraShakeDispatcher.shake(serverLevel, impactPosition,
                    hitWall ? 20.0F : 16.0F, hitWall ? 0.68F : 0.48F, hitWall ? 11 : 8);
        }

        this.setChargePhase(CHARGE_PHASE_IMPACT);
    }

    private void startChargeWallStun() {
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        Vec3 direction = this.chargeDirection == null ? Vec3.ZERO : this.chargeDirection;
        Vec3 impactPosition = this.position().add(direction.scale(this.getBbWidth() * 0.65D));

        this.playSound(SoundEvents.ANVIL_LAND, 1.45F, 0.62F);
        this.playSound(SoundEvents.STONE_HIT, 1.6F, 0.55F);
        this.playSound(SoundEvents.IRON_GOLEM_DAMAGE, 1.0F, 0.68F);
        ParticleHelper.spawnGroundDebrisBurst(this.level(), impactPosition.x, this.getY(), impactPosition.z,
                1.65F, 42, 0.36F);
        ParticleHelper.spawnImpactRing(this.level(), impactPosition.x, this.getY() + 0.14D, impactPosition.z,
                0.88F, 0.96F, 0.88F, 0.95F, 1.2F, 3.6F, 16, RingBehavior.GROW);

        if (this.level() instanceof ServerLevel serverLevel) {
            CameraShakeDispatcher.shake(serverLevel, impactPosition, 22.0F, 0.75F, 12);
        }

        this.setChargePhase(CHARGE_PHASE_WALL_STUN);
    }

    private void tickChargeWallStun() {
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        this.lockRotationToChargeDirection();

        if (this.chargePhaseTicks == CHARGE_WALL_STUN_DURATION - 10) {
            this.playSound(SoundEvents.STONE_HIT, 0.75F, 0.68F);
        }

        if (this.chargePhaseTicks >= CHARGE_WALL_STUN_DURATION) {
            this.setChargePhase(CHARGE_PHASE_WALL_RECOVER);
            this.playSound(SoundEvents.METAL_HIT, 0.75F, 0.82F);
        }
    }

    private void tickChargeWallRecover() {
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.15D, 1.0D, 0.15D));
        this.lockRotationToChargeDirection();
        if (this.chargePhaseTicks >= CHARGE_WALL_RECOVER_DURATION) {
            this.finishAttackState();
        }
    }

    private void lockRotationToChargeDirection() {
        if (this.chargeDirection == null) {
            return;
        }

        float lockedYaw = (float) (Mth.atan2(this.chargeDirection.z, this.chargeDirection.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(lockedYaw);
        this.setYBodyRot(lockedYaw);
        this.setYHeadRot(lockedYaw);
    }

    private void faceTargetHorizontally(LivingEntity target, float maxTurn) {
        double offsetX = target.getX() - this.getX();
        double offsetZ = target.getZ() - this.getZ();
        if (offsetX * offsetX + offsetZ * offsetZ < 1.0E-4D) {
            return;
        }

        float targetYaw = (float) (Mth.atan2(offsetZ, offsetX) * Mth.RAD_TO_DEG) - 90.0F;
        float facingYaw = Mth.rotateIfNecessary(this.getYRot(), targetYaw, maxTurn);
        this.setYRot(facingYaw);
        this.setYBodyRot(facingYaw);
        this.setYHeadRot(Mth.rotateIfNecessary(this.getYHeadRot(), targetYaw, maxTurn));
    }

    private void tickChargeImpactRecovery() {
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.18D, 1.0D, 0.18D));
        if (this.chargePhaseTicks >= CHARGE_IMPACT_RECOVERY_DURATION) {
            this.finishAttackState();
        }
    }

    private void startChargeMiss() {
        this.setChargePhase(CHARGE_PHASE_MISS);
        this.playSound(SoundEvents.IRON_GOLEM_DAMAGE, 0.8F, 1.1F);
    }

    private void tickChargeMissRecovery() {
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.62D, 1.0D, 0.62D));
        if ((this.chargePhaseTicks & 1) == 0 && this.chargeDirection != null) {
            Vec3 skidPosition = this.position().subtract(this.chargeDirection.scale(0.7D));
            ParticleHelper.spawnGroundDebrisBurst(this.level(), skidPosition.x, this.getY(), skidPosition.z,
                    0.45F, 5, 0.09F);
        }

        if (this.chargePhaseTicks >= CHARGE_MISS_RECOVERY_DURATION) {
            this.finishAttackState();
        }
    }

    private void spawnConfiguredImpactRing(float forwardOffset, float sideOffset, double yOffset,
            float red, float green, float blue, float alpha, float scale, float radius, int duration,
            RingBehavior behavior, boolean debris, float debrisRadius, int debrisCount) {
        ParticleHelper.spawnGroundImpact(this.level(), this, forwardOffset, sideOffset, yOffset,
                red, green, blue, alpha, scale, radius, duration, false, behavior, debris, debrisRadius, debrisCount);
    }

    private void performGroundSlamHit(float forwardOffset, float sideOffset, double radius,
            double horizontalKnockback, double verticalKnockback,
            float red, float green, float blue, float alpha,
            float scale, float ringRadius, int duration,
            RingBehavior behavior, boolean debris, float debrisRadius, int debrisCount,
            float volume, float pitch) {
        this.performOffsetRadialAttack(forwardOffset, sideOffset, radius, horizontalKnockback, verticalKnockback);
        this.playSound(ModSounds.GROUND_SLAM.get(), volume, pitch);
        this.spawnConfiguredImpactRing(forwardOffset, sideOffset, 0.10D, red, green, blue, alpha, scale, ringRadius, duration,
                behavior, debris, debrisRadius, debrisCount);
    }

    private void shakeGroundSlamImpact(float radius, float magnitude, int durationTicks) {
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 impactPosition = this.getOffsetGroundPosition(
                    GROUND_SLAM_IMPACT_FORWARD_OFFSET, GROUND_SLAM_IMPACT_SIDE_OFFSET, 0.0D);
            CameraShakeDispatcher.shake(serverLevel, impactPosition, radius, magnitude, durationTicks);
        }
    }

    private void tickStrongSlamDebrisWave() {
        if (this.strongSlamImpactPosition == null
                || this.attackTicks < GROUND_SLAM_STRONG_HIT_TICK
                || this.attackTicks >= GROUND_SLAM_STRONG_HIT_TICK + GROUND_SLAM_DEBRIS_WAVE_DURATION) {
            return;
        }

        int waveTick = this.attackTicks - GROUND_SLAM_STRONG_HIT_TICK;
        float progress = waveTick / (float) (GROUND_SLAM_DEBRIS_WAVE_DURATION - 1);
        float waveRadius = Mth.lerp(progress, GROUND_SLAM_DEBRIS_WAVE_START_RADIUS, GROUND_SLAM_DEBRIS_WAVE_END_RADIUS);
        ParticleHelper.spawnExpandingGroundDebris(this.level(),
                this.strongSlamImpactPosition.x, this.strongSlamImpactPosition.y, this.strongSlamImpactPosition.z,
                waveRadius, GROUND_SLAM_DEBRIS_WAVE_PARTICLES_PER_TICK, GROUND_SLAM_DEBRIS_WAVE_OUTWARD_STRENGTH);
    }

    private void performFrontalAttack(double range, double dotThreshold, double horizontalKnockback, double verticalKnockback) {
        Vec3 look = this.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0D, look.z);

        if (forward.lengthSqr() < 1.0E-4D) {
            return;
        }

        forward = forward.normalize();

        for (LivingEntity candidate : this.getAttackCandidates(range)) {
            Vec3 offset = candidate.position().subtract(this.position());
            Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);

            if (horizontal.lengthSqr() < 1.0E-4D) {
                continue;
            }

            if (forward.dot(horizontal.normalize()) >= dotThreshold) {
                this.damageAndLaunch(candidate, horizontalKnockback, verticalKnockback);
            }
        }
    }

    private boolean performFrontalDirectionalAttack(double range, double dotThreshold,
            double horizontalKnockback, double verticalKnockback) {
        Vec3 forward = this.getHorizontalForward();
        if (forward == null) {
            return false;
        }

        boolean hitTarget = false;
        for (LivingEntity candidate : this.getAttackCandidates(range)) {
            Vec3 offset = candidate.position().subtract(this.position());
            Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
            if (horizontal.lengthSqr() > 1.0E-4D && forward.dot(horizontal.normalize()) >= dotThreshold) {
                this.damageAndLaunchInDirection(candidate, forward, horizontalKnockback, verticalKnockback);
                hitTarget = true;
            }
        }

        return hitTarget;
    }

    private void performRadialAttack(double range, double horizontalKnockback, double verticalKnockback) {
        for (LivingEntity candidate : this.getAttackCandidates(range)) {
            this.damageAndLaunch(candidate, horizontalKnockback, verticalKnockback);
        }
    }

    private void performOffsetRadialAttack(float forwardOffset, float sideOffset, double range,
            double horizontalKnockback, double verticalKnockback) {
        Vec3 impactCenter = this.getOffsetGroundPosition(forwardOffset, sideOffset, 0.0D);
        double rangeSqr = range * range;

        for (LivingEntity candidate : this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(range + Math.abs(forwardOffset) + 1.5D, 1.5D, range + Math.abs(forwardOffset) + 1.5D),
                target -> this.isValidAttackTarget(target))) {
            Vec3 candidateHorizontal = new Vec3(candidate.getX() - impactCenter.x, 0.0D, candidate.getZ() - impactCenter.z);
            if (candidateHorizontal.lengthSqr() <= rangeSqr) {
                this.damageAndLaunchFromPoint(candidate, impactCenter, horizontalKnockback, verticalKnockback);
            }
        }
    }

    private List<LivingEntity> getAttackCandidates(double range) {
        return this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(range, 1.5D, range), this::isValidAttackTarget);
    }

    private boolean isValidAttackTarget(LivingEntity candidate) {
        if (candidate == this || !candidate.isAlive()) {
            return false;
        }

        if (candidate.isSpectator() || candidate.isInvulnerable()) {
            return false;
        }

        if (this.isAlliedTo(candidate)) {
            return false;
        }

        return this.hasLineOfSight(candidate);
    }

    private void damageAndLaunch(LivingEntity target, double horizontalKnockback, double verticalKnockback) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!this.doHurtTarget(serverLevel, target)) {
            return;
        }

        Vec3 push = target.position().subtract(this.position());
        Vec3 horizontal = new Vec3(push.x, 0.0D, push.z);

        if (horizontal.lengthSqr() > 1.0E-4D) {
            Vec3 normalized = horizontal.normalize().scale(horizontalKnockback);
            target.push(normalized.x, verticalKnockback, normalized.z);
        } else {
            target.push(0.0D, verticalKnockback, 0.0D);
        }
    }

    private void damageAndLaunchFromPoint(LivingEntity target, Vec3 source, double horizontalKnockback, double verticalKnockback) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!this.doHurtTarget(serverLevel, target)) {
            return;
        }

        Vec3 push = target.position().subtract(source);
        Vec3 horizontal = new Vec3(push.x, 0.0D, push.z);

        if (horizontal.lengthSqr() > 1.0E-4D) {
            Vec3 normalized = horizontal.normalize().scale(horizontalKnockback);
            target.push(normalized.x, verticalKnockback, normalized.z);
        } else {
            target.push(0.0D, verticalKnockback, 0.0D);
        }
    }

    private void damageAndLaunchInDirection(LivingEntity target, Vec3 direction,
            double horizontalKnockback, double verticalKnockback) {
        if (!(this.level() instanceof ServerLevel serverLevel) || !this.doHurtTarget(serverLevel, target)) {
            return;
        }

        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            target.push(0.0D, verticalKnockback, 0.0D);
            return;
        }

        Vec3 push = horizontal.normalize().scale(horizontalKnockback);
        target.push(push.x, verticalKnockback, push.z);
    }

    private void stepForward(double strength) {
        Vec3 look = this.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);

        if (horizontal.lengthSqr() < 1.0E-4D) {
            return;
        }

        Vec3 normalized = horizontal.normalize().scale(strength);
        this.setDeltaMovement(normalized.x, this.getDeltaMovement().y, normalized.z);
    }

    private Vec3 getHorizontalForward() {
        Vec3 look = this.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0D, look.z);

        if (forward.lengthSqr() < 1.0E-4D) {
            return null;
        }

        return forward.normalize();
    }

    private Vec3 getOffsetGroundPosition(float forwardOffset, float sideOffset, double yOffset) {
        float bodyYaw = this.getYRot() * Mth.DEG_TO_RAD;
        float cos = Mth.cos(bodyYaw);
        float sin = Mth.sin(bodyYaw);
        double theta = bodyYaw + (Math.PI / 2.0D);
        double forwardX = Math.cos(theta);
        double forwardZ = Math.sin(theta);
        return new Vec3(
                this.getX() + forwardOffset * forwardX + cos * sideOffset,
                this.getY() + yOffset,
                this.getZ() + forwardOffset * forwardZ + sin * sideOffset);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.IRON_GOLEM_REPAIR;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource damageSource) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playAttackSound() {
        this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    private abstract static class GolemAttackGoal extends Goal {
        protected final GolemEntity golem;
        private final int attackState;
        private final double minDistanceSqr;
        private final double maxDistanceSqr;

        protected GolemAttackGoal(GolemEntity golem, int attackState, double minDistance, double maxDistance) {
            this.golem = golem;
            this.attackState = attackState;
            this.minDistanceSqr = minDistance * minDistance;
            this.maxDistanceSqr = maxDistance * maxDistance;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.golem.getTarget();

            if (target == null || !target.isAlive() || !this.golem.isIdle()) {
                return false;
            }

            if (!this.golem.hasLineOfSight(target)) {
                return false;
            }

            double distanceSqr = this.golem.distanceToSqr(target);
            return distanceSqr >= this.minDistanceSqr && distanceSqr <= this.maxDistanceSqr && this.canStart(target);
        }

        @Override
        public boolean canContinueToUse() {
            return this.golem.getAttackState() == this.attackState;
        }

        @Override
        public void start() {
            this.golem.startAttackState(this.attackState);
        }

        @Override
        public void tick() {
            LivingEntity target = this.golem.getTarget();

            if (target != null) {
                this.golem.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }

            this.golem.getNavigation().stop();
        }

        protected abstract boolean canStart(LivingEntity target);
    }

    private static final class GolemPunchComboGoal extends GolemAttackGoal {
        private GolemPunchComboGoal(GolemEntity golem) {
            super(golem, ATTACK_STATE_PUNCH_COMBO, 0.0D, 4.25D);
        }

        @Override
        protected boolean canStart(LivingEntity target) {
            double distanceSqr = this.golem.distanceToSqr(target);
            boolean inPreferredComboRange = distanceSqr <= Mth.square(3.75D);
            return this.golem.comboCooldown <= 0
                    && (inPreferredComboRange
                            || this.golem.slamCooldown > 16 && this.golem.random.nextFloat() < 0.55F);
        }
    }

    private static final class GolemGroundSlamGoal extends GolemAttackGoal {
        private GolemGroundSlamGoal(GolemEntity golem) {
            super(golem, ATTACK_STATE_GROUND_SLAM, 0.0D, 5.5D);
        }

        @Override
        protected boolean canStart(LivingEntity target) {
            boolean wounded = this.golem.getHealth() < this.golem.getMaxHealth() * 0.65F;
            return this.golem.slamCooldown <= 0 && (wounded || this.golem.random.nextFloat() < 0.35F);
        }
    }

    private static final class GolemSeismicChargeGoal extends GolemAttackGoal {
        private GolemSeismicChargeGoal(GolemEntity golem) {
            super(golem, ATTACK_STATE_SEISMIC_CHARGE, 4.0D, 12.0D);
        }

        @Override
        protected boolean canStart(LivingEntity target) {
            return this.golem.chargeCooldown <= 0 && this.golem.onGround();
        }
    }

    private static final class GolemAdvanceGoal extends Goal {
        private final GolemEntity golem;
        private final double speedModifier;
        private int pathRecalculationDelay;

        private GolemAdvanceGoal(GolemEntity golem, double speedModifier) {
            this.golem = golem;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.golem.getTarget();
            return target != null && target.isAlive() && this.golem.isIdle();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.golem.getTarget();
            return target != null && target.isAlive() && this.golem.isIdle();
        }

        @Override
        public void stop() {
            this.golem.getNavigation().stop();
            this.pathRecalculationDelay = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = this.golem.getTarget();

            if (target == null) {
                return;
            }

            this.golem.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (this.golem.distanceToSqr(target) <= Mth.square(3.5D)) {
                this.golem.getNavigation().stop();
                this.golem.faceTargetHorizontally(target, 20.0F);
                this.pathRecalculationDelay = 0;
                return;
            }

            if (--this.pathRecalculationDelay <= 0 || this.golem.getNavigation().isDone()) {
                this.pathRecalculationDelay = 8 + this.golem.getRandom().nextInt(5);
                this.golem.getNavigation().moveTo(target, this.speedModifier);
            }
        }
    }
}
