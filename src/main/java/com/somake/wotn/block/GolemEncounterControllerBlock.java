package com.somake.wotn.block;

import com.mojang.serialization.MapCodec;
import com.somake.wotn.block.entity.GolemEncounterControllerBlockEntity;
import com.somake.wotn.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class GolemEncounterControllerBlock extends BaseEntityBlock {
    public static final MapCodec<GolemEncounterControllerBlock> CODEC = simpleCodec(GolemEncounterControllerBlock::new);

    public GolemEncounterControllerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<GolemEncounterControllerBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GolemEncounterControllerBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.GOLEM_ENCOUNTER_CONTROLLER.get(),
                (serverLevel, pos, blockState, controller) -> GolemEncounterControllerBlockEntity.serverTick(
                        (ServerLevel) serverLevel, pos, blockState, controller));
    }
}
