package com.somake.wotn.block.entity;

import java.util.List;
import java.util.UUID;

import com.somake.wotn.entity.FenrirEntity;
import com.somake.wotn.registry.ModBlockEntities;
import com.somake.wotn.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class FenrirSealBlockEntity extends BlockEntity {
    private static final int SEARCH_RADIUS = 96;
    private boolean hasCore;
    private boolean completed;
    private UUID fenrirUuid;
    private int searchCooldown;

    public FenrirSealBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FENRIR_SEAL.get(), pos, state);
    }

    public boolean insertCore(Player player, ItemStack stack) {
        if (level == null || level.isClientSide() || hasCore || completed || !stack.is(ModItems.GOLEM_CORE.get())) {
            return false;
        }
        hasCore = true;
        if (!player.hasInfiniteMaterials()) stack.shrink(1);
        setChangedAndSync();
        if (level instanceof ServerLevel serverLevel) awakenFenrir(serverLevel);
        return true;
    }

    public boolean tryReturnCore(Player player) {
        if (level == null || level.isClientSide() || !hasCore || !completed) return false;
        ItemStack core = new ItemStack(ModItems.GOLEM_CORE.get());
        if (!player.getInventory().add(core)) player.drop(core, false);
        hasCore = false;
        setChangedAndSync();
        return true;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, FenrirSealBlockEntity seal) {
        if (seal.completed) return;
        if (seal.searchCooldown-- > 0) return;
        seal.searchCooldown = seal.fenrirUuid == null ? 10 : 2;
        FenrirEntity fenrir = seal.findFenrir(level);
        if (fenrir == null) return;
        if (!fenrir.getUUID().equals(seal.fenrirUuid)) {
            seal.fenrirUuid = fenrir.getUUID();
            fenrir.setEncounterSeal(pos);
            seal.setChangedAndSync();
        }
        if (!seal.hasCore) return;
        if (fenrir.isDormant()) fenrir.beginWakeSequence();
        if (fenrir.isWaking()) seal.spawnWakeParticles(level, fenrir);
        if (fenrir.isDeadOrDying() || fenrir.isRemoved()) {
            seal.completed = true;
            seal.setChangedAndSync();
        }
    }

    private void awakenFenrir(ServerLevel level) {
        FenrirEntity fenrir = findFenrir(level);
        if (fenrir == null) return;
        fenrirUuid = fenrir.getUUID();
        fenrir.setEncounterSeal(worldPosition);
        fenrir.beginWakeSequence();
        setChangedAndSync();
    }

    private FenrirEntity findFenrir(ServerLevel level) {
        if (fenrirUuid != null && level.getEntity(fenrirUuid) instanceof FenrirEntity fenrir) return fenrir;
        AABB area = new AABB(worldPosition).inflate(SEARCH_RADIUS);
        List<FenrirEntity> fenrirs = level.getEntitiesOfClass(FenrirEntity.class, area);
        return fenrirs.stream().min(java.util.Comparator.comparingDouble(
                fenrir -> fenrir.distanceToSqr(worldPosition.getCenter()))).orElse(null);
    }

    private void spawnWakeParticles(ServerLevel level, FenrirEntity fenrir) {
        Vec3 start = worldPosition.getCenter();
        Vec3 end = fenrir.position().add(0.0D, 1.25D, 0.0D);
        double progress = level.getRandom().nextDouble();
        Vec3 point = start.lerp(end, progress);
        level.sendParticles(new DustColorTransitionOptions(0xD9A928, 0xFFF0A0, 0.85F),
                point.x, point.y, point.z, 1, 0.035D, 0.035D, 0.035D, 0.0D);
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public void markFenrirDefeated() {
        if (!hasCore || completed) return;
        completed = true;
        setChangedAndSync();
    }

    public UUID getFenrirUuid() {
        return fenrirUuid;
    }

    public boolean isCompleted() {
        return completed;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("HasCore", hasCore);
        output.putBoolean("Completed", completed);
        if (fenrirUuid != null) output.store("FenrirUuid", UUIDUtil.CODEC, fenrirUuid);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hasCore = input.getBooleanOr("HasCore", false);
        completed = input.getBooleanOr("Completed", false);
        fenrirUuid = input.read("FenrirUuid", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
