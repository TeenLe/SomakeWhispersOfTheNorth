package com.somake.wotn.data;

import java.util.concurrent.CompletableFuture;

import com.somake.wotn.registry.ModBlocks;
import com.somake.wotn.registry.ModItems;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
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

        planksFromLogs(ModBlocks.IDUNN_PLANKS.get(), ModTags.IDUNN_LOGS_ITEM, 4);
        woodFromLogs(ModBlocks.IDUNN_WOOD.get(), ModBlocks.IDUNN_LOG.get());
        woodFromLogs(ModBlocks.STRIPPED_IDUNN_WOOD.get(), ModBlocks.STRIPPED_IDUNN_LOG.get());

        Ingredient idunnPlanks = Ingredient.of(ModBlocks.IDUNN_PLANKS.get());
        stairBuilder(ModBlocks.IDUNN_STAIRS.get(), idunnPlanks)
                .unlockedBy(getHasName(ModBlocks.IDUNN_PLANKS.get()), has(ModBlocks.IDUNN_PLANKS.get()))
                .save(output);
        slabBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.IDUNN_SLAB.get(), idunnPlanks)
                .unlockedBy(getHasName(ModBlocks.IDUNN_PLANKS.get()), has(ModBlocks.IDUNN_PLANKS.get()))
                .save(output);
        fenceBuilder(ModBlocks.IDUNN_FENCE.get(), idunnPlanks)
                .unlockedBy(getHasName(ModBlocks.IDUNN_PLANKS.get()), has(ModBlocks.IDUNN_PLANKS.get()))
                .save(output);
        fenceGateBuilder(ModBlocks.IDUNN_FENCE_GATE.get(), idunnPlanks)
                .unlockedBy(getHasName(ModBlocks.IDUNN_PLANKS.get()), has(ModBlocks.IDUNN_PLANKS.get()))
                .save(output);
        doorBuilder(ModBlocks.IDUNN_DOOR.get(), idunnPlanks)
                .unlockedBy(getHasName(ModBlocks.IDUNN_PLANKS.get()), has(ModBlocks.IDUNN_PLANKS.get()))
                .save(output);
        trapdoorBuilder(ModBlocks.IDUNN_TRAPDOOR.get(), idunnPlanks)
                .unlockedBy(getHasName(ModBlocks.IDUNN_PLANKS.get()), has(ModBlocks.IDUNN_PLANKS.get()))
                .save(output);
        pressurePlateBuilder(RecipeCategory.REDSTONE, ModBlocks.IDUNN_PRESSURE_PLATE.get(), idunnPlanks)
                .unlockedBy(getHasName(ModBlocks.IDUNN_PLANKS.get()), has(ModBlocks.IDUNN_PLANKS.get()))
                .save(output);
        buttonBuilder(ModBlocks.IDUNN_BUTTON.get(), idunnPlanks)
                .unlockedBy(getHasName(ModBlocks.IDUNN_PLANKS.get()), has(ModBlocks.IDUNN_PLANKS.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, ModItems.LEVIATHAN_AXE.get())
                .define('I', ModItems.ICE_BONE.get())
                .define('D', Items.DIAMOND)
                .define('R', ModTags.NIFLHEIM_RUNES_ITEM)
                .define('S', Items.STICK)
                .define('N', Items.NETHERITE_INGOT)
                .pattern("IDR")
                .pattern(" SN")
                .pattern("S  ")
                .unlockedBy("has_ice_bone", has(ModItems.ICE_BONE.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModBlocks.ALCHEMIST_CAULDRON.get())
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CAULDRON)
                .define('B', Items.IRON_BLOCK)
                .pattern("ICI")
                .pattern("IBI")
                .unlockedBy("has_cauldron", has(Items.CAULDRON))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ModBlocks.BOOK_PILE.get())
                .define('B', Items.BOOK)
                .pattern("B")
                .pattern("B")
                .pattern("B")
                .unlockedBy("has_book", has(Items.BOOK))
                .save(output);

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
