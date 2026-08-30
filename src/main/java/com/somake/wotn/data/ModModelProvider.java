package com.somake.wotn.data;

import com.somake.wotn.registry.ModBlocks;
import com.somake.wotn.registry.ModItems;
import com.somake.wotn.block.BilberryBushBlock;
import com.somake.wotn.block.PotionDisplayBlock;
import com.somake.wotn.block.BookPileBlock;
import com.somake.wotn.client.property.LeviathanImbuedProperty;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.ItemDisplayContext;

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
        var leafLitter = ModBlocks.YGGDRASIL_LEAF_LITTER.get();
        var leafLitterModel1 = BlockModelGenerators.plainVariant(
                TexturedModel.LEAF_LITTER_1.create(leafLitter, blockModels.modelOutput));
        var leafLitterModel2 = BlockModelGenerators.plainVariant(
                TexturedModel.LEAF_LITTER_2.create(leafLitter, blockModels.modelOutput));
        var leafLitterModel3 = BlockModelGenerators.plainVariant(
                TexturedModel.LEAF_LITTER_3.create(leafLitter, blockModels.modelOutput));
        var leafLitterModel4 = BlockModelGenerators.plainVariant(
                TexturedModel.LEAF_LITTER_4.create(leafLitter, blockModels.modelOutput));
        var leafLitterItemModel = ModelTemplates.FLAT_ITEM.create(
                ModelLocationUtils.getModelLocation(leafLitter.asItem()),
                TextureMapping.layer0(leafLitter), blockModels.modelOutput);
        blockModels.registerSimpleItemModel(leafLitter.asItem(), leafLitterItemModel);
        blockModels.createSegmentedBlock(
                leafLitter,
                leafLitterModel1, BlockModelGenerators.LEAF_LITTER_MODEL_1_SEGMENT_CONDITION,
                leafLitterModel2, BlockModelGenerators.LEAF_LITTER_MODEL_2_SEGMENT_CONDITION,
                leafLitterModel3, BlockModelGenerators.LEAF_LITTER_MODEL_3_SEGMENT_CONDITION,
                leafLitterModel4, BlockModelGenerators.LEAF_LITTER_MODEL_4_SEGMENT_CONDITION);
        blockModels.createShelf(ModBlocks.YGGDRASIL_SHELF.get(), ModBlocks.STRIPPED_YGGDRASIL_LOG.get());

        blockModels.woodProvider(ModBlocks.IDUNN_LOG.get())
                .logWithHorizontal(ModBlocks.IDUNN_LOG.get())
                .wood(ModBlocks.IDUNN_WOOD.get());
        blockModels.woodProvider(ModBlocks.STRIPPED_IDUNN_LOG.get())
                .logWithHorizontal(ModBlocks.STRIPPED_IDUNN_LOG.get())
                .wood(ModBlocks.STRIPPED_IDUNN_WOOD.get());

        BlockModelGenerators.BlockFamilyProvider idunnFamily = blockModels.family(ModBlocks.IDUNN_PLANKS.get());
        idunnFamily.stairs(ModBlocks.IDUNN_STAIRS.get());
        idunnFamily.slab(ModBlocks.IDUNN_SLAB.get());
        idunnFamily.fence(ModBlocks.IDUNN_FENCE.get());
        idunnFamily.fenceGate(ModBlocks.IDUNN_FENCE_GATE.get());
        idunnFamily.door(ModBlocks.IDUNN_DOOR.get());
        idunnFamily.trapdoor(ModBlocks.IDUNN_TRAPDOOR.get());
        idunnFamily.pressurePlate(ModBlocks.IDUNN_PRESSURE_PLATE.get());
        idunnFamily.button(ModBlocks.IDUNN_BUTTON.get());

        blockModels.createTrivialBlock(ModBlocks.IDUNN_LEAVES.get(), TexturedModel.LEAVES);
        blockModels.createCrossBlockWithDefaultItem(
                ModBlocks.IDUNN_SAPLING.get(), BlockModelGenerators.PlantType.NOT_TINTED);
        var idunnLeafLitter = ModBlocks.IDUNN_LEAF_LITTER.get();
        var idunnLeafLitterModel1 = BlockModelGenerators.plainVariant(
                TexturedModel.LEAF_LITTER_1.create(idunnLeafLitter, blockModels.modelOutput));
        var idunnLeafLitterModel2 = BlockModelGenerators.plainVariant(
                TexturedModel.LEAF_LITTER_2.create(idunnLeafLitter, blockModels.modelOutput));
        var idunnLeafLitterModel3 = BlockModelGenerators.plainVariant(
                TexturedModel.LEAF_LITTER_3.create(idunnLeafLitter, blockModels.modelOutput));
        var idunnLeafLitterModel4 = BlockModelGenerators.plainVariant(
                TexturedModel.LEAF_LITTER_4.create(idunnLeafLitter, blockModels.modelOutput));
        var idunnLeafLitterItemModel = ModelTemplates.FLAT_ITEM.create(
                ModelLocationUtils.getModelLocation(idunnLeafLitter.asItem()),
                TextureMapping.layer0(idunnLeafLitter), blockModels.modelOutput);
        blockModels.registerSimpleItemModel(idunnLeafLitter.asItem(), idunnLeafLitterItemModel);
        blockModels.createSegmentedBlock(
                idunnLeafLitter,
                idunnLeafLitterModel1, BlockModelGenerators.LEAF_LITTER_MODEL_1_SEGMENT_CONDITION,
                idunnLeafLitterModel2, BlockModelGenerators.LEAF_LITTER_MODEL_2_SEGMENT_CONDITION,
                idunnLeafLitterModel3, BlockModelGenerators.LEAF_LITTER_MODEL_3_SEGMENT_CONDITION,
                idunnLeafLitterModel4, BlockModelGenerators.LEAF_LITTER_MODEL_4_SEGMENT_CONDITION);

        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.BOOK_PILE.get())
                .with(PropertyDispatch.initial(BookPileBlock.FORMAT)
                        .generate(ModModelProvider::bookPileVariants)));
        itemModels.itemModelOutput.accept(ModBlocks.BOOK_PILE_ITEM.get(), ItemModelUtils.select(
                new DisplayContext(),
                ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(ModBlocks.BOOK_PILE.get(), "_v4")),
                ItemModelUtils.when(ItemDisplayContext.GUI,
                        ItemModelUtils.plainModel(Identifier.fromNamespaceAndPath(
                                "wotn", "item/book_pile_inventory")))));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.POTION_DISPLAY.get())
                .with(PropertyDispatch.initial(PotionDisplayBlock.FAMILY, PotionDisplayBlock.TIER)
                        .generate((potionFamily, tier) -> BlockModelGenerators.plainVariant(
                                Identifier.fromNamespaceAndPath("wotn",
                                        "block/potions/" + potionFamily.modelPath(tier)))))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
        blockModels.createCrossBlock(
                ModBlocks.BILBERRY_BUSH.get(),
                BlockModelGenerators.PlantType.NOT_TINTED,
                BilberryBushBlock.AGE,
                0, 1, 2, 3);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(
                        ModBlocks.ALCHEMIST_CAULDRON.get(),
                        BlockModelGenerators.plainVariant(Identifier.fromNamespaceAndPath(
                                "wotn", "block/alchemist_cauldron")))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(
                        ModBlocks.FENRIR_SEAL.get(),
                        BlockModelGenerators.plainVariant(Identifier.fromNamespaceAndPath(
                                "wotn", "block/fenrir_seal")))
                .with(BlockModelGenerators.ROTATIONS_COLUMN_WITH_FACING));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(
                ModBlocks.GOLEM_ENCOUNTER_CONTROLLER.get(),
                BlockModelGenerators.plainVariant(Identifier.withDefaultNamespace("block/air"))));

        itemModels.declareCustomModelItem(ModItems.FENRIR_FUR.get());
        itemModels.declareCustomModelItem(ModItems.GOLEM_CORE.get());
        itemModels.generateFlatItem(ModItems.ICE_BONE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.WOLF_TOOTH.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.FENRIR_BLOOD.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.IDUNN_APPLE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.JORMUNGANDR_FANG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.JORMUNGANDR_VENOM.get(), ModelTemplates.FLAT_ITEM);
        itemModels.itemModelOutput.accept(ModItems.DRAUGR_SWORD.get(), ItemModelUtils.select(
                new DisplayContext(),
                ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(ModItems.DRAUGR_SWORD.get())),
                ItemModelUtils.when(ItemDisplayContext.GUI,
                        ItemModelUtils.plainModel(Identifier.fromNamespaceAndPath(
                                "wotn", "item/draugr_sword_inventory")))));
        itemModels.declareCustomModelItem(ModBlocks.ALCHEMIST_CAULDRON_ITEM.get());
        ModItems.SPAWN_EGGS.forEach(spawnEgg ->
                itemModels.generateFlatItem(spawnEgg.get(), ModelTemplates.FLAT_ITEM));
        itemModels.itemModelOutput.accept(ModItems.LEVIATHAN_AXE.get(), ItemModelUtils.conditional(
                new LeviathanImbuedProperty(),
                ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(ModItems.LEVIATHAN_AXE.get(), "_imbued")),
                ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(ModItems.LEVIATHAN_AXE.get()))));
        ModItems.TIERED_POTIONS.forEach(potion -> {
            var itemId = potion.getId();
            itemModels.itemModelOutput.accept(potion.get(), ItemModelUtils.select(
                    new DisplayContext(),
                    ItemModelUtils.plainModel(itemId.withPrefix("item/alchemy_preview/")),
                    ItemModelUtils.when(ItemDisplayContext.GUI,
                            ItemModelUtils.plainModel(itemId.withPrefix("item/")))));
        });
        ModItems.ALCHEMY_RUNES.forEach(rune -> {
            var model = ModelTemplates.FLAT_ITEM.create(
                    ModelLocationUtils.getModelLocation(rune.get()),
                    TextureMapping.layer0(new Material(rune.get().rune().textureId())),
                    blockModels.modelOutput);
            itemModels.itemModelOutput.accept(rune.get(), ItemModelUtils.plainModel(model));
        });
    }

    private static Identifier bookPileModel(int format, int palette) {
        String suffix = palette == 1 ? "" : "_" + palette;
        return Identifier.fromNamespaceAndPath("wotn", "block/book_pile_v" + format + suffix);
    }

    private static MultiVariant bookPileVariants(int format) {
        WeightedList.Builder<Variant> variants = WeightedList.builder();
        for (int palette = 1; palette <= 6; palette++) {
            variants.add(new Variant(bookPileModel(format, palette)));
        }
        return new MultiVariant(variants.build());
    }
}
