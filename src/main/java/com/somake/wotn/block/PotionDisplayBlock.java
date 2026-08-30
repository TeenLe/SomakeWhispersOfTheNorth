package com.somake.wotn.block;

import com.mojang.serialization.MapCodec;
import com.somake.wotn.block.entity.PotionDisplayBlockEntity;

import java.util.List;
import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class PotionDisplayBlock extends BaseEntityBlock {
    public static final MapCodec<PotionDisplayBlock> CODEC = simpleCodec(PotionDisplayBlock::new);
    public static final EnumProperty<PotionFamily> FAMILY = EnumProperty.create("family", PotionFamily.class);
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 1, 3);
    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape TIER_ONE_SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 10.0D, 11.0D);
    private static final VoxelShape TIER_TWO_SHAPE = Block.box(5.5D, 0.0D, 5.5D, 10.5D, 13.5D, 10.5D);
    private static final VoxelShape TIER_THREE_SHAPE = Block.box(2.5D, 0.0D, 2.5D, 13.5D, 15.5D, 13.5D);

    public PotionDisplayBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FAMILY, PotionFamily.NIFLHEIM)
                .setValue(TIER, 1)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<PotionDisplayBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(TIER)) {
            case 2 -> TIER_TWO_SHAPE;
            case 3 -> TIER_THREE_SHAPE;
            default -> TIER_ONE_SHAPE;
        };
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random) {
        return directionToNeighbour == Direction.DOWN && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PotionDisplayBlockEntity(pos, state);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof PotionDisplayBlockEntity display && !display.getPotion().isEmpty()) {
            return List.of(display.getPotion().copyWithCount(1));
        }
        return List.of();
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return level.getBlockEntity(pos) instanceof PotionDisplayBlockEntity display
                ? display.getPotion().copy()
                : ItemStack.EMPTY;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FAMILY, TIER, FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public enum PotionFamily implements StringRepresentable {
        JORMUNGANDR("jormungandr", "jormungandr_venom"),
        FENRIR("fenrir", "fenrir_blood"),
        NIFLHEIM("niflheim", "niflheim_essence"),
        IDUNN("idunn", "idunn_elixir");

        private final String id;
        private final String modelPrefix;

        PotionFamily(String id, String modelPrefix) {
            this.id = id;
            this.modelPrefix = modelPrefix;
        }

        @Override
        public String getSerializedName() {
            return id;
        }

        public String modelPath(int tier) {
            return modelPrefix + "_tier_" + tier;
        }

        public static PotionFamily fromId(String id) {
            String normalized = id.toLowerCase(Locale.ROOT);
            for (PotionFamily family : values()) {
                if (family.id.equals(normalized)) return family;
            }
            throw new IllegalArgumentException("Unknown potion family: " + id);
        }
    }
}
