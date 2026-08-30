package com.somake.wotn.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.somake.wotn.skilltree.LeviathanMastery;

import com.somake.wotn.registry.ModEntities;
import com.somake.wotn.particle.ParticleHelper;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;

public class IceSpikeEntity extends Entity implements GeoEntity {
    public static final int EMERGE_TICKS = 6;
    public static final int HOLD_TICKS = 18;
    public static final int SHATTER_TICKS = 6;
    private static final RawAnimation LIFECYCLE_ANIMATION = RawAnimation.begin()
            .thenPlay("animation.ice_spike.lifecycle");
    private static final EntityDataAccessor<Integer> EMERGENCE_DELAY = SynchedEntityData.defineId(
            IceSpikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VARIANT_SEED = SynchedEntityData.defineId(
            IceSpikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> VISUAL_SCALE = SynchedEntityData.defineId(
            IceSpikeEntity.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private ServerPlayer damageOwner;
    private ItemStack damageWeapon = ItemStack.EMPTY;
    private List<AABB> damageAreas = List.of();
    private Vec3 knockbackDirection = Vec3.ZERO;
    private int damageDelay = -1;
    private boolean damageApplied;

    public IceSpikeEntity(EntityType<? extends IceSpikeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static IceSpikeEntity create(Level level, double x, double y, double z, float yaw,
            int delay, int variantSeed, float scale) {
        IceSpikeEntity spike = new IceSpikeEntity(ModEntities.ICE_SPIKE.get(), level);
        spike.setPos(x, y, z);
        spike.setYRot(yaw);
        spike.entityData.set(EMERGENCE_DELAY, Math.max(0, delay));
        spike.entityData.set(VARIANT_SEED, variantSeed);
        spike.entityData.set(VISUAL_SCALE, scale);
        return spike;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(EMERGENCE_DELAY, 0);
        builder.define(VARIANT_SEED, 0);
        builder.define(VISUAL_SCALE, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        int visualAge = this.tickCount - this.getEmergenceDelay();
        if (this.level().isClientSide() && visualAge >= 0) {
            if (visualAge == 0) {
                BlockParticleOption ice = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState());
                for (int i = 0; i < 5; i++) {
                    this.level().addParticle(ice, this.getX(), this.getY() + 0.08D, this.getZ(),
                            this.random.nextGaussian() * 0.12D,
                            0.18D + this.random.nextDouble() * 0.22D,
                            this.random.nextGaussian() * 0.12D);
                }
            }
            if (visualAge <= EMERGE_TICKS && (visualAge & 1) == 0) {
                double particleX = this.getX() + this.random.nextGaussian() * 0.28D;
                double particleY = this.getY() + this.random.nextDouble() * 1.6D;
                double particleZ = this.getZ() + this.random.nextGaussian() * 0.28D;
                this.level().addParticle(ParticleTypes.SNOWFLAKE,
                        particleX, particleY, particleZ,
                        0.0D, 0.025D, 0.0D);
                if (visualAge % 4 == 0) {
                    ParticleHelper.spawnSnowflake(this.level(), ParticleHelper.SNOWFLAKE_AURA,
                            particleX, particleY, particleZ, 0.0D, 0.018D, 0.0D);
                }
            }
        }
        if (!this.level().isClientSide() && visualAge >= EMERGE_TICKS + HOLD_TICKS + SHATTER_TICKS) {
            this.discard();
        }
        if (!this.level().isClientSide() && !this.damageApplied && this.damageDelay >= 0
                && this.tickCount >= this.damageDelay) {
            this.applyCastDamage();
        }
    }

    public int getEmergenceDelay() {
        return this.entityData.get(EMERGENCE_DELAY);
    }

    public int getVariantSeed() {
        return this.entityData.get(VARIANT_SEED);
    }

    public float getVisualScale() {
        return this.entityData.get(VISUAL_SCALE);
    }

    public void configureCastDamage(ServerPlayer owner, ItemStack weapon, List<AABB> areas, Vec3 direction, int delay) {
        this.damageOwner = owner;
        this.damageWeapon = weapon;
        this.damageAreas = List.copyOf(areas);
        this.knockbackDirection = direction;
        this.damageDelay = Math.max(0, delay);
    }

    public void applyCastDamageNow() {
        if (!this.damageApplied) {
            this.applyCastDamage();
        }
    }

    private void applyCastDamage() {
        this.damageApplied = true;
        if (!(this.level() instanceof ServerLevel serverLevel) || this.damageOwner == null
                || !this.damageOwner.isAlive() || this.damageAreas.isEmpty()) return;
        AABB queryArea = this.damageAreas.stream().reduce(AABB::minmax).orElse(this.getBoundingBox());
        Set<UUID> hitTargets = new HashSet<>();
        int rewardedTargets = 0;
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, queryArea,
                target -> target != this.damageOwner && target.isAlive() && !target.isSpectator()
                        && !target.isInvulnerable() && !this.damageOwner.isAlliedTo(target)
                        && this.damageOwner.canAttack(target))) {
            boolean intersects = this.damageAreas.stream().anyMatch(area -> area.intersects(target.getBoundingBox()));
            if (intersects && hitTargets.add(target.getUUID())) {
                float healthBefore = target.getHealth();
                target.hurt(this.damageSources().thrown(this, this.damageOwner), 8.0F);
                if (target.getHealth() < healthBefore && rewardedTargets < 3
                        && target instanceof net.minecraft.world.entity.monster.Monster) {
                    LeviathanMastery.awardForHostileHit(this.damageOwner, this.damageWeapon, target, 2);
                    rewardedTargets++;
                }
                target.push(this.knockbackDirection.x * 0.45D, 0.18D, this.knockbackDirection.z * 0.45D);
            }
        }
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
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<IceSpikeEntity>("Base", 0, state -> {
            double visualAgeTicks = state.renderState().getAnimatableAge() - this.getEmergenceDelay();
            if (visualAgeTicks < 0.0D) {
                return PlayState.STOP;
            }

            state.setControllerSpeed(1.0F);
            return state.setAndContinue(LIFECYCLE_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }
}
