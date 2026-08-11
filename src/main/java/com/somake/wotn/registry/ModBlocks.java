package com.somake.wotn.registry;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.block.FlammableBlocks;
import com.somake.wotn.worldgen.ModTreeGrowers;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WhispersOfTheNorth.MODID);

    public static final DeferredBlock<FlammableBlocks.Pillar> YGGDRASIL_LOG = registerBlock(
            "yggdrasil_log", FlammableBlocks.Pillar::new, Blocks.OAK_LOG);
    public static final DeferredBlock<FlammableBlocks.Pillar> YGGDRASIL_WOOD = registerBlock(
            "yggdrasil_wood", FlammableBlocks.Pillar::new, Blocks.OAK_WOOD);
    public static final DeferredBlock<FlammableBlocks.Pillar> STRIPPED_YGGDRASIL_LOG = registerBlock(
            "stripped_yggdrasil_log", FlammableBlocks.Pillar::new, Blocks.STRIPPED_OAK_LOG);
    public static final DeferredBlock<FlammableBlocks.Pillar> STRIPPED_YGGDRASIL_WOOD = registerBlock(
            "stripped_yggdrasil_wood", FlammableBlocks.Pillar::new, Blocks.STRIPPED_OAK_WOOD);

    public static final DeferredBlock<FlammableBlocks.Basic> YGGDRASIL_PLANKS = registerBlock(
            "yggdrasil_planks", FlammableBlocks.Basic::new, Blocks.OAK_PLANKS);
    public static final DeferredBlock<FlammableBlocks.Stairs> YGGDRASIL_STAIRS = registerBlock(
            "yggdrasil_stairs",
            properties -> new FlammableBlocks.Stairs(YGGDRASIL_PLANKS.get().defaultBlockState(), properties),
            Blocks.OAK_STAIRS);
    public static final DeferredBlock<FlammableBlocks.Slab> YGGDRASIL_SLAB = registerBlock(
            "yggdrasil_slab", FlammableBlocks.Slab::new, Blocks.OAK_SLAB);
    public static final DeferredBlock<FlammableBlocks.Fence> YGGDRASIL_FENCE = registerBlock(
            "yggdrasil_fence", FlammableBlocks.Fence::new, Blocks.OAK_FENCE);
    public static final DeferredBlock<FlammableBlocks.FenceGate> YGGDRASIL_FENCE_GATE = registerBlock(
            "yggdrasil_fence_gate",
            properties -> new FlammableBlocks.FenceGate(ModWoodTypes.YGGDRASIL, properties),
            Blocks.OAK_FENCE_GATE);
    public static final DeferredBlock<FlammableBlocks.Door> YGGDRASIL_DOOR = registerBlock(
            "yggdrasil_door",
            properties -> new FlammableBlocks.Door(ModWoodTypes.YGGDRASIL_SET, properties),
            Blocks.OAK_DOOR);
    public static final DeferredBlock<FlammableBlocks.Trapdoor> YGGDRASIL_TRAPDOOR = registerBlock(
            "yggdrasil_trapdoor",
            properties -> new FlammableBlocks.Trapdoor(ModWoodTypes.YGGDRASIL_SET, properties),
            Blocks.OAK_TRAPDOOR);
    public static final DeferredBlock<FlammableBlocks.PressurePlate> YGGDRASIL_PRESSURE_PLATE = registerBlock(
            "yggdrasil_pressure_plate",
            properties -> new FlammableBlocks.PressurePlate(ModWoodTypes.YGGDRASIL_SET, properties),
            Blocks.OAK_PRESSURE_PLATE);
    public static final DeferredBlock<FlammableBlocks.Button> YGGDRASIL_BUTTON = registerBlock(
            "yggdrasil_button",
            properties -> new FlammableBlocks.Button(ModWoodTypes.YGGDRASIL_SET, 30, properties),
            Blocks.OAK_BUTTON);

    public static final DeferredBlock<FlammableBlocks.Leaves> YGGDRASIL_LEAVES = registerBlock(
            "yggdrasil_leaves",
            properties -> new FlammableBlocks.Leaves(0.01F, ParticleTypes.PALE_OAK_LEAVES, properties),
            Blocks.PALE_OAK_LEAVES);
    public static final DeferredBlock<SaplingBlock> YGGDRASIL_SAPLING = registerBlock(
            "yggdrasil_sapling",
            properties -> new SaplingBlock(ModTreeGrowers.YGGDRASIL, properties),
            Blocks.OAK_SAPLING);
    public static final DeferredBlock<FlammableBlocks.LeafLitter> YGGDRASIL_LEAF_LITTER = registerBlock(
            "yggdrasil_leaf_litter", FlammableBlocks.LeafLitter::new, Blocks.LEAF_LITTER);
    public static final DeferredBlock<FlammableBlocks.Shelf> YGGDRASIL_SHELF = registerBlock(
            "yggdrasil_shelf", FlammableBlocks.Shelf::new, Blocks.OAK_SHELF);

    public static final DeferredItem<BlockItem> YGGDRASIL_LOG_ITEM = registerItem(YGGDRASIL_LOG);
    public static final DeferredItem<BlockItem> YGGDRASIL_WOOD_ITEM = registerItem(YGGDRASIL_WOOD);
    public static final DeferredItem<BlockItem> STRIPPED_YGGDRASIL_LOG_ITEM = registerItem(STRIPPED_YGGDRASIL_LOG);
    public static final DeferredItem<BlockItem> STRIPPED_YGGDRASIL_WOOD_ITEM = registerItem(STRIPPED_YGGDRASIL_WOOD);
    public static final DeferredItem<BlockItem> YGGDRASIL_PLANKS_ITEM = registerItem(YGGDRASIL_PLANKS);
    public static final DeferredItem<BlockItem> YGGDRASIL_STAIRS_ITEM = registerItem(YGGDRASIL_STAIRS);
    public static final DeferredItem<BlockItem> YGGDRASIL_SLAB_ITEM = registerItem(YGGDRASIL_SLAB);
    public static final DeferredItem<BlockItem> YGGDRASIL_FENCE_ITEM = registerItem(YGGDRASIL_FENCE);
    public static final DeferredItem<BlockItem> YGGDRASIL_FENCE_GATE_ITEM = registerItem(YGGDRASIL_FENCE_GATE);
    public static final DeferredItem<BlockItem> YGGDRASIL_DOOR_ITEM = registerItem(YGGDRASIL_DOOR);
    public static final DeferredItem<BlockItem> YGGDRASIL_TRAPDOOR_ITEM = registerItem(YGGDRASIL_TRAPDOOR);
    public static final DeferredItem<BlockItem> YGGDRASIL_PRESSURE_PLATE_ITEM = registerItem(YGGDRASIL_PRESSURE_PLATE);
    public static final DeferredItem<BlockItem> YGGDRASIL_BUTTON_ITEM = registerItem(YGGDRASIL_BUTTON);
    public static final DeferredItem<BlockItem> YGGDRASIL_LEAVES_ITEM = registerItem(YGGDRASIL_LEAVES);
    public static final DeferredItem<BlockItem> YGGDRASIL_SAPLING_ITEM = registerItem(YGGDRASIL_SAPLING);
    public static final DeferredItem<BlockItem> YGGDRASIL_LEAF_LITTER_ITEM = registerItem(YGGDRASIL_LEAF_LITTER);
    public static final DeferredItem<BlockItem> YGGDRASIL_SHELF_ITEM = ModItems.ITEMS.registerSimpleBlockItem(
            YGGDRASIL_SHELF,
            properties -> properties.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));

    public static void addBlockEntityTypeBlocks(BlockEntityTypeAddBlocksEvent event) {
        event.modify(BlockEntityType.SHELF, YGGDRASIL_SHELF.get());
    }

    private static DeferredBlock<Block> registerSimpleBlock(String name, Block vanillaBlock) {
        return BLOCKS.registerSimpleBlock(name, () -> BlockBehaviour.Properties.ofFullCopy(vanillaBlock));
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(
            String name,
            java.util.function.Function<BlockBehaviour.Properties, T> factory,
            Block vanillaBlock) {
        return BLOCKS.registerBlock(name, factory, () -> BlockBehaviour.Properties.ofFullCopy(vanillaBlock));
    }

    private static DeferredItem<BlockItem> registerItem(DeferredBlock<? extends Block> block) {
        return ModItems.ITEMS.registerSimpleBlockItem(block);
    }

    private ModBlocks() {
    }
}
