package com.somake.wotn.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.somake.wotn.network.CameraShakeDispatcher;
import com.somake.wotn.particle.ParticleHelper;
import com.somake.wotn.particle.ImpactRingParticleData.RingBehavior;
import com.somake.wotn.registry.ModEntities;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class FenrirIceOrbEntity extends ThrowableProjectile implements GeoEntity {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.fenrir_ice_orb.idle");
    private static final int MAX_LIFETIME_TICKS = 80;
    private static final float DIRECT_DAMAGE = 11.0F;
    private static final float SPLASH_DAMAGE = 7.0F;
    private static final double SPLASH_RADIUS = 3.25D;
    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private boolean impacted;

    public FenrirIceOrbEntity(EntityType<? extends FenrirIceOrbEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public FenrirIceOrbEntity(Level level, FenrirEntity owner, Vec3 position, Vec3 direction, float speed) {
        this(ModEntities.FENRIR_ICE_ORB.get(), level);
        this.setOwner(owner);
        this.setPos(position.x, position.y, position.z);
        this.shoot(direction.x, direction.y, direction.z, speed, 0.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide() && this.tickCount >= MAX_LIFETIME_TICKS) {
            this.discard();
            return;
        }
        if (this.level().isClientSide()) {
            Vec3 movement = this.getDeltaMovement();
            Vec3 tail = movement.lengthSqr() < 1.0E-6D ? Vec3.ZERO : movement.normalize().scale(-0.38D);
            double x = this.getX() + tail.x;
            double y = this.getY() + tail.y;
            double z = this.getZ() + tail.z;
            this.level().addParticle(ParticleTypes.SNOWFLAKE, x, y, z,
                    -movement.x * 0.025D, -movement.y * 0.025D, -movement.z * 0.025D);
            ParticleHelper.spawnSnowflake(this.level(), ParticleHelper.SNOWFLAKE_TRAIL, x, y, z,
                    -movement.x * 0.018D, -movement.y * 0.018D, -movement.z * 0.018D);
            if ((this.tickCount & 1) == 0) {
                this.level().addParticle(new DustColorTransitionOptions(0x2BCBE8, 0xEFFFFF, 1.0F),
                        x, y, z, 0.0D, 0.0D, 0.0D);
            }
        }
        super.tick();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level().isClientSide() || this.impacted) {
            return;
        }
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        if (target == owner || owner != null && owner.isAlliedTo(target)) {
            return;
        }
        if (target instanceof LivingEntity living) {
            living.hurt(this.damageSources().mobProjectile(this,
                    owner instanceof LivingEntity livingOwner ? livingOwner : null), DIRECT_DAMAGE);
            this.explode(result.getLocation(), living);
            return;
        }
        this.explode(result.getLocation(), null);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide() && !this.impacted) {
            this.explode(result.getLocation(), null);
        }
    }

    private void explode(Vec3 center, LivingEntity directTarget) {
        if (this.impacted || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.impacted = true;
        Entity owner = this.getOwner();
        Set<UUID> affected = new HashSet<>();
        if (directTarget != null) {
            affected.add(directTarget.getUUID());
        }

        AABB splashArea = AABB.ofSize(center, SPLASH_RADIUS * 2.0D, SPLASH_RADIUS * 2.0D, SPLASH_RADIUS * 2.0D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, splashArea,
                target -> target.isAlive() && target != owner && !target.isSpectator()
                        && target.getBoundingBox().distanceToSqr(center) <= SPLASH_RADIUS * SPLASH_RADIUS
                        && (owner == null || !owner.isAlliedTo(target)))) {
            if (affected.add(target.getUUID())) {
                target.hurt(this.damageSources().mobProjectile(this,
                        owner instanceof LivingEntity livingOwner ? livingOwner : null), SPLASH_DAMAGE);
            }
            Vec3 push = target.position().subtract(center);
            if (push.lengthSqr() > 1.0E-4D) {
                Vec3 knockback = push.normalize().scale(0.55D);
                target.push(knockback.x, 0.2D, knockback.z);
            }
        }

        ParticleHelper.spawnLayeredSnowflakes(level, ParticleHelper.SNOWFLAKE_BURST, 0.58F,
                center.x, center.y, center.z, 36, 1.25D, 1.25D, 1.25D, 0.12D);
        ParticleHelper.spawnImpactRing(level, center.x, center.y, center.z,
                0.4F, 0.9F, 1.0F, 0.95F, 1.0F, 3.6F, 14, RingBehavior.GROW_THEN_SHRINK);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GLASS_BREAK,
                SoundSource.HOSTILE, 1.25F, 0.68F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 0.65F, 1.45F);
        CameraShakeDispatcher.shake(level, center, 13.0F, 0.24F, 7);
        this.discard();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return target != this.getOwner() && (this.getOwner() == null || !this.getOwner().isAlliedTo(target))
                && super.canHitEntity(target);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0D;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<FenrirIceOrbEntity>("Base", 0,
                state -> state.setAndContinue(IDLE_ANIMATION)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }
}
