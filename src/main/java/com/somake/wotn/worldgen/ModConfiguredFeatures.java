package com.somake.wotn.worldgen;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.registry.ModBlocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;

public final class ModConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> YGGDRASIL_TREE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "yggdrasil_tree"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> IDUNN_TREE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "idunn_tree"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> IDUNN_MEGA_TREE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "idunn_mega_tree"));

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        TreeConfiguration yggdrasilConfiguration = new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.YGGDRASIL_LOG.get()),
                new StraightTrunkPlacer(4, 2, 0),
                BlockStateProvider.simple(ModBlocks.YGGDRASIL_LEAVES.get()),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 3),
                new TwoLayersFeatureSize(1, 0, 1))
                .ignoreVines()
                .build();
        context.register(YGGDRASIL_TREE, new ConfiguredFeature<>(Feature.TREE, yggdrasilConfiguration));
        context.register(IDUNN_TREE, new ConfiguredFeature<>(
                com.somake.wotn.registry.ModFeatures.IDUNN_TREE_1.get(),
                net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE));
        context.register(IDUNN_MEGA_TREE, new ConfiguredFeature<>(
                com.somake.wotn.registry.ModFeatures.IDUNN_TREE_2.get(),
                net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE));
    }

    private ModConfiguredFeatures() {
    }
}
