package com.somake.wotn.worldgen;

import java.util.Optional;

import net.minecraft.world.level.block.grower.TreeGrower;

public final class ModTreeGrowers {
    public static final TreeGrower YGGDRASIL = new TreeGrower(
            "wotn:yggdrasil",
            Optional.empty(),
            Optional.of(ModConfiguredFeatures.YGGDRASIL_TREE),
            Optional.empty());
    public static final TreeGrower IDUNN = new TreeGrower(
            "wotn:idunn",
            Optional.of(ModConfiguredFeatures.IDUNN_MEGA_TREE),
            Optional.of(ModConfiguredFeatures.IDUNN_TREE),
            Optional.empty());

    private ModTreeGrowers() {
    }
}
