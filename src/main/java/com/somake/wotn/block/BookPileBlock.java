package com.somake.wotn.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BookPileBlock extends FlammableBlocks.Basic {
    public static final MapCodec<BookPileBlock> CODEC = simpleCodec(BookPileBlock::new);
    public static final IntegerProperty FORMAT = IntegerProperty.create("format", 1, 5);

    private static final VoxelShape FORMAT_ONE = Shapes.or(
            Block.box(1.817D, 0.0D, 0.464D, 14.183D, 4.0D, 15.536D),
            Block.box(0.464D, 4.0D, 1.817D, 15.536D, 8.0D, 14.183D));
    private static final VoxelShape FORMAT_TWO = Block.box(4.0D, 0.0D, 1.5D, 12.0D, 4.0D, 14.5D);
    private static final VoxelShape FORMAT_THREE = Block.box(
            0.575D, 0.0D, 0.575D, 15.425D, 4.0D, 15.425D);
    private static final VoxelShape FORMAT_FOUR = Shapes.or(
            Block.box(0.575D, 0.0D, 0.575D, 15.425D, 4.0D, 15.425D),
            Block.box(4.0D, 4.0D, 1.5D, 12.0D, 8.0D, 14.5D),
            Block.box(0.575D, 8.0D, 0.575D, 15.425D, 12.0D, 15.425D));
    private static final VoxelShape FORMAT_FIVE = Shapes.or(
            Block.box(1.0D, 0.0D, 1.5D, 9.0D, 4.0D, 14.5D),
            Block.box(7.257D, 0.0D, 1.5D, 15.743D, 8.443D, 14.5D));

    public BookPileBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMAT, 1));
    }

    @Override
    public MapCodec<BookPileBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FORMAT, 1 + context.getLevel().getRandom().nextInt(5));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return switch (state.getValue(FORMAT)) {
            case 2 -> FORMAT_TWO;
            case 3 -> FORMAT_THREE;
            case 4 -> FORMAT_FOUR;
            case 5 -> FORMAT_FIVE;
            default -> FORMAT_ONE;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMAT);
    }
}
