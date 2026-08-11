package com.somake.wotn.data;

import com.somake.wotn.registry.ModBlocks;
import com.somake.wotn.registry.ModItems;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.data.PackOutput;

public final class ModModelProvider extends ModelProvider {
    public ModModelProvider(PackOutput output, String modId) {
        super(output, modId);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        blockModels.woodProvider(ModBlocks.YGGDRASIL_LOG.get())
                .logWithHorizontal(ModBlocks.YGGDRASIL_LOG.get())
                .wood(ModBlocks.YGGDRASIL_WOOD.get());
        blockModels.woodProvider(ModBlocks.STRIPPED_YGGDRASIL_LOG.get())
                .logWithHorizontal(ModBlocks.STRIPPED_YGGDRASIL_LOG.get())
                .wood(ModBlocks.STRIPPED_YGGDRASIL_WOOD.get());

        BlockModelGenerators.BlockFamilyProvider family = blockModels.family(ModBlocks.YGGDRASIL_PLANKS.get());
        family.stairs(ModBlocks.YGGDRASIL_STAIRS.get());
        family.slab(ModBlocks.YGGDRASIL_SLAB.get());
        family.fence(ModBlocks.YGGDRASIL_FENCE.get());
        family.fenceGate(ModBlocks.YGGDRASIL_FENCE_GATE.get());
        family.door(ModBlocks.YGGDRASIL_DOOR.get());
        family.trapdoor(ModBlocks.YGGDRASIL_TRAPDOOR.get());
        family.pressurePlate(ModBlocks.YGGDRASIL_PRESSURE_PLATE.get());
        family.button(ModBlocks.YGGDRASIL_BUTTON.get());

        blockModels.createTrivialBlock(ModBlocks.YGGDRASIL_LEAVES.get(), TexturedModel.LEAVES);
        blockModels.createCrossBlockWithDefaultItem(
                ModBlocks.YGGDRASIL_SAPLING.get(), BlockModelGenerators.PlantType.NOT_TINTED);
        blockModels.createLeafLitter(ModBlocks.YGGDRASIL_LEAF_LITTER.get());
        blockModels.createShelf(ModBlocks.YGGDRASIL_SHELF.get(), ModBlocks.STRIPPED_YGGDRASIL_LOG.get());

        itemModels.declareCustomModelItem(ModItems.BILBERRY.get());
        itemModels.declareCustomModelItem(ModItems.FENRIR_FUR.get());
        itemModels.declareCustomModelItem(ModItems.GOLEM_CORE.get());
        itemModels.declareCustomModelItem(ModItems.LEVIATHAN_AXE.get());
    }
}
