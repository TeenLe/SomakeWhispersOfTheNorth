package com.somake.wotn.worldgen.feature;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import com.somake.wotn.registry.ModBlocks;

public final class IdunnStructureTreeFeature extends Feature<NoneFeatureConfiguration> {
    private final Identifier templateId;
    private final BlockPos templateAnchor;

    public IdunnStructureTreeFeature(Codec<NoneFeatureConfiguration> codec,
            Identifier templateId, BlockPos templateAnchor) {
        super(codec);
        this.templateId = templateId;
        this.templateAnchor = templateAnchor;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        StructureTemplate template = context.level().getLevel().getStructureManager().get(templateId).orElse(null);
        if (template == null) return false;

        Rotation rotation = Rotation.NONE;
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(rotation)
                .setRotationPivot(templateAnchor)
                .setRandom(context.random())
                .setIgnoreEntities(true)
                .setKnownShape(false)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
        BlockPos placementOrigin = context.origin().subtract(
                StructureTemplate.transform(templateAnchor, Mirror.NONE, rotation, templateAnchor));

        // Validate every tree block before placement so TreeGrower can restore
        // the sapling or the 2x2 group if the crown would collide with terrain.
        var bounds = template.getBoundingBox(settings, placementOrigin);
        if (bounds.minY() < context.level().getMinY() || bounds.maxY() >= context.level().getMaxY()) return false;
        for (Block block : new Block[] {
                ModBlocks.IDUNN_LOG.get(), ModBlocks.IDUNN_WOOD.get(),
                ModBlocks.IDUNN_LEAVES.get(), ModBlocks.IDUNN_FENCE.get(),
                ModBlocks.IDUNN_LEAF_LITTER.get() }) {
            for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(
                    placementOrigin, settings, block)) {
                BlockPos target = info.pos();
                if (!canReplace(context, target)) return false;
            }
        }

        return template.placeInWorld(context.level(), placementOrigin, placementOrigin,
                settings, context.random(), 3);
    }

    private static boolean canReplace(FeaturePlaceContext<NoneFeatureConfiguration> context, BlockPos pos) {
        var state = context.level().getBlockState(pos);
        return state.canBeReplaced() || state.isAir() || state.is(ModBlocks.IDUNN_SAPLING.get())
                || state.is(ModBlocks.IDUNN_LEAVES.get()) || state.is(ModBlocks.IDUNN_LEAF_LITTER.get());
    }
}
