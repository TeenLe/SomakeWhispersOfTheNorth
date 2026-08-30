package com.somake.wotn.data;

import java.util.Set;

import com.somake.wotn.block.BilberryBushBlock;
import com.somake.wotn.registry.ModBlocks;
import com.somake.wotn.registry.ModItems;

import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public final class ModBlockLootProvider extends BlockLootSubProvider {
    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.<Item>of(), FeatureFlags.DEFAULT_FLAGS, registries);
    }

    @Override
    protected void generate() {
        HolderLookup.RegistryLookup<Enchantment> enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);

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
        dropSelf(ModBlocks.IDUNN_LOG.get());
        dropSelf(ModBlocks.IDUNN_WOOD.get());
        dropSelf(ModBlocks.STRIPPED_IDUNN_LOG.get());
        dropSelf(ModBlocks.STRIPPED_IDUNN_WOOD.get());
        dropSelf(ModBlocks.IDUNN_PLANKS.get());
        dropSelf(ModBlocks.IDUNN_STAIRS.get());
        add(ModBlocks.IDUNN_SLAB.get(), createSlabItemTable(ModBlocks.IDUNN_SLAB.get()));
        dropSelf(ModBlocks.IDUNN_FENCE.get());
        dropSelf(ModBlocks.IDUNN_FENCE_GATE.get());
        add(ModBlocks.IDUNN_DOOR.get(), createDoorTable(ModBlocks.IDUNN_DOOR.get()));
        dropSelf(ModBlocks.IDUNN_TRAPDOOR.get());
        dropSelf(ModBlocks.IDUNN_PRESSURE_PLATE.get());
        dropSelf(ModBlocks.IDUNN_BUTTON.get());
        add(ModBlocks.IDUNN_LEAVES.get(), createIdunnLeavesDrops());
        dropSelf(ModBlocks.IDUNN_SAPLING.get());
        add(ModBlocks.IDUNN_LEAF_LITTER.get(), createSegmentedBlockDrops(ModBlocks.IDUNN_LEAF_LITTER.get()));
        dropSelf(ModBlocks.BOOK_PILE.get());
        dropSelf(ModBlocks.ALCHEMIST_CAULDRON.get());
        add(ModBlocks.BILBERRY_BUSH.get(), block -> applyExplosionDecay(
                block,
                LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                                .when(LootItemBlockStatePropertyCondition
                                        .hasBlockStateProperties(ModBlocks.BILBERRY_BUSH.get())
                                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                                .hasProperty(BilberryBushBlock.AGE, 3)))
                                .add(LootItem.lootTableItem(ModItems.BILBERRY.get()))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 3.0F)))
                                .apply(ApplyBonusCount.addUniformBonusCount(
                                        enchantments.getOrThrow(Enchantments.FORTUNE))))
                        .withPool(LootPool.lootPool()
                                .when(LootItemBlockStatePropertyCondition
                                        .hasBlockStateProperties(ModBlocks.BILBERRY_BUSH.get())
                                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                                .hasProperty(BilberryBushBlock.AGE, 2)))
                                .add(LootItem.lootTableItem(ModItems.BILBERRY.get()))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))
                                .apply(ApplyBonusCount.addUniformBonusCount(
                                        enchantments.getOrThrow(Enchantments.FORTUNE))))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(holder -> (Block) holder.value()).toList();
    }

    private LootTable.Builder createIdunnLeavesDrops() {
        HolderLookup.RegistryLookup<Enchantment> enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
        Block leaves = ModBlocks.IDUNN_LEAVES.get();
        return createLeavesDrops(leaves, ModBlocks.IDUNN_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES)
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(hasShears().or(hasSilkTouch()).invert())
                        .add(applyExplosionCondition(leaves, LootItem.lootTableItem(ModItems.IDUNN_APPLE.get()))
                                .when(BonusLevelTableCondition.bonusLevelFlatChance(
                                        enchantments.getOrThrow(Enchantments.FORTUNE),
                                        0.005F, 0.0055555557F, 0.00625F, 0.008333334F, 0.025F))));
    }
}
