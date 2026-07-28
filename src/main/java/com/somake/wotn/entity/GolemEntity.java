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

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
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

    private static final int ATTACK_STATE_IDLE = 0;
    private static final int ATTACK_STATE_PUNCH_COMBO = 1;
    private static final int ATTACK_STATE_GROUND_SLAM = 2;
    private static final int ATTACK_STATE_SEISMIC_CHARGE = 3;

    private static final int SPAWN_ANIMATION_TICKS = 26;
    private static final int PUNCH_COMBO_DURATION = 27;
    private static final int GROUND_SLAM_DURATION = 45;
    private static final int SEISMIC_CHARGE_DURATION = 30;
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

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.golem.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.golem.walk");
    private static final RawAnimation ATTACK_SWING_ANIMATION = RawAnimation.begin().thenPlay("animation.golem.attack.swing");
    private static final RawAnimation ATTACK_PUNCH_ANIMATION = RawAnimation.begin().thenPlay("animation.golem.attack.punch");
    private static final RawAnimation ATTACK_SLAM_ANIMATION = RawAnimation.begin().thenPlay("animation.golem.attack.slam");
    private static final RawAnimation ATTACK_CHARGE_ANIMATION = RawAnimation.begin().thenPlay("animation.golem.attack.charge");
    private static final RawAnimation SPAWN_ANIMATION = RawAnimation.begin().thenPlay("animation.golem.spawn");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlay("animation.golem.die");

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    private int attackTicks;
    private int comboCooldown;
    private int slamCooldown;
    private int chargeCooldown;
    private boolean chargeImpactTriggered;

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
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if (ATTACK_STATE.equals(key)) {
            this.attackTicks = 0;
            this.chargeImpactTriggered = false;
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
        }

        if (this.getAttackState() != ATTACK_STATE_IDLE) {
            this.attackTicks++;

            if (!this.level().isClientSide()) {
                this.tickActiveAttack();
            }
        } else {
            this.attackTicks = 0;
            this.chargeImpactTriggered = false;
        }

    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new GolemGroundSlamGoal(this));
        this.goalSelector.addGoal(3, new GolemPunchComboGoal(this));
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
                return state.setAndContinue(ATTACK_CHARGE_ANIMATION);
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

    private void startAttackState(int attackState) {
        this.attackTicks = 0;
        this.chargeImpactTriggered = false;
        this.entityData.set(ATTACK_STATE, attackState);
        this.getNavigation().stop();

        if (attackState == ATTACK_STATE_PUNCH_COMBO) {
            this.comboCooldown = 26;
        } else if (attackState == ATTACK_STATE_GROUND_SLAM) {
            this.slamCooldown = 72;
        } else if (attackState == ATTACK_STATE_SEISMIC_CHARGE) {
            this.chargeCooldown = 96;
        }
    }

    private void finishAttackState() {
        this.attackTicks = 0;
        this.chargeImpactTriggered = false;
        this.entityData.set(ATTACK_STATE, ATTACK_STATE_IDLE);
    }

    private void tickActiveAttack() {
        LivingEntity target = this.getTarget();

        if (target != null && target.isAlive()) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
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
        if (this.attackTicks == 4 || this.attackTicks == 15) {
            this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.2F, 0.85F + this.random.nextFloat() * 0.15F);
            this.spawnConfiguredImpactRing(2.3F, 0.0F, 1.0D, 0.95F, 0.95F, 0.95F, 0.65F, 0.55F, 0.95F, 8,
                    RingBehavior.GROW_THEN_SHRINK, false, 0.0F, 0);
        }

        if (this.attackTicks == 7 || this.attackTicks == 18) {
            this.stepForward(0.25D);
        }

        if (this.attackTicks == 8) {
            this.swing(InteractionHand.MAIN_HAND);
            this.performFrontalAttack(3.4D, -0.15D, 0.75D, 0.18D);
            this.spawnConfiguredImpactRing(2.7F, 0.0F, 0.10D, 0.96F, 0.96F, 0.96F, 0.9F, 0.75F, 1.9F, 10,
                    RingBehavior.GROW, true, 0.7F, 16);
        }

        if (this.attackTicks == 19) {
            this.swing(InteractionHand.MAIN_HAND);
            this.performFrontalAttack(3.9D, -0.25D, 1.05D, 0.25D);
            this.spawnConfiguredImpactRing(3.2F, 0.0F, 0.10D, 0.98F, 0.98F, 0.98F, 0.95F, 0.85F, 2.35F, 12,
                    RingBehavior.GROW, true, 0.9F, 20);
            this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);
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
            ParticleHelper.spawnGroundDebrisBurst(this.level(), impactPosition.x, impactPosition.y, impactPosition.z,
                    1.65F, 48, 0.28F);
            this.shakeGroundSlamImpact(GROUND_SLAM_STRONG_SHAKE_RADIUS, GROUND_SLAM_STRONG_SHAKE_MAGNITUDE,
                    GROUND_SLAM_STRONG_SHAKE_DURATION);
            this.spawnConfiguredImpactRing(GROUND_SLAM_IMPACT_FORWARD_OFFSET, GROUND_SLAM_IMPACT_SIDE_OFFSET, 0.14D, 0.70F, 0.84F, 0.70F, 0.42F, 1.22F, 4.9F, 20,
                    RingBehavior.GROW_THEN_SHRINK, false, 0.0F, 0);
        }

        if (this.attackTicks >= GROUND_SLAM_DURATION) {
            this.finishAttackState();
        }
    }

    private void tickSeismicCharge() {
        if (this.attackTicks == 5) {
            this.playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 0.9F);
            this.spawnConfiguredImpactRing(3.0F, 0.0F, 1.0D, 0.95F, 0.95F, 0.95F, 0.65F, 0.6F, 1.25F, 10,
                    RingBehavior.GROW_THEN_SHRINK, false, 0.0F, 0);
        }

        if (this.attackTicks >= 9 && this.attackTicks <= 17) {
            this.stepForward(0.34D);

            if (!this.chargeImpactTriggered && this.horizontalCollision) {
                this.triggerChargeImpact();
            }
        }

        if (this.attackTicks == 18 && !this.chargeImpactTriggered) {
            this.triggerChargeImpact();
        }

        if (this.attackTicks >= SEISMIC_CHARGE_DURATION) {
            this.finishAttackState();
        }
    }

    private void triggerChargeImpact() {
        this.chargeImpactTriggered = true;
        this.performFrontalAttack(4.6D, -0.35D, 1.3D, 0.32D);
        this.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.8F);
        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 1.2F);
        this.spawnConfiguredImpactRing(3.8F, 0.0F, 0.12D, 0.95F, 0.95F, 0.95F, 0.95F, 0.95F, 2.7F, 12,
                RingBehavior.GROW, true, 1.0F, 26);
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
        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), volume, pitch);
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
            super(golem, ATTACK_STATE_PUNCH_COMBO, 0.0D, 3.9D);
        }

        @Override
        protected boolean canStart(LivingEntity target) {
            return this.golem.comboCooldown <= 0 && (this.golem.slamCooldown > 16 || this.golem.random.nextFloat() < 0.72F);
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
        }

        @Override
        public void tick() {
            LivingEntity target = this.golem.getTarget();

            if (target == null) {
                return;
            }

            this.golem.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.golem.getNavigation().moveTo(target, this.speedModifier);

            if (this.golem.distanceToSqr(target) <= Mth.square(3.5D)) {
                this.golem.getNavigation().stop();
            }
        }
    }
}
