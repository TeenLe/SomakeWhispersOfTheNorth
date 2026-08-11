package com.somake.wotn.data;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.worldgen.ModConfiguredFeatures;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;

public final class ModDataGenerators {
    public static void gatherClientData(GatherDataEvent.Client event) {
        event.createProvider(output -> new ModModelProvider(output, WhispersOfTheNorth.MODID));
    }

    public static void gatherServerData(GatherDataEvent.Server event) {
        event.createDatapackRegistryObjects(new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap));
        event.createBlockAndItemTags(ModBlockTagsProvider::new, ModItemTagsProvider::new);
        event.createProvider(ModRecipeProvider.Runner::new);
        event.addProvider(new LootTableProvider(
                event.getGenerator().getPackOutput(),
                Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootProvider::new, LootContextParamSets.BLOCK)),
                event.getLookupProvider()));
    }

    private ModDataGenerators() {
    }
}
