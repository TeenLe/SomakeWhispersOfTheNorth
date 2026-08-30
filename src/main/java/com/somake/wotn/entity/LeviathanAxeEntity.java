package com.somake.wotn.entity;

import com.somake.wotn.effect.FreezeManager;
import com.somake.wotn.effect.LeviathanAxeEffects;
import com.somake.wotn.skill.LeviathanImbueSkill;
import com.somake.wotn.skilltree.LeviathanMastery;
import com.somake.wotn.registry.ModEntities;
import com.somake.wotn.registry.ModItems;
import com.somake.wotn.particle.ParticleHelper;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class LeviathanAxeEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> RETURNING = SynchedEntityData.defineId(LeviathanAxeEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RETURN_TO_OFFHAND = SynchedEntityData.defineId(LeviathanAxeEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final int MAX_OUTBOUND_TICKS = 18;
    private static final int MAX_LIFETIME_TICKS = 80;
    private static final float DAMAGE = 10.0F;

    public LeviathanAxeEntity(EntityType<? extends LeviathanAxeEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public LeviathanAxeEntity(Level level, LivingEntity owner, ItemStack stack) {
        super(ModEntities.LEVIATHAN_AXE_PROJECTILE.get(), owner, level, stack);
        this.setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.LEVIATHAN_AXE.get();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RETURNING, false);
        builder.define(RETURN_TO_OFFHAND, false);
    }

    @Override
    public void tick() {
        Entity owner = this.getOwner();
        if (!this.level().isClientSide()) {
            if (owner == null || !owner.isAlive()) {
                this.dropCarriedAxe();
                this.discard();
                return;
            }

            if (this.tickCount >= MAX_LIFETIME_TICKS) {
                this.returnAxeToOwner(owner);
                this.playReturnEffects(owner);
                this.discard();
                return;
            }
        }

        if (!this.level().isClientSide() && !this.isReturning() && this.tickCount >= MAX_OUTBOUND_TICKS) {
            this.startReturning();
        }

        if (this.isReturning() && owner != null) {
            Vec3 target = owner.getEyePosition().add(0.0D, -0.35D, 0.0D);
            Vec3 toOwner = target.subtract(this.position());
            if (!this.level().isClientSide() && toOwner.lengthSqr() < 1.4D) {
                this.returnAxeToOwner(owner);
                this.playReturnEffects(owner);
                this.discard();
                return;
            }

            Vec3 desired = toOwner.normalize().scale(1.85D);
            this.setDeltaMovement(this.getDeltaMovement().scale(0.35D).add(desired.scale(0.65D)));
        }

        if (this.level().isClientSide()) {
            Vec3 movement = this.getDeltaMovement();
            Vec3 back = movement.lengthSqr() > 1.0E-6D ? movement.normalize().scale(this.isReturning() ? -0.2D : -0.38D) : Vec3.ZERO;
            double px = this.getX() + back.x;
            double py = this.getY() + back.y;
            double pz = this.getZ() + back.z;
            this.level().addParticle(ParticleTypes.SNOWFLAKE, px, py, pz,
                    -movement.x * 0.035D, -movement.y * 0.035D, -movement.z * 0.035D);
            if ((this.tickCount & 1) == 0) {
                ParticleHelper.spawnSnowflake(this.level(), ParticleHelper.SNOWFLAKE_TRAIL, px, py, pz,
                        -movement.x * 0.025D, -movement.y * 0.025D, -movement.z * 0.025D);
            }
            if ((this.tickCount & 1) == 0) {
                this.level().addParticle(new DustColorTransitionOptions(0x39DFFC, 0xE8FDFF, 0.7F),
                        px, py, pz, 0.0D, 0.0D, 0.0D);
            }
            if (this.isReturning() && this.getOwner() != null && this.tickCount % 2 == 0) {
                Vec3 ownerTarget = this.getOwner().getEyePosition().add(0.0D, -0.35D, 0.0D);
                Vec3 tether = ownerTarget.subtract(this.position());
                int samples = Math.min(6, Math.max(1, (int) (tether.length() / 2.0D)));
                for (int i = 1; i <= samples; i++) {
                    Vec3 point = this.position().add(tether.scale(i / (double) (samples + 1)));
                    this.level().addParticle(ParticleTypes.SNOWFLAKE, point.x, point.y, point.z,
                            tether.x * 0.002D, tether.y * 0.002D, tether.z * 0.002D);
                    if ((i & 1) == 0) {
                        ParticleHelper.spawnSnowflake(this.level(), ParticleHelper.SNOWFLAKE_TRAIL,
                                point.x, point.y, point.z,
                                tether.x * 0.0015D, tether.y * 0.0015D, tether.z * 0.0015D);
                    }
                }
            }
        }

        super.tick();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level().isClientSide() || this.isReturning()) {
            return;
        }

        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        if (target == owner) {
            return;
        }

        boolean imbued = LeviathanImbueSkill.isActive(this.getItem(), this.level());
        float damage = DAMAGE + (imbued ? LeviathanImbueSkill.BONUS_DAMAGE : 0.0F);
        float healthBefore = target instanceof LivingEntity livingTarget ? livingTarget.getHealth() : 0.0F;
        target.hurt(this.damageSources().thrown(this, owner), damage);
        if (owner instanceof net.minecraft.server.level.ServerPlayer player
                && target instanceof LivingEntity living) {
            if (living.getHealth() < healthBefore) {
                ItemStack carried = this.getItem().copy();
                LeviathanMastery.awardForHostileHit(player, carried, living, 2);
                this.setItem(carried);
            }
        }
        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            LeviathanAxeEffects.spawnImpact(serverLevel, result.getLocation(), this.getDeltaMovement(), false);
            if (imbued) {
                LeviathanAxeEffects.spawnImbuedHit(serverLevel, target, this.getDeltaMovement(), true);
            }
        }
        if (target instanceof LivingEntity living) {
            FreezeManager.freeze(living, owner instanceof LivingEntity livingOwner ? livingOwner : null);
        }
        Vec3 knockback = this.getDeltaMovement().normalize().scale(0.65D);
        target.push(knockback.x, 0.22D, knockback.z);
        this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 0.8F);
        this.startReturning();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.isReturning()) {
            super.onHitBlock(result);
            if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                LeviathanAxeEffects.spawnImpact(serverLevel, result.getLocation(), this.getDeltaMovement(), true);
            }
            if (!this.level().isClientSide() && !this.level().getBlockState(result.getBlockPos()).isAir()) {
                FrozenBlockEntity frozenBlock = this.level().getEntitiesOfClass(FrozenBlockEntity.class,
                        new net.minecraft.world.phys.AABB(result.getBlockPos()).inflate(0.1D)).stream()
                        .findFirst().orElse(null);
                if (frozenBlock == null) {
                    frozenBlock = FrozenBlockEntity.create(this.level(), result.getBlockPos());
                    this.level().addFreshEntity(frozenBlock);
                } else {
                    frozenBlock.refreshDuration();
                }
                this.level().playSound(null, frozenBlock.getX(), frozenBlock.getY(), frozenBlock.getZ(),
                        com.somake.wotn.registry.ModSounds.FREEZE.get(), SoundSource.BLOCKS,
                        FreezeManager.SOUND_VOLUME, 0.9F);
            }
            this.setPos(result.getLocation().add(result.getDirection().getUnitVec3().scale(0.08D)));
            if (!this.level().isClientSide()) {
                this.playSound(SoundEvents.AXE_STRIP, 0.9F, 0.7F);
            }
            this.startReturning();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return target != this.getOwner() && super.canHitEntity(target);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0D;
    }

    public boolean isReturning() {
        return this.entityData.get(RETURNING);
    }

    public void setReturnHand(InteractionHand hand) {
        this.entityData.set(RETURN_TO_OFFHAND, hand == InteractionHand.OFF_HAND);
    }

    private void returnAxeToOwner(Entity owner) {
        ItemStack returned = this.getItem().copy();
        if (returned.isEmpty()) {
            return;
        }

        if (owner instanceof Player player) {
            InteractionHand preferredHand = this.entityData.get(RETURN_TO_OFFHAND)
                    ? InteractionHand.OFF_HAND
                    : InteractionHand.MAIN_HAND;
            if (player.getItemInHand(preferredHand).isEmpty()) {
                player.setItemInHand(preferredHand, returned);
                return;
            }
            if (player.getInventory().add(returned)) {
                return;
            }
            player.drop(returned, false);
            return;
        }

        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            this.spawnAtLocation(serverLevel, returned);
        }
    }

    private void playReturnEffects(Entity owner) {
        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                    owner.getX(), owner.getEyeY() - 0.35D, owner.getZ(),
                    9, 0.22D, 0.18D, 0.22D, 0.035D);
            ParticleHelper.spawnSnowflakes(serverLevel, ParticleHelper.SNOWFLAKE_BURST,
                    owner.getX(), owner.getEyeY() - 0.35D, owner.getZ(),
                    3, 0.18D, 0.14D, 0.18D, 0.025D);
            this.playSound(SoundEvents.TRIDENT_RETURN, 0.55F, 1.25F);
            this.playSound(SoundEvents.ITEM_PICKUP, 0.7F, 0.8F);
        }
    }

    private void dropCarriedAxe() {
        ItemStack carried = this.getItem().copy();
        if (!carried.isEmpty() && this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            this.spawnAtLocation(serverLevel, carried);
        }
    }

    private void startReturning() {
        this.entityData.set(RETURNING, true);
        this.setNoGravity(true);
        this.noPhysics = true;
    }
}
