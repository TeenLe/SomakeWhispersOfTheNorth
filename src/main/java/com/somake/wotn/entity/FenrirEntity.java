package com.somake.wotn.entity;

import java.util.EnumSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.constant.DataTickets;
import com.geckolib.util.GeckoLibUtil;
import com.somake.wotn.network.CameraShakeDispatcher;
import com.somake.wotn.effect.FreezeManager;
import com.somake.wotn.particle.ParticleHelper;
import com.somake.wotn.particle.ImpactRingParticleData.RingBehavior;
import com.somake.wotn.registry.ModEntities;
import com.somake.wotn.registry.ModSounds;
import com.somake.wotn.block.entity.FenrirSealBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

public class FenrirEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(FenrirEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK_STATE = SynchedEntityData.defineId(
            FenrirEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PHASE_TWO = SynchedEntityData.defineId(
            FenrirEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ICE_ORB_RETREAT_ANIMATION = SynchedEntityData.defineId(
            FenrirEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> GLACIAL_SLAM_PHASE = SynchedEntityData.defineId(
            FenrirEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> GLACIAL_SLAM_PHASE_START_TICK = SynchedEntityData.defineId(
            FenrirEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> MOUTH_DEBUG = SynchedEntityData.defineId(
            FenrirEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> MOUTH_DEBUG_FORWARD = SynchedEntityData.defineId(
            FenrirEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MOUTH_DEBUG_UP = SynchedEntityData.defineId(
            FenrirEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MOUTH_DEBUG_SIDE = SynchedEntityData.defineId(
            FenrirEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> ENCOUNTER_STATE = SynchedEntityData.defineId(
            FenrirEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAKE_TICKS = SynchedEntityData.defineId(
            FenrirEntity.class, EntityDataSerializers.INT);

    public static final int PHASE_SURFACE = 0;
    public static final int PHASE_BURROWING = 1;
    public static final int PHASE_UNDERGROUND_ACCELERATING = 2;
    public static final int PHASE_UNDERGROUND_HUNTING = 3;
    public static final int PHASE_UNDERGROUND_FINAL_CHARGE = 4;
    public static final int PHASE_EMERGING = 5;
    public static final int PHASE_RECOVERY = 6;
    public static final int PHASE_EMERGENCE_ANTICIPATION = 7;
    public static final int ENCOUNTER_DORMANT = 0;
    public static final int ENCOUNTER_WAKING = 1;
    public static final int ENCOUNTER_ACTIVE = 2;
    public static final int ENCOUNTER_DEFEATED = 3;
    public static final int WAKE_DURATION = 80;

    private static final int ATTACK_IDLE = 0;
    private static final int ATTACK_BITE = 1;
    private static final int ATTACK_CLAWS = 2;
    private static final int ATTACK_ICE_ORB = 3;
    private static final int ATTACK_FROST_HOWL = 4;
    private static final int ATTACK_PHASE_TRANSITION = 5;
    private static final int ATTACK_GLACIAL_SLAM = 6;

    private static final int GLACIAL_SLAM_PREPARE = 0;
    private static final int GLACIAL_SLAM_LEAP = 1;
    private static final int GLACIAL_SLAM_IMPACT = 2;
    private static final int GLACIAL_SLAM_RECOVERY = 3;

    private static final int BITE_DURATION = 22;
    private static final int BITE_HIT_TICK = 8;
    private static final int CLAWS_DURATION = 24;
    private static final int CLAWS_HIT_TICK = 10;
    private static final int ICE_ORB_DURATION = 34;
    private static final int ICE_ORB_AIM_LOCK_TICK = 18;
    private static final int ICE_ORB_FIRE_TICK = 23;
    private static final int FROST_HOWL_DURATION = 48;
    private static final int FROST_HOWL_SPIKE_TICK = 14;
    private static final int PHASE_TRANSITION_DURATION = 55;
    private static final int GLACIAL_SLAM_PREPARE_DURATION = 8;
    private static final int GLACIAL_SLAM_LEAP_TIMEOUT = 30;
    private static final int GLACIAL_SLAM_IMPACT_TICK = 18;
    private static final int GLACIAL_SLAM_IMPACT_DURATION = 27;
    private static final int GLACIAL_SLAM_RECOVERY_DURATION = 14;
    private static final int GLACIAL_SLAM_RECOVERY_TIMEOUT = 40;
    private static final int GLACIAL_SLAM_COOLDOWN = 180;
    private static final int PHASE_TWO_GLACIAL_SLAM_COOLDOWN = 130;
    private static final int COMBAT_CYCLE_SIZE = 15;
    private static final float GLACIAL_SLAM_SPIKE_DAMAGE = 12.0F;
    private static final double GLACIAL_SLAM_CENTRAL_RADIUS = 3.0D;
    private static final double[][] GLACIAL_SLAM_ROWS = {
            {0.0D},
            {-0.75D, 0.75D},
            {-1.35D, 0.0D, 1.35D},
            {-2.0D, -0.65D, 0.65D, 2.0D},
            {-1.4D, 0.0D, 1.4D}
    };
    private static final int HOWL_DAMAGE_DELAY = IceSpikeEntity.EMERGE_TICKS;
    private static final int GLOBAL_SKILL_COOLDOWN = 18;
    private static final int PHASE_TWO_GLOBAL_SKILL_COOLDOWN = 14;
    private static final int ICE_ORB_COOLDOWN = 80;
    private static final int FROST_HOWL_COOLDOWN = 100;
    private static final double PHASE_TWO_HEALTH_RATIO = 0.6D;
    private static final double SURFACE_HOLD_DISTANCE = 4.25D;
    public static final float DEFAULT_MOUTH_FORWARD = 6.0F;
    public static final float DEFAULT_MOUTH_UP = 2.5F;
    public static final float DEFAULT_MOUTH_SIDE = 0.0F;

    private static final int BURROW_DURATION = 20;
    private static final int ACCELERATION_DURATION = 18;
    private static final int MAX_UNDERGROUND_DURATION = 220;
    private static final int PHASE_TWO_MAX_UNDERGROUND_DURATION = 150;
    private static final int FINAL_CHARGE_MIN_DURATION = 14;
    private static final int FINAL_CHARGE_MAX_DURATION = 96;
    private static final int REQUIRED_PASSES = 2;
    private static final int PASS_COOLDOWN_TICKS = 12;
    private static final int EMERGE_IMPACT_TICK = 10;
    private static final int EMERGE_DURATION = 24;
    private static final int RECOVERY_DURATION = 26;
    private static final int EMERGENCE_ANTICIPATION_DURATION = 12;
    private static final int BURROW_COOLDOWN = 200;
    private static final int PHASE_TWO_BURROW_COOLDOWN = 150;
    private static final double BURROW_START_SPEED = 0.12D;
    private static final double BURROW_HUNT_SPEED = 0.44D;
    private static final double BURROW_FINAL_SPEED = 0.58D;
    private static final double BURROW_ACCELERATION = 0.018D;
    private static final double PASS_RADIUS = 3.4D;
    private static final double PASS_ARM_DISTANCE = 7.0D;
    private static final double BURROW_CONTACT_RADIUS = 1.9D;
    private static final int BURROW_CONTACT_COOLDOWN_TICKS = 14;
    private static final float BURROW_CONTACT_DAMAGE = 10.0F;
    private static final float FINAL_CHARGE_CONTACT_DAMAGE = 13.0F;
    private static final int UNDERGROUND_HEAL_INTERVAL = 10;
    private static final float UNDERGROUND_HEAL_AMOUNT = 1.0F;
    private static final float UNDERGROUND_MAX_HEAL_RATIO = 0.06F;
    private static final double EMERGE_DAMAGE_RADIUS = 3.75D;

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.fenrir.idle");
    private static final RawAnimation SLEEP_ANIMATION = RawAnimation.begin().thenLoop("animation.fenrir.sleep");
    private static final RawAnimation WAKE_ANIMATION = RawAnimation.begin().thenPlay("animation.fenrir.wake");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.fenrir.walk");
    private static final RawAnimation BURROW_ANIMATION = RawAnimation.begin().thenPlay("animation.fenrir.burrow");
    private static final RawAnimation EMERGE_ANIMATION = RawAnimation.begin().thenPlay("animation.fenrir.emerge");
    private static final RawAnimation RECOVERY_ANIMATION = RawAnimation.begin().thenPlay("animation.fenrir.recovery");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlay("animation.fenrir.die");
    private static final RawAnimation EMERGE_ANTICIPATION_ANIMATION = RawAnimation.begin().thenLoop("animation.fenrir.emerge_anticipation");
    private static final RawAnimation BITE_ANIMATION = RawAnimation.begin().thenPlay("animation.fenrir.bite");
    private static final RawAnimation CLAWS_ANIMATION = RawAnimation.begin().thenPlay("animation.fenrir.claws");
    private static final RawAnimation ICE_ORB_ANIMATION = RawAnimation.begin().thenPlay("animation.fenrir.ice_orb");
    private static final RawAnimation ICE_ORB_RETREAT_ANIMATION_SEQUENCE = RawAnimation.begin()
            .thenPlay("animation.fenrir.ice_orb_retreat");
    private static final RawAnimation FROST_HOWL_ANIMATION = RawAnimation.begin().thenPlay("animation.fenrir.frost_howl");
    private static final RawAnimation PHASE_TRANSITION_ANIMATION = RawAnimation.begin().thenPlay("animation.fenrir.phase_transition");
    private static final RawAnimation GLACIAL_LEAP_PREPARE_ANIMATION = RawAnimation.begin().thenPlay("animation.fenrir.glacial_leap_prepare");
    private static final RawAnimation GLACIAL_LEAP_ANIMATION = RawAnimation.begin().thenLoop("animation.fenrir.glacial_leap");
    private static final RawAnimation GLACIAL_SLAM_ANIMATION = RawAnimation.begin().thenPlay("animation.fenrir.glacial_slam");
    private static final RawAnimation GLACIAL_SLAM_RECOVERY_ANIMATION = RawAnimation.begin().thenPlay("animation.fenrir.glacial_slam_recovery");
    private static final RawAnimation MOUTH_DEBUG_ANIMATION = RawAnimation.begin().thenLoop("animation.fenrir.mouth_debug");

    private final FenrirPart chestPart;
    private final FenrirPart headPart;
    private final FenrirPart muzzlePart;
    private final FenrirPart hindquartersPart;
    private final FenrirPart tailPart;
    private final FenrirPart tailTipPart;
    private final FenrirPart[] multipartHitboxes;

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(UUID.randomUUID(),
            Component.translatable("boss.wotn.fenrir"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);

    private int phaseTicks;
    private int burrowCooldown = 45;
    private boolean emergeImpactApplied;
    private UUID groundWaveUuid;
    private BlockPos encounterSealPos;
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
    private int attackTicks;
    private int meleeCooldown;
    private int iceOrbCooldown = 20;
    private int frostHowlCooldown = 45;
    private int glacialSlamCooldown;
    private int globalSkillCooldown;
    private FenrirAction lastCombatAction;
    private final ArrayList<FenrirAction> combatActionDeck = new ArrayList<>();
    private Vec3 iceOrbRetreatLanding;
    private boolean iceOrbRetreatBecameAirborne;
    private Vec3 lockedAttackDirection = Vec3.ZERO;
    private double howlSafeAngle;
    private int glacialSlamPhaseTicks;
    private boolean glacialSlamBecameAirborne;
    private boolean glacialSlamImpactApplied;
    private UUID glacialSlamTargetUuid;
    private Vec3 glacialSlamRetreatDirection = Vec3.ZERO;
    private Vec3 glacialSlamTargetPosition = Vec3.ZERO;
    private Vec3 glacialSlamDirection = Vec3.ZERO;
    private List<GlacialSlamPlacement> glacialSlamPlacements = List.of();
    private boolean glacialSlamAnimationSeekPending;
    private final ArrayList<PendingHowlHit> pendingHowlHits = new ArrayList<>();
    private final ArrayList<PendingGlacialSlamCast> pendingGlacialSlamCasts = new ArrayList<>();
    private final Map<UUID, Integer> burrowContactCooldowns = new HashMap<>();
    private float undergroundHealingReceived;

    public FenrirEntity(EntityType<? extends FenrirEntity> entityType, Level level) {
        super(entityType, level);
        this.chestPart = new FenrirPart(this, "chest", 2.3F, 3.1F);
        this.headPart = new FenrirPart(this, "head", 1.8F, 3.2F);
        this.muzzlePart = new FenrirPart(this, "muzzle", 1.1F, 1.9F);
        this.hindquartersPart = new FenrirPart(this, "hindquarters", 2.3F, 3.1F);
        this.tailPart = new FenrirPart(this, "tail", 1.25F, 2.45F);
        this.tailTipPart = new FenrirPart(this, "tail_tip", 1.0F, 1.9F);
        this.multipartHitboxes = new FenrirPart[] {
                this.chestPart,
                this.headPart,
                this.muzzlePart,
                this.hindquartersPart,
                this.tailPart,
                this.tailTipPart
        };
        this.setId(ENTITY_COUNTER.getAndAdd(this.multipartHitboxes.length + 1) + 1);
        this.updateMultipartHitboxes();
        this.xpReward = 100;
        this.bossEvent.setDarkenScreen(true);
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (this.multipartHitboxes == null) return;
        for (int index = 0; index < this.multipartHitboxes.length; index++) {
            this.multipartHitboxes[index].setId(id + index + 1);
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return this.multipartHitboxes;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        this.updateMultipartHitboxes();
    }

    private void updateMultipartHitboxes() {
        if (this.multipartHitboxes == null) return;
        if (!this.areMultipartHitboxesActive()) {
            for (FenrirPart part : this.multipartHitboxes) this.collapsePart(part);
            return;
        }

        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        this.movePart(this.chestPart, forward, 1.15D, 1.15D);
        this.movePart(this.headPart, forward, 3.05D, 1.1D);
        this.movePart(this.muzzlePart, forward, 4.35D, 1.1D);
        this.movePart(this.hindquartersPart, forward, -1.15D, 1.15D);
        this.movePart(this.tailPart, forward, -2.85D, 0.7D);
        this.movePart(this.tailTipPart, forward, -4.1D, 0.65D);
    }

    private boolean areMultipartHitboxesActive() {
        return this.isAlive() && !this.isRemoved()
                && this.getPhase() != PHASE_BURROWING && !this.isUnderground();
    }

    private void movePart(FenrirPart part, Vec3 forward, double forwardOffset, double yOffset) {
        double oldX = part.getX();
        double oldY = part.getY();
        double oldZ = part.getZ();
        Vec3 position = this.position().add(forward.scale(forwardOffset)).add(0.0D, yOffset, 0.0D);
        part.setPos(position.x, position.y, position.z);
        part.setYRot(this.yBodyRot);
        part.xo = oldX;
        part.yo = oldY;
        part.zo = oldZ;
        part.xOld = oldX;
        part.yOld = oldY;
        part.zOld = oldZ;
    }

    private void collapsePart(FenrirPart part) {
        double oldX = part.getX();
        double oldY = part.getY();
        double oldZ = part.getZ();
        part.setPos(this.getX(), this.getY(), this.getZ());
        part.setBoundingBox(new AABB(this.getX(), this.getY(), this.getZ(), this.getX(), this.getY(), this.getZ()));
        part.xo = oldX;
        part.yo = oldY;
        part.zo = oldZ;
        part.xOld = oldX;
        part.yOld = oldY;
        part.zOld = oldZ;
    }

    private boolean hurtFromPart(ServerLevel level, DamageSource source, float amount) {
        return this.hurtServer(level, source, amount);
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
        builder.define(ATTACK_STATE, ATTACK_IDLE);
        builder.define(PHASE_TWO, false);
        builder.define(ICE_ORB_RETREAT_ANIMATION, false);
        builder.define(GLACIAL_SLAM_PHASE, GLACIAL_SLAM_PREPARE);
        builder.define(GLACIAL_SLAM_PHASE_START_TICK, 0L);
        builder.define(MOUTH_DEBUG, false);
        builder.define(MOUTH_DEBUG_FORWARD, DEFAULT_MOUTH_FORWARD);
        builder.define(MOUTH_DEBUG_UP, DEFAULT_MOUTH_UP);
        builder.define(MOUTH_DEBUG_SIDE, DEFAULT_MOUTH_SIDE);
        builder.define(ENCOUNTER_STATE, ENCOUNTER_ACTIVE);
        builder.define(WAKE_TICKS, 0);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(PHASE, PHASE_SURFACE);
        this.entityData.set(ATTACK_STATE, ATTACK_IDLE);
        this.entityData.set(PHASE_TWO, input.getBooleanOr("FenrirPhaseTwo", false));
        this.entityData.set(ICE_ORB_RETREAT_ANIMATION, false);
        this.entityData.set(GLACIAL_SLAM_PHASE, GLACIAL_SLAM_PREPARE);
        this.entityData.set(GLACIAL_SLAM_PHASE_START_TICK, this.level().getGameTime());
        this.entityData.set(ENCOUNTER_STATE,
                Mth.clamp(input.getIntOr("FenrirEncounterState", ENCOUNTER_ACTIVE),
                        ENCOUNTER_DORMANT, ENCOUNTER_DEFEATED));
        this.entityData.set(WAKE_TICKS,
                Mth.clamp(input.getIntOr("FenrirWakeTicks", 0), 0, WAKE_DURATION));
        this.noPhysics = false;
        this.setNoGravity(false);
        this.setInvisible(false);
        this.groundWaveUuid = null;
        this.burrowCooldown = 40;
        this.skillSelectionDelay = 20;
        this.glacialSlamCooldown = 0;
        this.lastCombatAction = null;
        this.combatActionDeck.clear();
        this.resetIceOrbRetreat();
        this.resetGlacialSlamRuntime();
        this.applyEncounterLock();
        this.encounterSealPos = input.read("FenrirEncounterSeal", BlockPos.CODEC).orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("FenrirPhaseTwo", this.entityData.get(PHASE_TWO));
        output.putInt("FenrirEncounterState", this.getEncounterState());
        output.putInt("FenrirWakeTicks", this.getWakeTicks());
        if (this.encounterSealPos != null) {
            output.store("FenrirEncounterSeal", BlockPos.CODEC, this.encounterSealPos);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ATTACK_STATE.equals(key)) {
            this.attackTicks = 0;
        } else if (GLACIAL_SLAM_PHASE.equals(key)) {
            this.glacialSlamPhaseTicks = 0;
            this.glacialSlamAnimationSeekPending = true;
        } else if (GLACIAL_SLAM_PHASE_START_TICK.equals(key) && this.level().isClientSide()) {
            this.glacialSlamAnimationSeekPending = true;
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new FenrirPhaseTransitionGoal(this));
        this.goalSelector.addGoal(3, new FenrirActionSelectionGoal(this));
        this.goalSelector.addGoal(4, new FenrirAdvanceGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 14.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.isDeadOrDying()) {
            if (!this.level().isClientSide()) this.clearCombatForDeath();
            return;
        }
        if (this.isEncounterLocked()) {
            this.tickEncounterLock();
            return;
        }
        if (this.isMouthDebugEnabled()) {
            this.setDeltaMovement(Vec3.ZERO);
            if (!this.level().isClientSide()) {
                this.getNavigation().stop();
                this.spawnMouthDebugMarker();
            }
            return;
        }
        if (!this.level().isClientSide()) {
            this.tickPendingHowlHits();
            this.tickPendingGlacialSlamCasts();
        }
        if (FreezeManager.isFrozen(this)) {
            if (!this.level().isClientSide() && this.getAttackState() == ATTACK_GLACIAL_SLAM) {
                this.entityData.set(GLACIAL_SLAM_PHASE_START_TICK,
                        this.entityData.get(GLACIAL_SLAM_PHASE_START_TICK) + 1L);
            }
            return;
        }

        if (!this.level().isClientSide()) {
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
            if (this.burrowCooldown > 0 && this.getPhase() == PHASE_SURFACE) {
                this.burrowCooldown--;
            }
            if (this.skillSelectionDelay > 0 && this.getPhase() == PHASE_SURFACE) {
                this.skillSelectionDelay--;
            }
            if (this.meleeCooldown > 0) this.meleeCooldown--;
            if (this.iceOrbCooldown > 0) this.iceOrbCooldown--;
            if (this.frostHowlCooldown > 0) this.frostHowlCooldown--;
            if (this.glacialSlamCooldown > 0) this.glacialSlamCooldown--;
            if (this.globalSkillCooldown > 0) this.globalSkillCooldown--;
            if (this.getPhase() != PHASE_SURFACE) {
                this.phaseTicks++;
                this.tickBurrowSkill();
            }
        }

        if (this.getAttackState() != ATTACK_IDLE) {
            this.attackTicks++;
            if (!this.level().isClientSide()) {
                this.tickSurfaceAttack();
            }
        } else {
            this.attackTicks = 0;
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
        this.burrowContactCooldowns.clear();
        this.undergroundHealingReceived = 0.0F;
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
        this.tickBurrowContactCooldowns();
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
        this.damageAlongBurrowPath(oldPosition, this.position());
        this.tickUndergroundHealing();

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

        int maxUndergroundTicks = this.entityData.get(PHASE_TWO)
                ? PHASE_TWO_MAX_UNDERGROUND_DURATION : MAX_UNDERGROUND_DURATION;
        if (this.undergroundTicks >= maxUndergroundTicks) {
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
        double phaseBonus = this.entityData.get(PHASE_TWO) ? 0.07D : 0.0D;
        return (this.getPhase() == PHASE_UNDERGROUND_FINAL_CHARGE ? BURROW_FINAL_SPEED : BURROW_HUNT_SPEED)
                + phaseBonus;
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

    private void tickBurrowContactCooldowns() {
        this.burrowContactCooldowns.replaceAll((uuid, ticks) -> ticks - 1);
        this.burrowContactCooldowns.values().removeIf(ticks -> ticks <= 0);
    }

    private void damageAlongBurrowPath(Vec3 start, Vec3 end) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        double minX = Math.min(start.x, end.x) - BURROW_CONTACT_RADIUS;
        double minY = Math.min(start.y, end.y) - 0.75D;
        double minZ = Math.min(start.z, end.z) - BURROW_CONTACT_RADIUS;
        double maxX = Math.max(start.x, end.x) + BURROW_CONTACT_RADIUS;
        double maxY = Math.max(start.y, end.y) + 2.75D;
        double maxZ = Math.max(start.z, end.z) + BURROW_CONTACT_RADIUS;
        AABB sweptArea = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        float damage = this.getPhase() == PHASE_UNDERGROUND_FINAL_CHARGE
                ? FINAL_CHARGE_CONTACT_DAMAGE : BURROW_CONTACT_DAMAGE;

        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, sweptArea,
                target -> target != this && target.isAlive() && !target.isSpectator()
                        && !this.isAlliedTo(target)
                        && !this.burrowContactCooldowns.containsKey(target.getUUID()))) {
            double hitRadius = BURROW_CONTACT_RADIUS + victim.getBbWidth() * 0.5D;
            if (this.horizontalDistanceToSegmentSqr(victim.position(), start, end) > hitRadius * hitRadius) {
                continue;
            }

            float healthBefore = victim.getHealth();
            victim.hurt(this.damageSources().mobAttack(this), damage);
            if (victim.getHealth() >= healthBefore) continue;

            this.burrowContactCooldowns.put(victim.getUUID(), BURROW_CONTACT_COOLDOWN_TICKS);
            Vec3 direction = this.horizontalDirection(end.subtract(start));
            victim.push(direction.x * 0.9D, 0.32D, direction.z * 0.9D);
            ParticleHelper.spawnGroundDebrisBurst(serverLevel,
                    victim.getX(), victim.getY(), victim.getZ(), 0.85F, 14, 0.15F);
            ParticleHelper.spawnLayeredSnowflakes(serverLevel, ParticleHelper.SNOWFLAKE_BURST, 0.3F,
                    victim.getX(), victim.getY() + victim.getBbHeight() * 0.45D, victim.getZ(),
                    10, 0.55D, 0.45D, 0.55D, 0.07D);
            CameraShakeDispatcher.shake(serverLevel, victim.position(), 9.0F, 0.2F, 5);
            serverLevel.playSound(null, victim.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                    net.minecraft.sounds.SoundSource.HOSTILE, 0.9F, 0.62F);
        }
    }

    private double horizontalDistanceToSegmentSqr(Vec3 point, Vec3 start, Vec3 end) {
        double segmentX = end.x - start.x;
        double segmentZ = end.z - start.z;
        double segmentLengthSqr = segmentX * segmentX + segmentZ * segmentZ;
        if (segmentLengthSqr < 1.0E-8D) return this.horizontalDistanceSqr(point, start);
        double progress = Mth.clamp(((point.x - start.x) * segmentX + (point.z - start.z) * segmentZ)
                / segmentLengthSqr, 0.0D, 1.0D);
        double closestX = start.x + segmentX * progress;
        double closestZ = start.z + segmentZ * progress;
        double offsetX = point.x - closestX;
        double offsetZ = point.z - closestZ;
        return offsetX * offsetX + offsetZ * offsetZ;
    }

    private void tickUndergroundHealing() {
        if (this.undergroundTicks % UNDERGROUND_HEAL_INTERVAL != 0 || this.getHealth() >= this.getMaxHealth()) {
            return;
        }
        float maximumHealing = this.getMaxHealth() * UNDERGROUND_MAX_HEAL_RATIO;
        float remainingHealing = maximumHealing - this.undergroundHealingReceived;
        if (remainingHealing <= 0.0F) return;

        float healthBefore = this.getHealth();
        this.heal(Math.min(UNDERGROUND_HEAL_AMOUNT, remainingHealing));
        float healed = this.getHealth() - healthBefore;
        if (healed <= 0.0F) return;
        this.undergroundHealingReceived += healed;

        Vec3 effectPosition = this.lastSurfacePosition == null ? this.position() : this.lastSurfacePosition;
        ParticleHelper.spawnLayeredSnowflakes(this.level(), ParticleHelper.SNOWFLAKE_AURA, 0.32F,
                effectPosition.x, effectPosition.y + 0.22D, effectPosition.z,
                9, 0.8D, 0.25D, 0.8D, 0.025D);
        if (this.undergroundTicks % (UNDERGROUND_HEAL_INTERVAL * 2) == 0) {
            this.level().playSound(null, effectPosition.x, effectPosition.y, effectPosition.z,
                    SoundEvents.AMETHYST_BLOCK_RESONATE, net.minecraft.sounds.SoundSource.HOSTILE,
                    0.45F, 1.45F);
        }
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
        this.burrowContactCooldowns.clear();
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
        ParticleHelper.spawnLayeredSnowflakes(this.level(), ParticleHelper.SNOWFLAKE_BURST, 0.48F,
                this.getX(), this.getY() + 0.7D, this.getZ(), 32, 2.1D, 1.0D, 2.1D, 0.1D);
        if (this.entityData.get(PHASE_TWO)) {
            this.spawnHowlSpikeRing(0);
        }
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
        this.burrowCooldown = this.entityData.get(PHASE_TWO) ? PHASE_TWO_BURROW_COOLDOWN : BURROW_COOLDOWN;
        this.entityData.set(PHASE, PHASE_SURFACE);
        this.noPhysics = false;
        this.setNoGravity(false);
        this.setInvisible(false);
        this.setDeltaMovement(Vec3.ZERO);
    }

    private int getAttackState() {
        return this.entityData.get(ATTACK_STATE);
    }

    private boolean isSurfaceIdle() {
        return this.getPhase() == PHASE_SURFACE && this.getAttackState() == ATTACK_IDLE;
    }

    private boolean shouldEnterPhaseTwo() {
        return !this.entityData.get(PHASE_TWO) && this.getHealth() <= this.getMaxHealth() * PHASE_TWO_HEALTH_RATIO;
    }

    private void startSurfaceAttack(int attackState) {
        this.attackTicks = 0;
        this.lockedAttackDirection = Vec3.ZERO;
        this.resetIceOrbRetreat();
        if (attackState == ATTACK_ICE_ORB) {
            LivingEntity target = this.getTarget();
            if (target != null && this.horizontalDistanceSqr(this.position(), target.position()) < Mth.square(8.0D)) {
                this.iceOrbRetreatLanding = this.findTacticalRetreatLanding(target, 5.0D, 8.0D);
                this.entityData.set(ICE_ORB_RETREAT_ANIMATION, this.iceOrbRetreatLanding != null);
            }
        }
        if (attackState != ATTACK_GLACIAL_SLAM) {
            this.resetGlacialSlamRuntime();
        }
        this.entityData.set(ATTACK_STATE, attackState);
        if (attackState == ATTACK_FROST_HOWL || attackState == ATTACK_PHASE_TRANSITION) {
            this.howlSafeAngle = (this.getYRot() + 90.0F) * Mth.DEG_TO_RAD + Math.PI;
        }
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        switch (attackState) {
            case ATTACK_BITE -> this.meleeCooldown = 26;
            case ATTACK_CLAWS -> this.meleeCooldown = 32;
            case ATTACK_ICE_ORB -> this.iceOrbCooldown = this.entityData.get(PHASE_TWO) ? 65 : ICE_ORB_COOLDOWN;
            case ATTACK_FROST_HOWL -> this.frostHowlCooldown = FROST_HOWL_COOLDOWN;
            case ATTACK_GLACIAL_SLAM -> this.glacialSlamCooldown = this.entityData.get(PHASE_TWO)
                    ? PHASE_TWO_GLACIAL_SLAM_COOLDOWN : GLACIAL_SLAM_COOLDOWN;
            default -> {
            }
        }
    }

    private void finishSurfaceAttack() {
        boolean finishedGlacialSlam = this.getAttackState() == ATTACK_GLACIAL_SLAM;
        if (finishedGlacialSlam && !this.glacialSlamImpactApplied) {
            this.glacialSlamCooldown = Math.min(this.glacialSlamCooldown, 20);
        }
        this.attackTicks = 0;
        this.lockedAttackDirection = Vec3.ZERO;
        this.resetIceOrbRetreat();
        this.entityData.set(ATTACK_STATE, ATTACK_IDLE);
        if (finishedGlacialSlam) {
            this.resetGlacialSlamRuntime();
        }
        this.globalSkillCooldown = this.entityData.get(PHASE_TWO)
                ? PHASE_TWO_GLOBAL_SKILL_COOLDOWN : GLOBAL_SKILL_COOLDOWN;
    }

    private void tickSurfaceAttack() {
        this.getNavigation().stop();
        LivingEntity target = this.getTarget();
        int state = this.getAttackState();
        boolean tacticalMovement = state == ATTACK_GLACIAL_SLAM && this.getGlacialSlamPhase() == GLACIAL_SLAM_LEAP
                || state == ATTACK_ICE_ORB && this.iceOrbRetreatLanding != null;
        if (!tacticalMovement) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.35D, 1.0D, 0.35D));
        }
        int trackingEnd = switch (state) {
            case ATTACK_BITE -> 5;
            case ATTACK_CLAWS -> 6;
            case ATTACK_ICE_ORB -> ICE_ORB_AIM_LOCK_TICK;
            case ATTACK_FROST_HOWL, ATTACK_PHASE_TRANSITION -> 3;
            case ATTACK_GLACIAL_SLAM -> this.getGlacialSlamPhase() == GLACIAL_SLAM_PREPARE ? Integer.MAX_VALUE : 0;
            default -> 0;
        };
        if (target != null && target.isAlive() && this.attackTicks <= trackingEnd) {
            this.getLookControl().setLookAt(target, 35.0F, 30.0F);
            this.faceTargetHorizontally(target, 18.0F);
        }

        switch (state) {
            case ATTACK_BITE -> this.tickBite();
            case ATTACK_CLAWS -> this.tickClaws();
            case ATTACK_ICE_ORB -> this.tickIceOrb();
            case ATTACK_FROST_HOWL -> this.tickFrostHowl();
            case ATTACK_PHASE_TRANSITION -> this.tickPhaseTransition();
            case ATTACK_GLACIAL_SLAM -> this.tickGlacialSlam();
            default -> this.finishSurfaceAttack();
        }
    }

    private void tickBite() {
        if (this.attackTicks == 4) {
            this.lockedAttackDirection = this.getHorizontalForward();
            this.playSound(SoundEvents.POLAR_BEAR_WARNING, 1.0F, 0.72F);
        }
        if (this.attackTicks == 7) this.stepForward(0.42D);
        if (this.attackTicks == BITE_HIT_TICK) {
            this.performFrontalFrostAttack(4.0D, 0.35D, 16.0F, 1.0D, 0.18D, this.lockedAttackDirection);
            this.playSound(SoundEvents.RAVAGER_ATTACK, 1.1F, 0.84F);
        }
        if (this.attackTicks >= BITE_DURATION) this.finishSurfaceAttack();
    }

    private void tickClaws() {
        if (this.attackTicks == 5) {
            this.lockedAttackDirection = this.getHorizontalForward();
            this.playSound(SoundEvents.POLAR_BEAR_WARNING, 1.05F, 0.62F);
        }
        if (this.attackTicks == 9) this.stepForward(0.5D);
        if (this.attackTicks == CLAWS_HIT_TICK) {
            this.performFrontalFrostAttack(4.8D, 0.05D, 13.0F, 1.45D, 0.28D, this.lockedAttackDirection);
            ParticleHelper.spawnClawSlash(this.level(), this, this.lockedAttackDirection);
            this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.65F);
            ParticleHelper.spawnGroundImpact(this.level(), this, 2.6F, 0.0F, 0.1D,
                    0.45F, 0.88F, 1.0F, 0.86F, 0.9F, 3.4F, 12,
                    false, 0.0F, 0);
        }
        if (this.attackTicks >= CLAWS_DURATION) this.finishSurfaceAttack();
    }

    private void tickIceOrb() {
        if (this.attackTicks == 1) {
            if (this.iceOrbRetreatLanding != null) {
                Vec3 retreat = this.horizontalDirection(this.iceOrbRetreatLanding.subtract(this.position()));
                double distance = Math.sqrt(this.horizontalDistanceSqr(this.position(), this.iceOrbRetreatLanding));
                this.setDeltaMovement(retreat.x * Mth.clamp(distance / 9.0D, 0.48D, 0.78D),
                        0.58D, retreat.z * Mth.clamp(distance / 9.0D, 0.48D, 0.78D));
                this.iceOrbRetreatBecameAirborne = false;
                this.playSound(SoundEvents.ENDER_DRAGON_FLAP, 0.8F, 0.88F);
                ParticleHelper.spawnGroundDebrisBurst(this.level(), this.getX(), this.getY(), this.getZ(),
                        0.9F, 16, 0.16F);
            }
        }
        this.tickIceOrbRetreat();
        Vec3 mouth = this.getMouthPosition();
        if (this.attackTicks >= 5 && this.attackTicks < ICE_ORB_FIRE_TICK && (this.attackTicks & 1) == 0) {
            ParticleHelper.spawnLayeredSnowflakes(this.level(), ParticleHelper.SNOWFLAKE_AURA, 0.5F,
                    mouth.x, mouth.y, mouth.z, 8, 0.35D, 0.3D, 0.35D, 0.025D);
        }
        if (this.attackTicks == 8) {
            this.playSound(SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.1F, 1.35F);
        }
        if (this.attackTicks == ICE_ORB_AIM_LOCK_TICK) {
            LivingEntity target = this.getTarget();
            Vec3 aim = target == null ? mouth.add(this.getLookAngle().scale(12.0D))
                    : target.getEyePosition().add(target.getDeltaMovement().scale(5.0D));
            this.lockedAttackDirection = aim.subtract(mouth).normalize();
        }
        if (this.attackTicks == ICE_ORB_FIRE_TICK) {
            this.fireIceOrbVolley(mouth);
        }
        if (this.attackTicks >= ICE_ORB_DURATION) this.finishSurfaceAttack();
    }

    private void tickIceOrbRetreat() {
        if (this.iceOrbRetreatLanding == null) return;
        this.resetFallDistance();
        if (!this.onGround()) this.iceOrbRetreatBecameAirborne = true;
        if (this.iceOrbRetreatBecameAirborne && this.onGround() && this.attackTicks >= 4) {
            this.stopIceOrbRetreatMovement();
            return;
        }
        Vec3 remaining = this.iceOrbRetreatLanding.subtract(this.position());
        if (remaining.horizontalDistanceSqr() < 0.36D || this.horizontalCollision) return;
        Vec3 retreat = this.horizontalDirection(remaining);
        Vec3 current = this.getDeltaMovement();
        double desiredSpeed = Mth.clamp(Math.sqrt(remaining.horizontalDistanceSqr()) * 0.12D, 0.18D, 0.75D);
        this.setDeltaMovement(Mth.lerp(0.16D, current.x, retreat.x * desiredSpeed),
                current.y, Mth.lerp(0.16D, current.z, retreat.z * desiredSpeed));
    }

    private void resetIceOrbRetreat() {
        this.stopIceOrbRetreatMovement();
        this.entityData.set(ICE_ORB_RETREAT_ANIMATION, false);
    }

    private void stopIceOrbRetreatMovement() {
        this.iceOrbRetreatLanding = null;
        this.iceOrbRetreatBecameAirborne = false;
    }

    private void fireIceOrbVolley(Vec3 mouth) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        Vec3 direction = this.lockedAttackDirection.lengthSqr() < 1.0E-4D ? this.getLookAngle() : this.lockedAttackDirection;
        int shots = this.entityData.get(PHASE_TWO) ? 3 : 1;
        for (int index = 0; index < shots; index++) {
            double angle = shots == 1 ? 0.0D : (index - 1) * 8.0D * Mth.DEG_TO_RAD;
            Vec3 shotDirection = this.rotateProjectileYaw(direction, angle);
            serverLevel.addFreshEntity(new FenrirIceOrbEntity(serverLevel, this, mouth, shotDirection, 1.25F));
        }
        this.playSound(SoundEvents.BREEZE_SHOOT, 1.35F, 0.62F);
        this.playSound(SoundEvents.GLASS_PLACE, 0.9F, 1.4F);
    }

    private void tickFrostHowl() {
        if (this.attackTicks == 5) {
            this.playSound(SoundEvents.RAVAGER_ROAR, 1.7F, 0.58F);
            ParticleHelper.spawnImpactRing(this.level(), this.getX(), this.getY() + 0.1D, this.getZ(),
                    0.35F, 0.9F, 1.0F, 0.9F, 1.0F, 9.5F, 24, RingBehavior.GROW);
        }
        for (int ring = 0; ring < 3; ring++) {
            int spikeTick = FROST_HOWL_SPIKE_TICK + ring * 10;
            if (this.attackTicks == spikeTick - 9 || this.attackTicks == spikeTick - 4) {
                this.spawnHowlSpikeRingTelegraph(ring, spikeTick);
            }
            if (this.attackTicks == spikeTick) {
                this.spawnHowlSpikeRing(ring);
            }
        }
        if (this.attackTicks >= FROST_HOWL_DURATION) this.finishSurfaceAttack();
    }

    private void tickPhaseTransition() {
        if (this.attackTicks == 1) {
            this.playSound(SoundEvents.RAVAGER_ROAR, 2.0F, 0.46F);
        }
        if (this.attackTicks == 2 || this.attackTicks == 6 || this.attackTicks == 10
                || this.attackTicks == 14) {
            this.spawnGroundCircleTelegraph(this.position(), 5.0D, 28);
        }
        if (this.attackTicks == 16) {
            this.performRadialFrostAttack(5.0D, 0.75D, 0.22D, false);
            ParticleHelper.spawnImpactRing(this.level(), this.getX(), this.getY() + 0.15D, this.getZ(),
                    0.3F, 0.86F, 1.0F, 1.0F, 1.2F, 7.5F, 20, RingBehavior.GROW_THEN_SHRINK);
            if (this.level() instanceof ServerLevel serverLevel) {
                CameraShakeDispatcher.shake(serverLevel, this.position(), 24.0F, 0.7F, 14);
            }
        }
        for (int ring = 0; ring < 3; ring++) {
            int spikeTick = 20 + ring * 10;
            if (this.attackTicks == spikeTick - 9 || this.attackTicks == spikeTick - 4) {
                this.spawnHowlSpikeRingTelegraph(ring, spikeTick);
            }
            if (this.attackTicks == spikeTick) {
                this.spawnHowlSpikeRing(ring);
            }
        }
        if (this.attackTicks >= PHASE_TRANSITION_DURATION) {
            this.entityData.set(PHASE_TWO, true);
            this.iceOrbCooldown = 25;
            this.frostHowlCooldown = 35;
            this.glacialSlamCooldown = Math.min(this.glacialSlamCooldown, 20);
            this.burrowCooldown = Math.min(this.burrowCooldown, 40);
            this.combatActionDeck.clear();
            this.finishSurfaceAttack();
        }
    }

    private int getGlacialSlamPhase() {
        return this.entityData.get(GLACIAL_SLAM_PHASE);
    }

    private void setGlacialSlamPhase(int phase) {
        this.entityData.set(GLACIAL_SLAM_PHASE_START_TICK, this.level().getGameTime());
        this.entityData.set(GLACIAL_SLAM_PHASE, phase);
        this.glacialSlamPhaseTicks = 0;
    }

    private void startGlacialSlam(LivingEntity target) {
        this.resetGlacialSlamRuntime();
        this.glacialSlamTargetUuid = target.getUUID();
        this.glacialSlamTargetPosition = target.position();
        this.glacialSlamRetreatDirection = this.horizontalDirection(this.position().subtract(target.position()));
        if (this.glacialSlamRetreatDirection.lengthSqr() < 1.0E-4D) {
            this.glacialSlamRetreatDirection = this.getHorizontalForward().scale(-1.0D);
        }
        this.setGlacialSlamPhase(GLACIAL_SLAM_PREPARE);
        this.startSurfaceAttack(ATTACK_GLACIAL_SLAM);
    }

    private void tickGlacialSlam() {
        this.glacialSlamPhaseTicks++;
        switch (this.getGlacialSlamPhase()) {
            case GLACIAL_SLAM_LEAP -> this.tickGlacialSlamLeap();
            case GLACIAL_SLAM_IMPACT -> this.tickGlacialSlamImpact();
            case GLACIAL_SLAM_RECOVERY -> this.tickGlacialSlamRecovery();
            default -> this.tickGlacialSlamPreparation();
        }
    }

    private void tickGlacialSlamPreparation() {
        LivingEntity target = this.getGlacialSlamTarget();
        if (target != null) {
            this.glacialSlamTargetPosition = target.position();
            this.faceTargetHorizontally(target, 24.0F);
        }
        if (this.glacialSlamPhaseTicks == 2) {
            this.playSound(SoundEvents.POLAR_BEAR_WARNING, 1.25F, 0.58F);
            ParticleHelper.spawnImpactRing(this.level(), this.getX(), this.getY() + 0.08D, this.getZ(),
                    0.35F, 0.88F, 1.0F, 0.78F, 0.7F, 2.8F, 9, RingBehavior.GROW_THEN_SHRINK);
        }
        if (this.glacialSlamPhaseTicks < GLACIAL_SLAM_PREPARE_DURATION) return;

        if (!this.onGround() || this.glacialSlamRetreatDirection.lengthSqr() < 1.0E-4D) {
            this.beginGlacialSlamImpact();
            return;
        }
        this.setDeltaMovement(this.glacialSlamRetreatDirection.x * 0.68D, 0.58D,
                this.glacialSlamRetreatDirection.z * 0.68D);
        this.glacialSlamBecameAirborne = false;
        this.setGlacialSlamPhase(GLACIAL_SLAM_LEAP);
        this.playSound(SoundEvents.ENDER_DRAGON_FLAP, 1.0F, 0.72F);
        ParticleHelper.spawnGroundDebrisBurst(this.level(), this.getX(), this.getY(), this.getZ(), 1.25F, 24, 0.2F);
    }

    private void tickGlacialSlamLeap() {
        this.resetFallDistance();
        LivingEntity target = this.getGlacialSlamTarget();
        if (target != null) {
            this.faceTargetHorizontally(target, 30.0F);
        }
        if (!this.onGround()) {
            this.glacialSlamBecameAirborne = true;
        }
        if (this.glacialSlamBecameAirborne && this.onGround() && this.glacialSlamPhaseTicks >= 4) {
            this.beginGlacialSlamImpact();
            return;
        }
        if (this.glacialSlamPhaseTicks >= GLACIAL_SLAM_LEAP_TIMEOUT) {
            this.beginGlacialSlamImpact();
            return;
        }
        if (this.horizontalCollision || this.glacialSlamRetreatDirection.lengthSqr() < 1.0E-4D) return;
        Vec3 current = this.getDeltaMovement();
        double correctedX = Mth.lerp(0.16D, current.x, this.glacialSlamRetreatDirection.x * 0.58D);
        double correctedZ = Mth.lerp(0.16D, current.z, this.glacialSlamRetreatDirection.z * 0.58D);
        this.setDeltaMovement(correctedX, current.y, correctedZ);
    }

    private void beginGlacialSlamImpact() {
        this.setDeltaMovement(Vec3.ZERO);
        LivingEntity target = this.getGlacialSlamTarget();
        if (target != null) {
            this.glacialSlamTargetPosition = target.position();
        }
        this.glacialSlamDirection = this.horizontalDirection(this.glacialSlamTargetPosition.subtract(this.position()));
        if (this.glacialSlamDirection.lengthSqr() < 1.0E-4D) {
            float yaw = (this.getYRot() + 90.0F) * Mth.DEG_TO_RAD;
            this.glacialSlamDirection = new Vec3(Mth.cos(yaw), 0.0D, Mth.sin(yaw));
        }

        this.lockRotationToDirection(this.glacialSlamDirection);
        this.glacialSlamPlacements = this.collectDirectGlacialSlamPlacements();
        this.glacialSlamImpactApplied = false;
        this.setGlacialSlamPhase(GLACIAL_SLAM_IMPACT);
        this.spawnGlacialSlamTelegraphs();
        this.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.4F, 0.48F);
        this.playSound(SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.15F, 0.62F);
        ParticleHelper.spawnLayeredSnowflakes(this.level(), ParticleHelper.SNOWFLAKE_AURA, 0.62F,
                this.getX(), this.getY() + 2.2D, this.getZ(), 24, 0.9D, 1.25D, 0.9D, 0.04D);
    }

    private void tickGlacialSlamImpact() {
        this.setDeltaMovement(Vec3.ZERO);
        this.lockRotationToDirection(this.glacialSlamDirection);
        if (this.glacialSlamPhaseTicks == 4 || this.glacialSlamPhaseTicks == 8
                || this.glacialSlamPhaseTicks == 12 || this.glacialSlamPhaseTicks == 16) {
            this.spawnGlacialSlamTelegraphs();
        }
        if (!this.glacialSlamImpactApplied && this.glacialSlamPhaseTicks >= GLACIAL_SLAM_IMPACT_TICK) {
            this.glacialSlamImpactApplied = true;
            this.performGlacialSlamImpact();
        }
        if (this.glacialSlamPhaseTicks >= GLACIAL_SLAM_IMPACT_DURATION) {
            this.setGlacialSlamPhase(GLACIAL_SLAM_RECOVERY);
        }
    }

    private void tickGlacialSlamRecovery() {
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
        if (this.glacialSlamPhaseTicks >= GLACIAL_SLAM_RECOVERY_TIMEOUT
                || this.glacialSlamPhaseTicks >= GLACIAL_SLAM_RECOVERY_DURATION && this.onGround()) {
            this.finishSurfaceAttack();
        }
    }

    private List<GlacialSlamPlacement> collectDirectGlacialSlamPlacements() {
        ArrayList<GlacialSlamPlacement> placements = new ArrayList<>();
        Vec3 right = new Vec3(-this.glacialSlamDirection.z, 0.0D, this.glacialSlamDirection.x);
        for (int row = 0; row < GLACIAL_SLAM_ROWS.length; row++) {
            double distance = 2.0D + row * 2.0D;
            Vec3 center = this.position().add(this.glacialSlamDirection.scale(distance));
            float scale = 1.08F + row * 0.14F;
            for (double sideOffset : GLACIAL_SLAM_ROWS[row]) {
                Vec3 desired = center.add(right.scale(sideOffset));
                placements.add(new GlacialSlamPlacement(
                        new Vec3(desired.x, this.getY(), desired.z), row, row * 2, scale));
            }
        }
        return List.copyOf(placements);
    }

    private void spawnGlacialSlamTelegraphs() {
        this.spawnGroundCircleTelegraph(this.position(), GLACIAL_SLAM_CENTRAL_RADIUS, 22);
        for (GlacialSlamPlacement placement : this.glacialSlamPlacements) {
            float radius = 0.72F + placement.scale * 0.34F;
            this.spawnIceTelegraphRing(placement.position, radius, 4);
        }
    }

    private void performGlacialSlamImpact() {
        if (!(this.level() instanceof ServerLevel level) || this.glacialSlamPlacements.isEmpty()) return;
        this.playSound(ModSounds.GROUND_SLAM.get(), 1.65F, 0.72F);
        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.82F);
        ParticleHelper.spawnGroundDebrisBurst(level, this.getX(), this.getY(), this.getZ(), 2.1F, 52, 0.34F);
        ParticleHelper.spawnImpactRing(level, this.getX(), this.getY() + 0.1D, this.getZ(),
                0.3F, 0.9F, 1.0F, 0.96F, 1.15F, 6.2F, 18, RingBehavior.GROW_THEN_SHRINK);
        ParticleHelper.spawnLayeredSnowflakes(level, ParticleHelper.SNOWFLAKE_BURST, 0.5F,
                this.getX(), this.getY() + 0.65D, this.getZ(), 42, 2.2D, 1.0D, 2.2D, 0.12D);
        CameraShakeDispatcher.shake(level, this.position(), 24.0F, 0.78F, 12);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(GLACIAL_SLAM_CENTRAL_RADIUS, 1.5D, GLACIAL_SLAM_CENTRAL_RADIUS),
                target -> this.isValidSpikeTarget(target)
                        && this.horizontalDistanceSqr(this.position(), target.position())
                                <= Mth.square(GLACIAL_SLAM_CENTRAL_RADIUS))) {
            Vec3 away = this.horizontalDirection(victim.position().subtract(this.position()));
            victim.hurt(this.damageSources().mobAttack(this), 7.0F);
            if (away.lengthSqr() > 1.0E-4D) victim.push(away.x * 0.65D, 0.28D, away.z * 0.65D);
        }

        float spikeYaw = (float) (Mth.atan2(this.glacialSlamDirection.x, this.glacialSlamDirection.z)
                * Mth.RAD_TO_DEG);
        ArrayList<PendingGlacialSlamRow> pendingRows = new ArrayList<>();
        for (int row = 0; row < GLACIAL_SLAM_ROWS.length; row++) {
            ArrayList<AABB> damageAreas = new ArrayList<>();
            int rowDelay = row * 2;
            for (GlacialSlamPlacement placement : this.glacialSlamPlacements) {
                if (placement.row != row) continue;
                IceSpikeEntity spike = IceSpikeEntity.create(level, placement.position.x, placement.position.y,
                        placement.position.z, spikeYaw, placement.delay, this.random.nextInt(), placement.scale);
                level.addFreshEntity(spike);
                level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                                net.minecraft.core.particles.ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                        placement.position.x, placement.position.y + 0.08D, placement.position.z,
                        10, 0.3D, 0.12D, 0.3D, 0.1D);
                double halfWidth = 0.65D + placement.scale * 0.3D;
                double height = 2.4D + placement.scale * 0.75D;
                damageAreas.add(new AABB(placement.position.x - halfWidth, placement.position.y - 0.35D,
                        placement.position.z - halfWidth, placement.position.x + halfWidth,
                        placement.position.y + height, placement.position.z + halfWidth));
            }
            if (!damageAreas.isEmpty()) {
                pendingRows.add(new PendingGlacialSlamRow(
                        this.tickCount + rowDelay + IceSpikeEntity.EMERGE_TICKS, List.copyOf(damageAreas)));
            }
        }
        if (!pendingRows.isEmpty()) {
            this.pendingGlacialSlamCasts.add(new PendingGlacialSlamCast(
                    new ArrayList<>(pendingRows), new HashSet<>(), this.glacialSlamDirection));
        }
        this.playSound(SoundEvents.GLASS_BREAK, 1.25F, 0.72F);
    }

    private void tickPendingGlacialSlamCasts() {
        if (!(this.level() instanceof ServerLevel level) || this.pendingGlacialSlamCasts.isEmpty()) return;
        Iterator<PendingGlacialSlamCast> castIterator = this.pendingGlacialSlamCasts.iterator();
        while (castIterator.hasNext()) {
            PendingGlacialSlamCast cast = castIterator.next();
            Iterator<PendingGlacialSlamRow> rowIterator = cast.rows.iterator();
            while (rowIterator.hasNext()) {
                PendingGlacialSlamRow row = rowIterator.next();
                if (row.triggerTick > this.tickCount) continue;
                AABB queryArea = row.areas.stream().reduce(AABB::minmax).orElse(this.getBoundingBox());
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, queryArea,
                        target -> this.isValidSpikeTarget(target) && !cast.hitTargets.contains(target.getUUID()))) {
                    if (row.areas.stream().noneMatch(area -> area.intersects(victim.getBoundingBox()))) continue;
                    float healthBefore = victim.getHealth();
                    float absorptionBefore = victim.getAbsorptionAmount();
                    victim.hurt(this.damageSources().mobAttack(this), GLACIAL_SLAM_SPIKE_DAMAGE);
                    if (victim.getHealth() >= healthBefore && victim.getAbsorptionAmount() >= absorptionBefore) continue;
                    cast.hitTargets.add(victim.getUUID());
                    victim.push(cast.direction.x * 0.7D, 0.34D, cast.direction.z * 0.7D);
                }
                rowIterator.remove();
            }
            if (cast.rows.isEmpty()) castIterator.remove();
        }
    }

    private void lockRotationToDirection(Vec3 direction) {
        if (direction.horizontalDistanceSqr() < 1.0E-4D) return;
        float yaw = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
    }

    private LivingEntity getGlacialSlamTarget() {
        if (!(this.level() instanceof ServerLevel level) || this.glacialSlamTargetUuid == null) return null;
        Entity target = level.getEntity(this.glacialSlamTargetUuid);
        return target instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private void resetGlacialSlamRuntime() {
        this.glacialSlamPhaseTicks = 0;
        this.glacialSlamBecameAirborne = false;
        this.glacialSlamImpactApplied = false;
        this.glacialSlamTargetUuid = null;
        this.glacialSlamRetreatDirection = Vec3.ZERO;
        this.glacialSlamTargetPosition = Vec3.ZERO;
        this.glacialSlamDirection = Vec3.ZERO;
        this.glacialSlamPlacements = List.of();
        this.glacialSlamAnimationSeekPending = false;
        if (this.getGlacialSlamPhase() != GLACIAL_SLAM_PREPARE) {
            this.entityData.set(GLACIAL_SLAM_PHASE, GLACIAL_SLAM_PREPARE);
        }
    }

    private void spawnHowlSpikeRing(int ring) {
        if (!(this.level() instanceof ServerLevel level)) return;
        double[] radii = {3.25D, 5.4D, 7.6D};
        int[] slots = {8, 12, 16};
        float[] scales = {0.75F, 0.95F, 1.15F};
        double safeAngle = this.howlSafeAngle;
        double radius = radii[ring];
        int count = slots[ring];
        for (int slot = 0; slot < count; slot++) {
            double angle = Math.PI * 2.0D * slot / count;
            double difference = Math.abs(Mth.wrapDegrees((float) ((angle - safeAngle) * Mth.RAD_TO_DEG)));
            if (difference < 28.0D) continue;
            Vec3 desired = this.position().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            Vec3 surface = this.findSpikeSurfacePosition(desired);
            if (surface == null) continue;
            IceSpikeEntity spike = IceSpikeEntity.create(level, surface.x, surface.y, surface.z,
                    (float) (angle * Mth.RAD_TO_DEG), 0, this.random.nextInt(), scales[ring]);
            level.addFreshEntity(spike);
            level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                            net.minecraft.core.particles.ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                    surface.x, surface.y + 0.08D, surface.z, 8, 0.25D, 0.12D, 0.25D, 0.08D);
            this.pendingHowlHits.add(new PendingHowlHit(this.tickCount + HOWL_DAMAGE_DELAY,
                    new AABB(surface.x - 0.8D, surface.y - 0.25D, surface.z - 0.8D,
                            surface.x + 0.8D, surface.y + 2.8D, surface.z + 0.8D)));
        }
        this.playSound(SoundEvents.GLASS_BREAK, 1.0F, 0.85F + ring * 0.12F);
        CameraShakeDispatcher.shake(level, this.position(), 16.0F, 0.16F + ring * 0.05F, 6);
    }

    private void spawnHowlSpikeRingTelegraph(int ring, int spikeTick) {
        double[] radii = {3.25D, 5.4D, 7.6D};
        int[] slots = {8, 12, 16};
        float[] scales = {0.75F, 0.95F, 1.15F};
        double radius = radii[ring];
        int count = slots[ring];
        for (int slot = 0; slot < count; slot++) {
            double angle = Math.PI * 2.0D * slot / count;
            double difference = Math.abs(Mth.wrapDegrees((float) ((angle - this.howlSafeAngle) * Mth.RAD_TO_DEG)));
            if (difference < 28.0D) continue;
            Vec3 desired = this.position().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            Vec3 surface = this.findGroundTelegraphPosition(desired);
            if (surface == null) continue;
            float markerRadius = 0.65F + scales[ring] * 0.3F;
            int duration = Math.max(1, spikeTick + HOWL_DAMAGE_DELAY - this.attackTicks);
            this.spawnIceTelegraphRing(surface, markerRadius, duration);
        }
    }

    private void spawnGroundCircleTelegraph(Vec3 center, double radius, int ignoredSamples) {
        Vec3 surface = this.findGroundTelegraphPosition(center);
        if (surface != null) this.spawnIceTelegraphRing(surface, (float) radius, 4);
    }

    private void spawnIceTelegraphRing(Vec3 surface, float radius, int duration) {
        ParticleHelper.spawnImpactRing(this.level(), surface.x, surface.y + 0.06D, surface.z,
                0.22F, 0.82F, 1.0F, 0.76F, 1.0F, radius, duration, RingBehavior.CONSTANT);
    }

    private Vec3 findGroundTelegraphPosition(Vec3 desiredPosition) {
        BlockPos center = BlockPos.containing(desiredPosition);
        for (int distance = 0; distance <= 4; distance++) {
            int[] offsets = distance == 0 ? new int[] {0} : new int[] {-distance, distance};
            for (int offset : offsets) {
                BlockPos support = new BlockPos(center.getX(), center.getY() + offset - 1, center.getZ());
                net.minecraft.world.phys.shapes.VoxelShape shape = this.level().getBlockState(support)
                        .getCollisionShape(this.level(), support);
                if (shape.isEmpty()) continue;
                return new Vec3(desiredPosition.x,
                        support.getY() + shape.max(net.minecraft.core.Direction.Axis.Y), desiredPosition.z);
            }
        }
        return null;
    }

    private void performFrontalFrostAttack(double range, double dotThreshold, float damage,
            double horizontalKnockback, double verticalKnockback, Vec3 lockedDirection) {
        Vec3 forward = lockedDirection == null || lockedDirection.horizontalDistanceSqr() < 1.0E-4D
                ? this.getHorizontalForward() : lockedDirection;
        if (forward.horizontalDistanceSqr() < 1.0E-4D) return;
        forward = new Vec3(forward.x, 0.0D, forward.z).normalize();
        for (LivingEntity target : this.getAttackCandidates(range)) {
            Vec3 offset = target.position().subtract(this.position());
            Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
            if (horizontal.lengthSqr() > 1.0E-4D && horizontal.lengthSqr() <= range * range
                    && forward.dot(horizontal.normalize()) >= dotThreshold) {
                float healthBefore = target.getHealth();
                target.hurt(this.damageSources().mobAttack(this), damage);
                if (target.getHealth() >= healthBefore) continue;
                Vec3 push = horizontal.normalize().scale(horizontalKnockback);
                target.push(push.x, verticalKnockback, push.z);
            }
        }
    }

    private void tickPendingHowlHits() {
        if (!(this.level() instanceof ServerLevel level) || this.pendingHowlHits.isEmpty()) return;
        java.util.Iterator<PendingHowlHit> iterator = this.pendingHowlHits.iterator();
        while (iterator.hasNext()) {
            PendingHowlHit hit = iterator.next();
            if (hit.triggerTick > this.tickCount) continue;
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, hit.area, this::isValidSpikeTarget)) {
                float healthBefore = victim.getHealth();
                victim.hurt(this.damageSources().mobAttack(this), 9.0F);
                if (victim.getHealth() < healthBefore) {
                    Vec3 push = victim.position().subtract(hit.area.getCenter());
                    if (push.horizontalDistanceSqr() > 1.0E-4D) {
                        Vec3 knockback = new Vec3(push.x, 0.0D, push.z).normalize().scale(0.45D);
                        victim.push(knockback.x, 0.35D, knockback.z);
                    }
                }
            }
            iterator.remove();
        }
    }

    private void performRadialFrostAttack(double range, double horizontalKnockback,
            double verticalKnockback, boolean damage) {
        for (LivingEntity target : this.getAttackCandidates(range)) {
            if (this.horizontalDistanceSqr(this.position(), target.position()) > Mth.square(range)) continue;
            float healthBefore = target.getHealth();
            if (damage) target.hurt(this.damageSources().mobAttack(this), 8.0F);
            boolean affected = !damage || target.getHealth() < healthBefore;
            if (!affected) continue;
            Vec3 offset = target.position().subtract(this.position());
            Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
            if (horizontal.lengthSqr() > 1.0E-4D) {
                Vec3 push = horizontal.normalize().scale(horizontalKnockback);
                target.push(push.x, verticalKnockback, push.z);
            }
        }
    }

    private List<LivingEntity> getAttackCandidates(double range) {
        return this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(range, 2.0D, range), this::isValidAttackTarget);
    }

    private boolean isValidAttackTarget(LivingEntity target) {
        return target != this && target.isAlive() && !target.isSpectator() && !target.isInvulnerable()
                && !this.isAlliedTo(target) && this.hasLineOfSight(target);
    }

    private boolean isValidSpikeTarget(LivingEntity target) {
        return target != this && target.isAlive() && !target.isSpectator() && !target.isInvulnerable()
                && !this.isAlliedTo(target);
    }

    private void faceTargetHorizontally(LivingEntity target, float maxTurn) {
        double x = target.getX() - this.getX();
        double z = target.getZ() - this.getZ();
        if (x * x + z * z < 1.0E-4D) return;
        float desired = (float) (Mth.atan2(z, x) * Mth.RAD_TO_DEG) - 90.0F;
        float yaw = Mth.approachDegrees(this.getYRot(), desired, maxTurn);
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
    }

    private Vec3 getHorizontalForward() {
        Vec3 look = this.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        return horizontal.lengthSqr() < 1.0E-4D ? Vec3.ZERO : horizontal.normalize();
    }

    private void stepForward(double strength) {
        Vec3 forward = this.getHorizontalForward();
        this.setDeltaMovement(forward.x * strength, this.getDeltaMovement().y, forward.z * strength);
    }

    private Vec3 getMouthPosition() {
        return this.getLocalMouthPosition(DEFAULT_MOUTH_FORWARD, DEFAULT_MOUTH_UP, DEFAULT_MOUTH_SIDE);
    }

    public boolean isMouthDebugEnabled() {
        return this.entityData.get(MOUTH_DEBUG);
    }

    public float getMouthDebugForward() {
        return this.entityData.get(MOUTH_DEBUG_FORWARD);
    }

    public float getMouthDebugUp() {
        return this.entityData.get(MOUTH_DEBUG_UP);
    }

    public float getMouthDebugSide() {
        return this.entityData.get(MOUTH_DEBUG_SIDE);
    }

    public boolean isPerformingClawAttack() {
        return this.getAttackState() == ATTACK_CLAWS;
    }

    public float getClawAttackTime(float partialTick) {
        return this.attackTicks + partialTick;
    }

    public void setMouthDebugEnabled(boolean enabled) {
        if (enabled) {
            this.stopGroundWave();
            this.pendingHowlHits.clear();
            this.pendingGlacialSlamCasts.clear();
            this.resetIceOrbRetreat();
            this.resetGlacialSlamRuntime();
            this.entityData.set(PHASE, PHASE_SURFACE);
            this.entityData.set(ATTACK_STATE, ATTACK_IDLE);
            this.noPhysics = false;
            this.setNoGravity(false);
            this.setInvisible(false);
            this.getNavigation().stop();
            this.setDeltaMovement(Vec3.ZERO);
        }
        this.entityData.set(MOUTH_DEBUG, enabled);
        this.setNoAi(enabled);
    }

    public void setMouthDebugOffset(float forward, float up, float side) {
        this.entityData.set(MOUTH_DEBUG_FORWARD, Mth.clamp(forward, -16.0F, 16.0F));
        this.entityData.set(MOUTH_DEBUG_UP, Mth.clamp(up, -16.0F, 16.0F));
        this.entityData.set(MOUTH_DEBUG_SIDE, Mth.clamp(side, -16.0F, 16.0F));
    }

    public Vec3 getMouthDebugPosition() {
        return this.getLocalMouthPosition(
                this.getMouthDebugForward(), this.getMouthDebugUp(), this.getMouthDebugSide());
    }

    private Vec3 getLocalMouthPosition(float forwardOffset, float upOffset, float sideOffset) {
        Vec3 forward = this.getHorizontalForward();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        return new Vec3(this.getX(), this.getY() + upOffset, this.getZ())
                .add(forward.scale(forwardOffset))
                .add(right.scale(sideOffset));
    }

    private void spawnMouthDebugMarker() {
        if (!(this.level() instanceof ServerLevel level)) return;
        Vec3 marker = this.getMouthDebugPosition();
        level.sendParticles(new DustColorTransitionOptions(0x29E7FF, 0xFFFFFF, 1.5F),
                marker.x, marker.y, marker.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        if ((this.tickCount & 1) == 0) {
            level.sendParticles(ParticleTypes.END_ROD, marker.x, marker.y, marker.z,
                    1, 0.025D, 0.025D, 0.025D, 0.0D);
        }
    }

    private boolean isFacingTarget(LivingEntity target, float maximumAngle) {
        Vec3 forward = this.getHorizontalForward();
        Vec3 offset = target.position().subtract(this.position());
        Vec3 direction = new Vec3(offset.x, 0.0D, offset.z);
        if (forward.horizontalDistanceSqr() < 1.0E-4D || direction.horizontalDistanceSqr() < 1.0E-4D) {
            return true;
        }
        return forward.dot(direction.normalize()) >= Mth.cos(maximumAngle * Mth.DEG_TO_RAD);
    }

    private Vec3 rotateProjectileYaw(Vec3 direction, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(direction.x * cos - direction.z * sin, direction.y,
                direction.x * sin + direction.z * cos).normalize();
    }

    private Vec3 findSpikeSurfacePosition(Vec3 desiredPosition) {
        BlockPos center = BlockPos.containing(desiredPosition);
        for (int distance = 0; distance <= 5; distance++) {
            int[] offsets = distance == 0 ? new int[] {0} : new int[] {-distance, distance};
            for (int offset : offsets) {
                BlockPos support = new BlockPos(center.getX(), center.getY() + offset - 1, center.getZ());
                net.minecraft.world.phys.shapes.VoxelShape supportShape = this.level().getBlockState(support)
                        .getCollisionShape(this.level(), support);
                if (supportShape.isEmpty()) continue;
                double surfaceY = support.getY() + supportShape.max(net.minecraft.core.Direction.Axis.Y);
                Vec3 candidate = new Vec3(desiredPosition.x, surfaceY, desiredPosition.z);
                AABB spikeSpace = new AABB(
                        candidate.x - 0.85D, candidate.y + 0.02D, candidate.z - 0.85D,
                        candidate.x + 0.85D, candidate.y + 3.2D, candidate.z + 0.85D);
                if (this.level().noBlockCollision(this, spikeSpace)) return candidate;
            }
        }
        return null;
    }

    private Vec3 findTacticalRetreatLanding(LivingEntity target, double minimumDistance, double maximumDistance) {
        Vec3 away = this.horizontalDirection(this.position().subtract(target.position()));
        if (away.lengthSqr() < 1.0E-4D) away = this.getHorizontalForward().scale(-1.0D);
        if (away.lengthSqr() < 1.0E-4D) return null;

        double[] angles = {0.0D, 25.0D, -25.0D, 50.0D, -50.0D};
        for (double distance = maximumDistance; distance >= minimumDistance; distance -= 1.0D) {
            for (double angle : angles) {
                Vec3 direction = this.rotateHorizontal(away, angle * Mth.DEG_TO_RAD);
                Vec3 desired = this.position().add(direction.scale(distance));
                Vec3 landing = this.findSurfacePosition(new Vec3(desired.x, this.getY(), desired.z));
                if (landing != null && Math.abs(landing.y - this.getY()) <= 2.5D
                        && this.isSafeGlacialSlamLanding(landing) && this.isGlacialSlamArcClear(landing)) {
                    return landing;
                }
            }
        }
        return null;
    }

    private boolean isSafeGlacialSlamLanding(Vec3 candidate) {
        AABB landingBox = this.getBoundingBox().move(candidate.subtract(this.position()));
        AABB visualClearance = new AABB(
                landingBox.minX - 0.2D, landingBox.minY, landingBox.minZ - 0.2D,
                landingBox.maxX + 0.2D, landingBox.minY + 4.2D, landingBox.maxZ + 0.2D);
        if (!this.level().noCollision(this, visualClearance)) return false;

        double[] footprintOffsets = {-0.72D, 0.0D, 0.72D};
        for (double offsetX : footprintOffsets) {
            for (double offsetZ : footprintOffsets) {
                BlockPos feet = BlockPos.containing(candidate.x + offsetX, candidate.y + 0.1D, candidate.z + offsetZ);
                BlockPos support = BlockPos.containing(candidate.x + offsetX, candidate.y - 0.1D, candidate.z + offsetZ);
                BlockState supportState = this.level().getBlockState(support);
                if (!this.level().getFluidState(feet).isEmpty() || !this.level().getFluidState(support).isEmpty()
                        || supportState.getCollisionShape(this.level(), support).isEmpty()
                        || this.isHazardousLandingBlock(supportState)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isHazardousLandingBlock(BlockState state) {
        return state.is(Blocks.CACTUS) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.LAVA);
    }

    private boolean isGlacialSlamArcClear(Vec3 landing) {
        Vec3 start = this.position();
        double peakHeight = 3.0D + Math.max(0.0D, landing.y - start.y) * 0.35D;
        for (int sample = 1; sample < 10; sample++) {
            double progress = sample / 10.0D;
            Vec3 position = start.lerp(landing, progress)
                    .add(0.0D, 4.0D * peakHeight * progress * (1.0D - progress), 0.0D);
            AABB movedBox = this.getBoundingBox().move(position.subtract(start));
            AABB sampleBox = new AABB(
                    movedBox.minX - 0.08D, movedBox.minY + 0.02D, movedBox.minZ - 0.08D,
                    movedBox.maxX + 0.08D, movedBox.maxY + 0.08D, movedBox.maxZ + 0.08D);
            if (!this.level().noCollision(this, sampleBox)) return false;
        }
        return true;
    }

    private void clearCombatForDeath() {
        if (this.getAttackState() != ATTACK_IDLE) this.entityData.set(ATTACK_STATE, ATTACK_IDLE);
        if (this.getPhase() != PHASE_SURFACE) this.entityData.set(PHASE, PHASE_SURFACE);
        this.stopGroundWave();
        this.noPhysics = false;
        this.setNoGravity(false);
        this.setInvisible(false);
        this.setDeltaMovement(Vec3.ZERO);
        this.pendingHowlHits.clear();
        this.pendingGlacialSlamCasts.clear();
        this.resetIceOrbRetreat();
        this.resetGlacialSlamRuntime();
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || FreezeManager.isFrozen(this);
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

    public int getEncounterState() {
        return this.entityData.get(ENCOUNTER_STATE);
    }

    public int getWakeTicks() {
        return this.entityData.get(WAKE_TICKS);
    }

    public boolean isDormant() {
        return this.getEncounterState() == ENCOUNTER_DORMANT;
    }

    public boolean isWaking() {
        return this.getEncounterState() == ENCOUNTER_WAKING;
    }

    public boolean isEncounterActive() {
        return this.getEncounterState() == ENCOUNTER_ACTIVE;
    }

    public boolean isEncounterLocked() {
        return this.isDormant() || this.isWaking();
    }

    public void setDormant() {
        if (this.isDeadOrDying()) return;
        this.entityData.set(ENCOUNTER_STATE, ENCOUNTER_DORMANT);
        this.entityData.set(WAKE_TICKS, 0);
        this.applyEncounterLock();
    }

    public void setEncounterSeal(BlockPos sealPos) {
        this.encounterSealPos = sealPos.immutable();
    }

    public boolean beginWakeSequence() {
        if (!this.isDormant() || this.isDeadOrDying()) return false;
        this.entityData.set(ENCOUNTER_STATE, ENCOUNTER_WAKING);
        this.entityData.set(WAKE_TICKS, 0);
        this.applyEncounterLock();
        this.playSound(SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.8F, 0.55F);
        return true;
    }

    private void applyEncounterLock() {
        boolean locked = this.isEncounterLocked();
        if (locked) {
            this.stopGroundWave();
            this.pendingHowlHits.clear();
            this.pendingGlacialSlamCasts.clear();
            this.resetIceOrbRetreat();
            this.resetGlacialSlamRuntime();
            this.entityData.set(PHASE, PHASE_SURFACE);
            this.entityData.set(ATTACK_STATE, ATTACK_IDLE);
            this.setTarget(null);
            this.getNavigation().stop();
            this.setDeltaMovement(Vec3.ZERO);
        }
        this.setNoAi(locked);
        this.bossEvent.setVisible(!locked && this.isEncounterActive());
    }

    private void tickEncounterLock() {
        this.setTarget(null);
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.bossEvent.setVisible(false);
        if (!this.level().isClientSide() && this.isWaking()) {
            int ticks = this.getWakeTicks() + 1;
            this.entityData.set(WAKE_TICKS, ticks);
            if (ticks == WAKE_DURATION - 20) {
                this.playSound(SoundEvents.RAVAGER_ROAR, 2.0F, 0.65F);
            }
            if (ticks >= WAKE_DURATION) {
                this.entityData.set(ENCOUNTER_STATE, ENCOUNTER_ACTIVE);
                this.entityData.set(WAKE_TICKS, WAKE_DURATION);
                this.setNoAi(false);
                this.bossEvent.setVisible(true);
                this.burrowCooldown = 80;
                this.skillSelectionDelay = 30;
            }
        }
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
        if (this.isEncounterLocked()) {
            return false;
        }
        if ((this.getPhase() == PHASE_BURROWING || this.isUnderground())
                && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void die(DamageSource source) {
        this.entityData.set(ENCOUNTER_STATE, ENCOUNTER_DEFEATED);
        this.bossEvent.setVisible(false);
        if (this.encounterSealPos != null && this.level().getBlockEntity(this.encounterSealPos)
                instanceof FenrirSealBlockEntity seal) {
            seal.markFenrirDefeated();
        }
        super.die(source);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
        this.bossEvent.setVisible(this.isEncounterActive());
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(Component.translatable("boss.wotn.fenrir"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<FenrirEntity>("Base", 2, state -> {
            if (state.getDataOrDefault(DataTickets.IS_DEAD_OR_DYING, false)) {
                return state.setAndContinue(DEATH_ANIMATION);
            }
            state.setControllerSpeed(FreezeManager.isFrozen(this) ? 0.0F : 1.0F);
            if (this.isDormant()) {
                return state.setAndContinue(SLEEP_ANIMATION);
            }
            if (this.isWaking()) {
                return state.setAndContinue(WAKE_ANIMATION);
            }
            if (this.isMouthDebugEnabled()) {
                return state.setAndContinue(MOUTH_DEBUG_ANIMATION);
            }
            if (this.getAttackState() != ATTACK_IDLE) {
                return switch (this.getAttackState()) {
                    case ATTACK_BITE -> state.setAndContinue(BITE_ANIMATION);
                    case ATTACK_CLAWS -> state.setAndContinue(CLAWS_ANIMATION);
                    case ATTACK_ICE_ORB -> state.setAndContinue(this.entityData.get(ICE_ORB_RETREAT_ANIMATION)
                            ? ICE_ORB_RETREAT_ANIMATION_SEQUENCE : ICE_ORB_ANIMATION);
                    case ATTACK_FROST_HOWL -> state.setAndContinue(FROST_HOWL_ANIMATION);
                    case ATTACK_PHASE_TRANSITION -> state.setAndContinue(PHASE_TRANSITION_ANIMATION);
                    case ATTACK_GLACIAL_SLAM -> this.playSynchronizedGlacialSlamAnimation(state);
                    default -> state.setAndContinue(IDLE_ANIMATION);
                };
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

    private com.geckolib.animation.object.PlayState playSynchronizedGlacialSlamAnimation(
            com.geckolib.animation.state.AnimationTest<FenrirEntity> state) {
        RawAnimation animation = switch (this.getGlacialSlamPhase()) {
            case GLACIAL_SLAM_LEAP -> GLACIAL_LEAP_ANIMATION;
            case GLACIAL_SLAM_IMPACT -> GLACIAL_SLAM_ANIMATION;
            case GLACIAL_SLAM_RECOVERY -> GLACIAL_SLAM_RECOVERY_ANIMATION;
            default -> GLACIAL_LEAP_PREPARE_ANIMATION;
        };
        boolean animationChanged = !state.isCurrentAnimation(animation);
        com.geckolib.animation.object.PlayState playState = state.setAndContinue(animation);
        if (animationChanged) {
            this.glacialSlamAnimationSeekPending = true;
            return playState;
        }
        if (this.glacialSlamAnimationSeekPending && state.controller().getCurrentAnimationPoint() != null) {
            long elapsedTicks = Math.max(0L,
                    this.level().getGameTime() - this.entityData.get(GLACIAL_SLAM_PHASE_START_TICK));
            double animationTime = Math.max(0.0D, elapsedTicks - 2.0D) / 20.0D;
            if (this.getGlacialSlamPhase() == GLACIAL_SLAM_LEAP) {
                animationTime %= 0.6D;
            }
            state.controller().setAnimationTime(animationTime);
            this.glacialSlamAnimationSeekPending = false;
        }
        return playState;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isEncounterLocked()) return null;
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

    private static final class FenrirPhaseTransitionGoal extends Goal {
        private final FenrirEntity fenrir;

        private FenrirPhaseTransitionGoal(FenrirEntity fenrir) {
            this.fenrir = fenrir;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return this.fenrir.isSurfaceIdle() && this.fenrir.shouldEnterPhaseTwo();
        }

        @Override
        public boolean canContinueToUse() {
            return this.fenrir.getAttackState() == ATTACK_PHASE_TRANSITION;
        }

        @Override
        public void start() {
            this.fenrir.startSurfaceAttack(ATTACK_PHASE_TRANSITION);
        }
    }

    private static final class FenrirActionSelectionGoal extends Goal {
        private final FenrirEntity fenrir;
        private FenrirAction selectedAction;

        private FenrirActionSelectionGoal(FenrirEntity fenrir) {
            this.fenrir = fenrir;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            this.selectedAction = null;
            LivingEntity target = this.fenrir.getTarget();
            if (target == null || !target.isAlive() || !this.fenrir.isSurfaceIdle()
                    || this.fenrir.shouldEnterPhaseTwo() || this.fenrir.globalSkillCooldown > 0) {
                return false;
            }

            double distance = Math.sqrt(this.fenrir.distanceToSqr(target));
            boolean phaseTwo = this.fenrir.entityData.get(PHASE_TWO);
            this.selectedAction = this.drawNextAction(target, distance, phaseTwo);
            return this.selectedAction != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.selectedAction == null) return false;
            return this.selectedAction == FenrirAction.BURROW
                    ? this.fenrir.isUsingBurrowSkill()
                    : this.fenrir.getAttackState() == this.selectedAction.attackState;
        }

        @Override
        public void start() {
            LivingEntity target = this.fenrir.getTarget();
            if (this.selectedAction == null || target == null || !target.isAlive()) return;

            this.fenrir.lastCombatAction = this.selectedAction;
            if (this.selectedAction == FenrirAction.BURROW) {
                this.fenrir.skillSelectionDelay = 20;
                this.fenrir.startBurrowSkill();
            } else if (this.selectedAction == FenrirAction.GLACIAL_SLAM) {
                this.fenrir.startGlacialSlam(target);
            } else {
                this.fenrir.startSurfaceAttack(this.selectedAction.attackState);
            }
        }

        @Override
        public void stop() {
            this.selectedAction = null;
        }

        private FenrirAction drawNextAction(LivingEntity target, double distance, boolean phaseTwo) {
            if (this.fenrir.combatActionDeck.isEmpty()) this.refillDeck(phaseTwo);
            int attempts = this.fenrir.combatActionDeck.size();
            FenrirAction repeatedCandidate = null;
            for (int attempt = 0; attempt < attempts; attempt++) {
                FenrirAction action = this.fenrir.combatActionDeck.remove(0);
                if (action == this.fenrir.lastCombatAction) {
                    if (this.canExecute(action, target, distance, phaseTwo)) repeatedCandidate = action;
                    this.fenrir.combatActionDeck.add(action);
                    continue;
                }
                if (this.canExecute(action, target, distance, phaseTwo)) return action;
                this.fenrir.combatActionDeck.add(action);
            }
            if (repeatedCandidate != null) {
                this.fenrir.combatActionDeck.remove(repeatedCandidate);
                return repeatedCandidate;
            }
            return null;
        }

        private void refillDeck(boolean phaseTwo) {
            this.fenrir.combatActionDeck.clear();
            if (phaseTwo) {
                this.addCopies(FenrirAction.BITE, 5);
                this.addCopies(FenrirAction.CLAWS, 4);
                this.addCopies(FenrirAction.ICE_ORB, 3);
                this.addCopies(FenrirAction.FROST_HOWL, 1);
                this.addCopies(FenrirAction.GLACIAL_SLAM, 1);
                this.addCopies(FenrirAction.BURROW, 1);
            } else {
                this.addCopies(FenrirAction.BITE, 6);
                this.addCopies(FenrirAction.CLAWS, 4);
                this.addCopies(FenrirAction.ICE_ORB, 3);
                this.addCopies(FenrirAction.GLACIAL_SLAM, 1);
                this.addCopies(FenrirAction.BURROW, 1);
            }
            if (this.fenrir.combatActionDeck.size() != COMBAT_CYCLE_SIZE) {
                throw new IllegalStateException("Fenrir combat cycle must contain " + COMBAT_CYCLE_SIZE + " actions");
            }
            Collections.shuffle(this.fenrir.combatActionDeck, new java.util.Random(this.fenrir.random.nextLong()));
            if (this.fenrir.combatActionDeck.get(0) == this.fenrir.lastCombatAction) {
                for (int index = 1; index < this.fenrir.combatActionDeck.size(); index++) {
                    if (this.fenrir.combatActionDeck.get(index) != this.fenrir.lastCombatAction) {
                        Collections.swap(this.fenrir.combatActionDeck, 0, index);
                        break;
                    }
                }
            }
        }

        private void addCopies(FenrirAction action, int count) {
            for (int index = 0; index < count; index++) {
                this.fenrir.combatActionDeck.add(action);
            }
        }

        private boolean canExecute(FenrirAction action, LivingEntity target, double distance, boolean phaseTwo) {
            return switch (action) {
                case BITE, CLAWS -> distance <= 6.5D;
                case ICE_ORB -> true;
                case FROST_HOWL -> phaseTwo;
                case GLACIAL_SLAM -> this.fenrir.onGround();
                case BURROW -> this.fenrir.onGround();
            };
        }
    }

    private static final class FenrirAdvanceGoal extends Goal {
        private final FenrirEntity fenrir;
        private int pathRecalculationDelay;

        private FenrirAdvanceGoal(FenrirEntity fenrir) {
            this.fenrir = fenrir;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.fenrir.getTarget();
            return target != null && target.isAlive() && this.fenrir.isSurfaceIdle()
                    && !this.fenrir.shouldEnterPhaseTwo();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            this.fenrir.getNavigation().stop();
            this.pathRecalculationDelay = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = this.fenrir.getTarget();
            if (target == null) return;
            this.fenrir.getLookControl().setLookAt(target, 25.0F, 25.0F);
            double distanceSqr = this.fenrir.distanceToSqr(target);
            if (this.fenrir.globalSkillCooldown <= 0 && this.fenrir.iceOrbCooldown <= 0
                    && distanceSqr >= Mth.square(6.0D) && distanceSqr <= Mth.square(34.0D)
                    && !this.fenrir.isFacingTarget(target, 12.0F)) {
                this.fenrir.getNavigation().stop();
                this.fenrir.faceTargetHorizontally(target, 12.0F);
                this.pathRecalculationDelay = 0;
                return;
            }
            if (distanceSqr <= Mth.square(SURFACE_HOLD_DISTANCE)) {
                this.fenrir.getNavigation().stop();
                this.fenrir.faceTargetHorizontally(target, 12.0F);
                this.pathRecalculationDelay = 0;
                return;
            }
            if (--this.pathRecalculationDelay <= 0 || this.fenrir.getNavigation().isDone()) {
                this.pathRecalculationDelay = 8 + this.fenrir.getRandom().nextInt(5);
                this.fenrir.getNavigation().moveTo(target,
                        this.fenrir.entityData.get(PHASE_TWO) ? 1.28D : 1.15D);
            }
        }
    }

    private enum FenrirAction {
        BITE(ATTACK_BITE),
        CLAWS(ATTACK_CLAWS),
        ICE_ORB(ATTACK_ICE_ORB),
        FROST_HOWL(ATTACK_FROST_HOWL),
        GLACIAL_SLAM(ATTACK_GLACIAL_SLAM),
        BURROW(ATTACK_IDLE);

        private final int attackState;

        FenrirAction(int attackState) {
            this.attackState = attackState;
        }
    }

    private static final class FenrirPart extends PartEntity<FenrirEntity> {
        private final String name;
        private final EntityDimensions dimensions;

        private FenrirPart(FenrirEntity parent, String name, float width, float height) {
            super(parent);
            this.name = name;
            this.dimensions = EntityDimensions.scalable(width, height);
            this.refreshDimensions();
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
        }

        @Override
        protected void readAdditionalSaveData(ValueInput input) {
        }

        @Override
        protected void addAdditionalSaveData(ValueOutput output) {
        }

        @Override
        public boolean isPickable() {
            return this.getParent().areMultipartHitboxesActive();
        }

        @Override
        public ItemStack getPickResult() {
            return this.getParent().getPickResult();
        }

        @Override
        public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
            return !this.getParent().areMultipartHitboxesActive() || this.isInvulnerableToBase(source)
                    ? false
                    : this.getParent().hurtFromPart(level, source, amount);
        }

        @Override
        public void push(double x, double y, double z) {
            this.getParent().push(x, y, z);
        }

        @Override
        public boolean ignoreExplosion(net.minecraft.world.level.Explosion explosion) {
            return true;
        }

        @Override
        public boolean is(Entity entity) {
            return this == entity || this.getParent() == entity;
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            return this.dimensions;
        }

        @Override
        public boolean shouldBeSaved() {
            return false;
        }

        @Override
        public String toString() {
            return super.toString() + "[part=" + this.name + "]";
        }
    }

    private record PendingHowlHit(int triggerTick, AABB area) {}
    private record GlacialSlamPlacement(Vec3 position, int row, int delay, float scale) {}
    private record PendingGlacialSlamRow(int triggerTick, List<AABB> areas) {}
    private record PendingGlacialSlamCast(ArrayList<PendingGlacialSlamRow> rows,
            Set<UUID> hitTargets, Vec3 direction) {}
}
