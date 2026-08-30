package com.somake.wotn.data;

import java.util.concurrent.CompletableFuture;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.alchemy.AlchemyRune;
import com.somake.wotn.registry.ModItems;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagCopyingItemTagProvider;
import net.neoforged.neoforge.common.Tags;

public final class ModItemTagsProvider extends BlockTagCopyingItemTagProvider {
    public ModItemTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            CompletableFuture<TagsProvider.TagLookup<Block>> blockTags) {
        super(output, lookupProvider, blockTags, WhispersOfTheNorth.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        copy(ModTags.YGGDRASIL_LOGS_BLOCK, ModTags.YGGDRASIL_LOGS_ITEM);
        copy(ModTags.IDUNN_LOGS_BLOCK, ModTags.IDUNN_LOGS_ITEM);
        copy(BlockTags.LOGS, ItemTags.LOGS);
        copy(BlockTags.LOGS_THAT_BURN, ItemTags.LOGS_THAT_BURN);
        copy(BlockTags.PLANKS, ItemTags.PLANKS);
        copy(BlockTags.WOODEN_STAIRS, ItemTags.WOODEN_STAIRS);
        copy(BlockTags.WOODEN_SLABS, ItemTags.WOODEN_SLABS);
        copy(BlockTags.WOODEN_FENCES, ItemTags.WOODEN_FENCES);
        copy(BlockTags.FENCE_GATES, ItemTags.FENCE_GATES);
        copy(BlockTags.WOODEN_DOORS, ItemTags.WOODEN_DOORS);
        copy(BlockTags.WOODEN_TRAPDOORS, ItemTags.WOODEN_TRAPDOORS);
        copy(BlockTags.WOODEN_PRESSURE_PLATES, ItemTags.WOODEN_PRESSURE_PLATES);
        copy(BlockTags.WOODEN_BUTTONS, ItemTags.WOODEN_BUTTONS);
        copy(BlockTags.LEAVES, ItemTags.LEAVES);
        copy(BlockTags.SAPLINGS, ItemTags.SAPLINGS);
        copy(BlockTags.WOODEN_SHELVES, ItemTags.WOODEN_SHELVES);
        tag(ItemTags.FOX_FOOD).add(ModItems.BILBERRY.get());
        tag(ItemTags.SWORDS).add(ModItems.DRAUGR_SWORD.get());
        tag(Tags.Items.FOODS_BERRY).add(ModItems.BILBERRY.get());
        tag(Tags.Items.BONES).add(ModItems.ICE_BONE.get());
        tag(Tags.Items.MELEE_WEAPON_TOOLS).add(ModItems.DRAUGR_SWORD.get());
        tag(ModTags.NIFLHEIM_RUNES_ITEM).add(
                ModItems.runeItem(AlchemyRune.GLACIAL_DURATION).get(),
                ModItems.runeItem(AlchemyRune.GLACIAL_POWER).get(),
                ModItems.runeItem(AlchemyRune.FROST_MIST).get(),
                ModItems.runeItem(AlchemyRune.WHITEOUT).get(),
                ModItems.runeItem(AlchemyRune.RIME_MARK).get(),
                ModItems.runeItem(AlchemyRune.ABSOLUTE_ZERO).get());
    }
}
