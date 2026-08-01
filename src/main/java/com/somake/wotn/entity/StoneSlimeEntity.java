package com.somake.wotn.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
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

import java.util.EnumSet;

public class StoneSlimeEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(StoneSlimeEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int ATTACK_DURATION = 16;
    private static final int ATTACK_DAMAGE_TICK = 8;
    private static final int ATTACK_COOLDOWN_TICKS = 18;
    private static final double ATTACK_RANGE = 2.6D;
    private static final double ATTACK_RANGE_SQR = ATTACK_RANGE * ATTACK_RANGE;
    private static final float ATTACK_MAX_TURN = 30.0F;

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.stone_slime.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.stone_slime.walk");
    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("animation.stone_slime.attack");

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    private int attackTicks;
    private int attackCooldown;
    private boolean attackDamageApplied;

    public StoneSlimeEntity(EntityType<? extends StoneSlimeEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 3.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FOLLOW_RANGE, 20.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACKING, false);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        if (this.isAttacking()) {
            this.attackTicks++;

            if (!this.level().isClientSide()) {
                this.tickAttack();
            }
        } else {
            this.attackTicks = 0;
            this.attackDamageApplied = false;
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new StoneSlimeAttackGoal(this));
        this.goalSelector.addGoal(3, new StoneSlimeAdvanceGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<StoneSlimeEntity>("Base", 0, state -> {
            if (this.isAttacking()) {
                state.setControllerSpeed(1.0F);
                return state.setAndContinue(ATTACK_ANIMATION);
            }

            if (state.isMoving()) {
                state.setControllerSpeed(1.0F);
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

    public boolean isAttacking() {
        return this.entityData.get(ATTACKING);
    }

    private boolean isIdle() {
        return !this.isAttacking();
    }

    private void startAttack() {
        this.attackTicks = 0;
        this.attackDamageApplied = false;
        this.attackCooldown = ATTACK_COOLDOWN_TICKS;
        this.entityData.set(ATTACKING, true);
        this.getNavigation().stop();
    }

    private void finishAttack() {
        this.attackTicks = 0;
        this.attackDamageApplied = false;
        this.entityData.set(ATTACKING, false);
    }

    private void tickAttack() {
        LivingEntity target = this.getTarget();
        this.getNavigation().stop();
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.65D, 1.0D, 0.65D));

        if (target != null && target.isAlive()) {
            this.getLookControl().setLookAt(target, ATTACK_MAX_TURN, ATTACK_MAX_TURN);
            this.faceTargetHorizontally(target, ATTACK_MAX_TURN);
        }

        if (this.attackTicks == 5) {
            this.playSound(SoundEvents.MAGMA_CUBE_SQUISH, 0.8F, 0.75F + this.random.nextFloat() * 0.1F);
        }

        if (this.attackTicks == ATTACK_DAMAGE_TICK && !this.attackDamageApplied) {
            this.attackDamageApplied = true;
            this.performAttackHit(target);
        }

        if (this.attackTicks >= ATTACK_DURATION) {
            this.finishAttack();
        }
    }

    private void performAttackHit(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel) || target == null || !target.isAlive()) {
            return;
        }

        if (this.distanceToSqr(target) > ATTACK_RANGE_SQR || !this.hasLineOfSight(target)) {
            return;
        }

        Vec3 direction = target.position().subtract(this.position());
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);

        if (this.doHurtTarget(serverLevel, target)) {
            this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, 1.35F);
            if (horizontal.lengthSqr() > 1.0E-4D) {
                Vec3 push = horizontal.normalize().scale(0.4D);
                target.push(push.x, 0.18D, push.z);
            } else {
                target.push(0.0D, 0.18D, 0.0D);
            }
        }
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

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.TUFF_BREAK;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource damageSource) {
        return SoundEvents.STONE_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STONE_BREAK;
    }

    private static final class StoneSlimeAttackGoal extends Goal {
        private final StoneSlimeEntity slime;

        private StoneSlimeAttackGoal(StoneSlimeEntity slime) {
            this.slime = slime;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.slime.getTarget();
            return target != null
                    && target.isAlive()
                    && this.slime.isIdle()
                    && this.slime.attackCooldown <= 0
                    && this.slime.distanceToSqr(target) <= ATTACK_RANGE_SQR
                    && this.slime.hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return this.slime.isAttacking();
        }

        @Override
        public void start() {
            this.slime.startAttack();
        }

        @Override
        public void tick() {
            LivingEntity target = this.slime.getTarget();
            if (target != null) {
                this.slime.getLookControl().setLookAt(target, ATTACK_MAX_TURN, ATTACK_MAX_TURN);
            }
            this.slime.getNavigation().stop();
        }
    }

    private static final class StoneSlimeAdvanceGoal extends Goal {
        private final StoneSlimeEntity slime;
        private final double speedModifier;
        private int pathRecalculationDelay;

        private StoneSlimeAdvanceGoal(StoneSlimeEntity slime, double speedModifier) {
            this.slime = slime;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.slime.getTarget();
            return target != null && target.isAlive() && this.slime.isIdle();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.slime.getTarget();
            return target != null && target.isAlive() && this.slime.isIdle();
        }

        @Override
        public void stop() {
            this.slime.getNavigation().stop();
            this.pathRecalculationDelay = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = this.slime.getTarget();
            if (target == null) {
                return;
            }

            this.slime.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (this.slime.distanceToSqr(target) <= Mth.square(ATTACK_RANGE - 0.15D)) {
                this.slime.getNavigation().stop();
                this.slime.faceTargetHorizontally(target, 20.0F);
                this.pathRecalculationDelay = 0;
                return;
            }

            if (--this.pathRecalculationDelay <= 0 || this.slime.getNavigation().isDone()) {
                this.pathRecalculationDelay = 6 + this.slime.getRandom().nextInt(4);
                this.slime.getNavigation().moveTo(target, this.speedModifier);
            }
        }
    }
}
