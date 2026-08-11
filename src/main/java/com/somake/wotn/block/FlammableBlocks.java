package com.somake.wotn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public final class FlammableBlocks {
    private interface Flammable {
        int flammability();

        int fireSpreadSpeed();

        default int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
            return flammability();
        }

        default int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
            return fireSpreadSpeed();
        }
    }

    public static class Basic extends Block implements Flammable {
        public Basic(Properties properties) {
            super(properties);
        }

        @Override public int flammability() { return 20; }
        @Override public int fireSpreadSpeed() { return 5; }
        @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction d) { return flammability(); }
        @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction d) { return fireSpreadSpeed(); }
    }

    public static class Pillar extends RotatedPillarBlock implements Flammable {
        public Pillar(Properties properties) { super(properties); }
        @Override public int flammability() { return 5; }
        @Override public int fireSpreadSpeed() { return 5; }
        @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction d) { return flammability(); }
        @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction d) { return fireSpreadSpeed(); }
    }

    public static class Stairs extends StairBlock implements Flammable {
        public Stairs(BlockState baseState, Properties properties) { super(baseState, properties); }
        @Override public int flammability() { return 20; }
        @Override public int fireSpreadSpeed() { return 5; }
        @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction d) { return flammability(); }
        @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction d) { return fireSpreadSpeed(); }
    }

    public static class Slab extends SlabBlock implements Flammable {
        public Slab(Properties properties) { super(properties); }
        @Override public int flammability() { return 20; }
        @Override public int fireSpreadSpeed() { return 5; }
        @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction d) { return flammability(); }
        @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction d) { return fireSpreadSpeed(); }
    }

    public static class Fence extends FenceBlock implements Flammable {
        public Fence(Properties properties) { super(properties); }
        @Override public int flammability() { return 20; }
        @Override public int fireSpreadSpeed() { return 5; }
        @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction d) { return flammability(); }
        @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction d) { return fireSpreadSpeed(); }
    }

    public static class FenceGate extends FenceGateBlock implements Flammable {
        public FenceGate(WoodType type, Properties properties) { super(type, properties); }
        @Override public int flammability() { return 20; }
        @Override public int fireSpreadSpeed() { return 5; }
        @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction d) { return flammability(); }
        @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction d) { return fireSpreadSpeed(); }
    }

    public static class Door extends DoorBlock implements Flammable {
        public Door(BlockSetType type, Properties properties) { super(type, properties); }
        @Override public int flammability() { return 20; }
        @Override public int fireSpreadSpeed() { return 5; }
        @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction d) { return flammability(); }
        @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction d) { return fireSpreadSpeed(); }
    }

    public static class Trapdoor extends TrapDoorBlock implements Flammable {
        public Trapdoor(BlockSetType type, Properties properties) { super(type, properties); }
        @Override public int flammability() { return 20; }
        @Override public int fireSpreadSpeed() { return 5; }
        @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction d) { return flammability(); }
        @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction d) { return fireSpreadSpeed(); }
    }

    public static class PressurePlate extends PressurePlateBlock implements Flammable {
        public PressurePlate(BlockSetType type, Properties properties) { super(type, properties); }
        @Override public int flammability() { return 20; }
        @Override public int fireSpreadSpeed() { return 5; }
        @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction d) { return flammability(); }
        @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction d) { return fireSpreadSpeed(); }
    }

    public static class Button extends ButtonBlock implements Flammable {
        public Button(BlockSetType type, int ticksToStayPressed, Properties properties) {
            super(type, ticksToStayPressed, properties);
        }
        @Override public int flammability() { return 20; }
        @Override public int fireSpreadSpeed() { return 5; }
        @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction d) { return flammability(); }
        @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction d) { return fireSpreadSpeed(); }
    }

    public static class Leaves extends UntintedParticleLeavesBlock implements Flammable {
        public Leaves(float particleChance, ParticleOptions particle, Properties properties) {
            super(particleChance, particle, properties);
        }
        @Override public int flammability() { return 60; }
        @Override public int fireSpreadSpeed() { return 30; }
        @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction d) { return flammability(); }
        @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction d) { return fireSpreadSpeed(); }
    }

    public static class LeafLitter extends LeafLitterBlock implements Flammable {
        public LeafLitter(Properties properties) { super(properties); }
        @Override public int flammability() { return 100; }
        @Override public int fireSpreadSpeed() { return 60; }
        @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction d) { return flammability(); }
        @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction d) { return fireSpreadSpeed(); }
    }

    public static class Shelf extends ShelfBlock implements Flammable {
        public Shelf(Properties properties) { super(properties); }
        @Override public int flammability() { return 20; }
        @Override public int fireSpreadSpeed() { return 30; }
        @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction d) {
            return s.getValue(WATERLOGGED) ? 0 : flammability();
        }
        @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction d) {
            return s.getValue(WATERLOGGED) ? 0 : fireSpreadSpeed();
        }
    }

    private FlammableBlocks() {
    }
}
