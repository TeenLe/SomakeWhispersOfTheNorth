package com.somake.wotn.data;

import java.util.concurrent.CompletableFuture;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.registry.ModBlocks;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

public final class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, WhispersOfTheNorth.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.YGGDRASIL_LOGS_BLOCK).add(
                ModBlocks.YGGDRASIL_LOG.get(),
                ModBlocks.YGGDRASIL_WOOD.get(),
                ModBlocks.STRIPPED_YGGDRASIL_LOG.get(),
                ModBlocks.STRIPPED_YGGDRASIL_WOOD.get());
        tag(BlockTags.LOGS).addTag(ModTags.YGGDRASIL_LOGS_BLOCK);
        tag(BlockTags.LOGS_THAT_BURN).addTag(ModTags.YGGDRASIL_LOGS_BLOCK);
        tag(BlockTags.PLANKS).add(ModBlocks.YGGDRASIL_PLANKS.get());
        tag(BlockTags.WOODEN_STAIRS).add(ModBlocks.YGGDRASIL_STAIRS.get());
        tag(BlockTags.WOODEN_SLABS).add(ModBlocks.YGGDRASIL_SLAB.get());
        tag(BlockTags.WOODEN_FENCES).add(ModBlocks.YGGDRASIL_FENCE.get());
        tag(BlockTags.FENCE_GATES).add(ModBlocks.YGGDRASIL_FENCE_GATE.get());
        tag(BlockTags.WOODEN_DOORS).add(ModBlocks.YGGDRASIL_DOOR.get());
        tag(BlockTags.WOODEN_TRAPDOORS).add(ModBlocks.YGGDRASIL_TRAPDOOR.get());
        tag(BlockTags.WOODEN_PRESSURE_PLATES).add(ModBlocks.YGGDRASIL_PRESSURE_PLATE.get());
        tag(BlockTags.WOODEN_BUTTONS).add(ModBlocks.YGGDRASIL_BUTTON.get());
        tag(BlockTags.LEAVES).add(ModBlocks.YGGDRASIL_LEAVES.get());
        tag(BlockTags.SAPLINGS).add(ModBlocks.YGGDRASIL_SAPLING.get());
        tag(BlockTags.WOODEN_SHELVES).add(ModBlocks.YGGDRASIL_SHELF.get());

        tag(BlockTags.MINEABLE_WITH_AXE).add(
                ModBlocks.YGGDRASIL_LOG.get(), ModBlocks.YGGDRASIL_WOOD.get(),
                ModBlocks.STRIPPED_YGGDRASIL_LOG.get(), ModBlocks.STRIPPED_YGGDRASIL_WOOD.get(),
                ModBlocks.YGGDRASIL_PLANKS.get(), ModBlocks.YGGDRASIL_STAIRS.get(),
                ModBlocks.YGGDRASIL_SLAB.get(), ModBlocks.YGGDRASIL_FENCE.get(),
                ModBlocks.YGGDRASIL_FENCE_GATE.get(), ModBlocks.YGGDRASIL_DOOR.get(),
                ModBlocks.YGGDRASIL_TRAPDOOR.get(), ModBlocks.YGGDRASIL_PRESSURE_PLATE.get(),
                ModBlocks.YGGDRASIL_BUTTON.get());
        tag(BlockTags.MINEABLE_WITH_HOE).add(ModBlocks.YGGDRASIL_LEAVES.get());
        tag(BlockTags.REPLACEABLE).add(ModBlocks.YGGDRASIL_LEAF_LITTER.get());
        tag(BlockTags.REPLACEABLE_BY_TREES).add(ModBlocks.YGGDRASIL_LEAF_LITTER.get());
        tag(BlockTags.REPLACEABLE_BY_MUSHROOMS).add(ModBlocks.YGGDRASIL_LEAF_LITTER.get());
        tag(BlockTags.INSIDE_STEP_SOUND_BLOCKS).add(ModBlocks.YGGDRASIL_LEAF_LITTER.get());
    }
}
