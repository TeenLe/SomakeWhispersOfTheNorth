package com.somake.wotn.data;

import java.util.Set;

import com.somake.wotn.registry.ModBlocks;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModBlockLootProvider extends BlockLootSubProvider {
    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.<Item>of(), FeatureFlags.DEFAULT_FLAGS, registries);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.YGGDRASIL_LOG.get());
        dropSelf(ModBlocks.YGGDRASIL_WOOD.get());
        dropSelf(ModBlocks.STRIPPED_YGGDRASIL_LOG.get());
        dropSelf(ModBlocks.STRIPPED_YGGDRASIL_WOOD.get());
        dropSelf(ModBlocks.YGGDRASIL_PLANKS.get());
        dropSelf(ModBlocks.YGGDRASIL_STAIRS.get());
        add(ModBlocks.YGGDRASIL_SLAB.get(), createSlabItemTable(ModBlocks.YGGDRASIL_SLAB.get()));
        dropSelf(ModBlocks.YGGDRASIL_FENCE.get());
        dropSelf(ModBlocks.YGGDRASIL_FENCE_GATE.get());
        add(ModBlocks.YGGDRASIL_DOOR.get(), createDoorTable(ModBlocks.YGGDRASIL_DOOR.get()));
        dropSelf(ModBlocks.YGGDRASIL_TRAPDOOR.get());
        dropSelf(ModBlocks.YGGDRASIL_PRESSURE_PLATE.get());
        dropSelf(ModBlocks.YGGDRASIL_BUTTON.get());
        add(ModBlocks.YGGDRASIL_LEAVES.get(), createLeavesDrops(
                ModBlocks.YGGDRASIL_LEAVES.get(),
                ModBlocks.YGGDRASIL_SAPLING.get(),
                NORMAL_LEAVES_SAPLING_CHANCES));
        dropSelf(ModBlocks.YGGDRASIL_SAPLING.get());
        add(ModBlocks.YGGDRASIL_LEAF_LITTER.get(), createSegmentedBlockDrops(ModBlocks.YGGDRASIL_LEAF_LITTER.get()));
        dropSelf(ModBlocks.YGGDRASIL_SHELF.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(holder -> (Block) holder.value()).toList();
    }
}
