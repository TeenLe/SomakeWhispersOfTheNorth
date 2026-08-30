package com.somake.wotn.registry;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.worldgen.feature.IdunnStructureTreeFeature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(
            Registries.FEATURE, WhispersOfTheNorth.MODID);

    public static final DeferredHolder<Feature<?>, IdunnStructureTreeFeature> IDUNN_TREE_1 = FEATURES.register(
            "idunn_tree_1",
            () -> new IdunnStructureTreeFeature(NoneFeatureConfiguration.CODEC,
                    Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "idunn_tree_1"),
                    new BlockPos(3, 0, 3)));
    public static final DeferredHolder<Feature<?>, IdunnStructureTreeFeature> IDUNN_TREE_2 = FEATURES.register(
            "idunn_tree_2",
            () -> new IdunnStructureTreeFeature(NoneFeatureConfiguration.CODEC,
                    Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "idunn_tree_2"),
                    new BlockPos(8, 0, 7)));

    private ModFeatures() {
    }
}
