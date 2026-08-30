package com.somake.wotn.block;

import com.mojang.serialization.MapCodec;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class AlchemistCauldronBlock extends Block {
    public static final MapCodec<AlchemistCauldronBlock> CODEC = simpleCodec(AlchemistCauldronBlock::new);
    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    private static final SourceBox[] SOURCE_BOXES = {
            new SourceBox(-16, -16, -6.5, 32, -9, 0.5),
            new SourceBox(-16, -16, 4.5, 32, -9, 11.5),
            new SourceBox(-16, -16, 15.5, 32, -9, 22.5),
            new SourceBox(-6.5, -16, -16, 0.5, -9, 32),
            new SourceBox(4.5, -16, -16, 11.5, -9, 32),
            new SourceBox(15.5, -16, -16, 22.5, -9, 32),
            new SourceBox(24, 16, -8, 26, 20, 24),
            new SourceBox(-13, -10, -13, 29, 16, 29),
            new SourceBox(-10, 16, -10, 26, 20, -8),
            new SourceBox(-10, 16, -8, -8, 20, 24),
            new SourceBox(-10, 16, 24, 26, 20, 26)
    };
    private static final Map<Direction, Map<Part, VoxelShape>> PART_SHAPES = createPartShapes();

    private final Set<BlockPos> tearingDownControllers = new HashSet<>();

    public AlchemistCauldronBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, Part.CENTER));
    }

    @Override
    public MapCodec<AlchemistCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return canPlaceStructure(context.getLevel(), context.getClickedPos(), context, facing)
                ? defaultBlockState().setValue(FACING, facing).setValue(PART, Part.CENTER)
                : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            @Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide()) return;
        BlockState controllerState = state.setValue(PART, Part.CENTER);
        if (state != controllerState) level.setBlock(pos, controllerState, Block.UPDATE_CLIENTS);
        for (Part part : Part.values()) {
            if (part == Part.CENTER) continue;
            level.setBlock(pos.offset(part.x, part.y, part.z),
                    controllerState.setValue(PART, part), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        for (Part part : Part.values()) level.updateNeighborsAt(pos.offset(part.x, part.y, part.z), this);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return partShape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return partShape(state);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART) == Part.CENTER ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathType) {
        return false;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level,
            BlockPos pos, boolean movedByPiston) {
        BlockPos controllerPos = controllerPos(pos, state.getValue(PART));
        if (!tearingDownControllers.contains(controllerPos)) {
            removeStructure(level, controllerPos, pos, state.getValue(FACING));
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            removeStructure(level, controllerPos(pos, state.getValue(PART)), pos, state.getValue(FACING));
        }
        return super.playerWillDestroy(level, pos, state.setValue(PART, Part.CENTER), player);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state,
            boolean includeData) {
        return new ItemStack(asItem());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
                .setValue(PART, state.getValue(PART).rotate(rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)))
                .setValue(PART, state.getValue(PART).mirror(mirror));
    }

    private void removeStructure(Level level, BlockPos controllerPos, BlockPos excludedPos, Direction facing) {
        tearingDownControllers.add(controllerPos.immutable());
        try {
            for (Part part : Part.values()) {
                BlockPos partPos = controllerPos.offset(part.x, part.y, part.z);
                if (partPos.equals(excludedPos)) continue;
                BlockState partState = level.getBlockState(partPos);
                if (partState.is(this) && partState.getValue(PART) == part
                        && partState.getValue(FACING) == facing) {
                    level.removeBlock(partPos, false);
                }
            }
        } finally {
            tearingDownControllers.remove(controllerPos);
        }
    }

    private boolean canPlaceStructure(Level level, BlockPos controllerPos,
            BlockPlaceContext context, Direction facing) {
        for (Part part : Part.values()) {
            BlockPos partPos = controllerPos.offset(part.x, part.y, part.z);
            if (!level.isInWorldBounds(partPos) || !level.getWorldBorder().isWithinBounds(partPos)) return false;
            if (!level.getBlockState(partPos).canBeReplaced(context)) return false;
            BlockState partState = defaultBlockState().setValue(FACING, facing).setValue(PART, part);
            VoxelShape shape = partShape(partState);
            if (!shape.isEmpty() && !level.isUnobstructed(partState, partPos, CollisionContext.empty())) return false;
        }
        return true;
    }

    private static BlockPos controllerPos(BlockPos partPos, Part part) {
        return partPos.offset(-part.x, -part.y, -part.z);
    }

    private boolean isCompleteStructure(Level level, BlockPos pos, BlockState state) {
        BlockPos controllerPos = controllerPos(pos, state.getValue(PART));
        Direction facing = state.getValue(FACING);
        for (Part part : Part.values()) {
            BlockState partState = level.getBlockState(controllerPos.offset(part.x, part.y, part.z));
            if (!partState.is(this) || partState.getValue(PART) != part
                    || partState.getValue(FACING) != facing) return false;
        }
        return true;
    }

    private static VoxelShape partShape(BlockState state) {
        return PART_SHAPES.get(state.getValue(FACING)).get(state.getValue(PART));
    }

    private static Map<Direction, Map<Part, VoxelShape>> createPartShapes() {
        Map<Direction, Map<Part, VoxelShape>> byFacing = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            Map<Part, VoxelShape> shapes = new EnumMap<>(Part.class);
            int turns = switch (facing) {
                case EAST -> 1;
                case SOUTH -> 2;
                case WEST -> 3;
                default -> 0;
            };
            for (Part part : Part.values()) {
                double cellX = part.x * 16.0D;
                double cellY = part.y * 16.0D;
                double cellZ = part.z * 16.0D;
                VoxelShape result = Shapes.empty();
                for (SourceBox sourceBox : SOURCE_BOXES) {
                    SourceBox box = sourceBox.rotateClockwise(turns);
                    double minX = Math.max(box.minX, cellX);
                    double minY = Math.max(box.minY, cellY);
                    double minZ = Math.max(box.minZ, cellZ);
                    double maxX = Math.min(box.maxX, cellX + 16.0D);
                    double maxY = Math.min(box.maxY, cellY + 16.0D);
                    double maxZ = Math.min(box.maxZ, cellZ + 16.0D);
                    if (minX >= maxX || minY >= maxY || minZ >= maxZ) continue;
                    result = Shapes.joinUnoptimized(result, Block.box(
                            minX - cellX, minY - cellY, minZ - cellZ,
                            maxX - cellX, maxY - cellY, maxZ - cellZ), BooleanOp.OR);
                }
                shapes.put(part, result.optimize());
            }
            byFacing.put(facing, Map.copyOf(shapes));
        }
        return Map.copyOf(byFacing);
    }

    private record SourceBox(double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        private SourceBox rotateClockwise(int turns) {
            SourceBox box = this;
            for (int turn = 0; turn < turns; turn++) {
                box = new SourceBox(16.0D - box.maxZ, box.minY, box.minX,
                        16.0D - box.minZ, box.maxY, box.maxX);
            }
            return box;
        }
    }

    public enum Part implements StringRepresentable {
        LOWER_NORTH_WEST(-1, -1, -1), LOWER_NORTH(0, -1, -1), LOWER_NORTH_EAST(1, -1, -1),
        LOWER_WEST(-1, -1, 0), LOWER_CENTER(0, -1, 0), LOWER_EAST(1, -1, 0),
        LOWER_SOUTH_WEST(-1, -1, 1), LOWER_SOUTH(0, -1, 1), LOWER_SOUTH_EAST(1, -1, 1),
        NORTH_WEST(-1, 0, -1), NORTH(0, 0, -1), NORTH_EAST(1, 0, -1),
        WEST(-1, 0, 0), CENTER(0, 0, 0), EAST(1, 0, 0),
        SOUTH_WEST(-1, 0, 1), SOUTH(0, 0, 1), SOUTH_EAST(1, 0, 1),
        UPPER_NORTH_WEST(-1, 1, -1), UPPER_NORTH(0, 1, -1), UPPER_NORTH_EAST(1, 1, -1),
        UPPER_WEST(-1, 1, 0), UPPER_EAST(1, 1, 0),
        UPPER_SOUTH_WEST(-1, 1, 1), UPPER_SOUTH(0, 1, 1), UPPER_SOUTH_EAST(1, 1, 1);

        private final int x;
        private final int y;
        private final int z;

        Part(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        private Part rotate(Rotation rotation) {
            return switch (rotation) {
                case CLOCKWISE_90 -> fromOffset(-z, y, x);
                case CLOCKWISE_180 -> fromOffset(-x, y, -z);
                case COUNTERCLOCKWISE_90 -> fromOffset(z, y, -x);
                default -> this;
            };
        }

        private Part mirror(Mirror mirror) {
            return switch (mirror) {
                case LEFT_RIGHT -> fromOffset(x, y, -z);
                case FRONT_BACK -> fromOffset(-x, y, z);
                default -> this;
            };
        }

        private static Part fromOffset(int x, int y, int z) {
            for (Part part : values()) {
                if (part.x == x && part.y == y && part.z == z) return part;
            }
            throw new IllegalArgumentException("Unsupported cauldron part offset: " + x + "," + y + "," + z);
        }
    }
}
