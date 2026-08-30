package com.somake.wotn.data;

import java.util.function.BiConsumer;

import com.somake.wotn.block.BilberryBushBlock;
import com.somake.wotn.registry.ModBlocks;
import com.somake.wotn.registry.ModItems;

import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public record ModBlockInteractLootProvider(HolderLookup.Provider registries) implements LootTableSubProvider {
    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(
                BilberryBushBlock.HARVEST_LOOT_TABLE,
                LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                                .add(LootItem.lootTableItem(ModItems.BILBERRY.get())
                                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                                        .when(LootItemBlockStatePropertyCondition
                                                .hasBlockStateProperties(ModBlocks.BILBERRY_BUSH.get())
                                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                                        .hasProperty(BilberryBushBlock.AGE, 3)))))
                        .withPool(LootPool.lootPool()
                                .add(LootItem.lootTableItem(ModItems.BILBERRY.get())
                                        .apply(SetItemCountFunction.setCount(
                                                UniformGenerator.between(1.0F, 2.0F))))));
    }
}
