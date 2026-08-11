package com.somake.wotn.data;

import java.util.concurrent.CompletableFuture;

import com.somake.wotn.registry.ModBlocks;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.crafting.Ingredient;

public final class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        planksFromLogs(ModBlocks.YGGDRASIL_PLANKS.get(), ModTags.YGGDRASIL_LOGS_ITEM, 4);
        woodFromLogs(ModBlocks.YGGDRASIL_WOOD.get(), ModBlocks.YGGDRASIL_LOG.get());
        woodFromLogs(ModBlocks.STRIPPED_YGGDRASIL_WOOD.get(), ModBlocks.STRIPPED_YGGDRASIL_LOG.get());

        Ingredient planks = Ingredient.of(ModBlocks.YGGDRASIL_PLANKS.get());
        stairBuilder(ModBlocks.YGGDRASIL_STAIRS.get(), planks)
                .unlockedBy(getHasName(ModBlocks.YGGDRASIL_PLANKS.get()), has(ModBlocks.YGGDRASIL_PLANKS.get()))
                .save(output);
        slabBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.YGGDRASIL_SLAB.get(), planks)
                .unlockedBy(getHasName(ModBlocks.YGGDRASIL_PLANKS.get()), has(ModBlocks.YGGDRASIL_PLANKS.get()))
                .save(output);
        fenceBuilder(ModBlocks.YGGDRASIL_FENCE.get(), planks)
                .unlockedBy(getHasName(ModBlocks.YGGDRASIL_PLANKS.get()), has(ModBlocks.YGGDRASIL_PLANKS.get()))
                .save(output);
        fenceGateBuilder(ModBlocks.YGGDRASIL_FENCE_GATE.get(), planks)
                .unlockedBy(getHasName(ModBlocks.YGGDRASIL_PLANKS.get()), has(ModBlocks.YGGDRASIL_PLANKS.get()))
                .save(output);
        doorBuilder(ModBlocks.YGGDRASIL_DOOR.get(), planks)
                .unlockedBy(getHasName(ModBlocks.YGGDRASIL_PLANKS.get()), has(ModBlocks.YGGDRASIL_PLANKS.get()))
                .save(output);
        trapdoorBuilder(ModBlocks.YGGDRASIL_TRAPDOOR.get(), planks)
                .unlockedBy(getHasName(ModBlocks.YGGDRASIL_PLANKS.get()), has(ModBlocks.YGGDRASIL_PLANKS.get()))
                .save(output);
        pressurePlateBuilder(RecipeCategory.REDSTONE, ModBlocks.YGGDRASIL_PRESSURE_PLATE.get(), planks)
                .unlockedBy(getHasName(ModBlocks.YGGDRASIL_PLANKS.get()), has(ModBlocks.YGGDRASIL_PLANKS.get()))
                .save(output);
        buttonBuilder(ModBlocks.YGGDRASIL_BUTTON.get(), planks)
                .unlockedBy(getHasName(ModBlocks.YGGDRASIL_PLANKS.get()), has(ModBlocks.YGGDRASIL_PLANKS.get()))
                .save(output);
        shelf(ModBlocks.YGGDRASIL_SHELF.get(), ModBlocks.STRIPPED_YGGDRASIL_LOG.get());
    }

    public static final class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ModRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Whispers of the North Recipes";
        }
    }
}
