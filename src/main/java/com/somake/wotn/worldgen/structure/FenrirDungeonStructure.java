package com.somake.wotn.worldgen.structure;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.somake.wotn.registry.ModStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/**
 * A fixed-footprint Jigsaw root that only generates Fenrir's mountain on terrain
 * that can support its full base. Vanilla Jigsaw projection samples only the
 * center of the 250x235 root template, which can leave most of it suspended
 * when that point happens to sit near the edge of a peak.
 */
public final class FenrirDungeonStructure extends Structure {
    public static final MapCodec<FenrirDungeonStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool),
            Identifier.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(structure -> structure.startJigsawName),
            Codec.intRange(0, 20).fieldOf("size").forGetter(structure -> structure.maxDepth),
            Codec.intRange(16, 128).optionalFieldOf("footprint_radius", 128)
                    .forGetter(structure -> structure.footprintRadius),
            Codec.intRange(16, 128).optionalFieldOf("sample_step", 64)
                    .forGetter(structure -> structure.sampleStep),
            Codec.intRange(0, 64).optionalFieldOf("max_terrain_variation", 14)
                    .forGetter(structure -> structure.maxTerrainVariation),
            Codec.intRange(-64, 0).optionalFieldOf("terrain_offset", -1)
                    .forGetter(structure -> structure.terrainOffset),
            JigsawStructure.MaxDistance.CODEC.fieldOf("max_distance_from_center")
                    .forGetter(structure -> structure.maxDistanceFromCenter),
            LiquidSettings.CODEC.optionalFieldOf("liquid_settings", LiquidSettings.IGNORE_WATERLOGGING)
                    .forGetter(structure -> structure.liquidSettings))
            .apply(instance, FenrirDungeonStructure::new));

    private final Holder<StructureTemplatePool> startPool;
    private final Optional<Identifier> startJigsawName;
    private final int maxDepth;
    private final int footprintRadius;
    private final int sampleStep;
    private final int maxTerrainVariation;
    private final int terrainOffset;
    private final JigsawStructure.MaxDistance maxDistanceFromCenter;
    private final LiquidSettings liquidSettings;

    public FenrirDungeonStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool,
            Optional<Identifier> startJigsawName, int maxDepth, int footprintRadius, int sampleStep,
            int maxTerrainVariation, int terrainOffset, JigsawStructure.MaxDistance maxDistanceFromCenter,
            LiquidSettings liquidSettings) {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.maxDepth = maxDepth;
        this.footprintRadius = footprintRadius;
        this.sampleStep = sampleStep;
        this.maxTerrainVariation = maxTerrainVariation;
        this.terrainOffset = terrainOffset;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.liquidSettings = liquidSettings;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        Optional<PlacementSite> site = findPlacementSite(
                context.chunkGenerator(), context.heightAccessor(), context.randomState(),
                context.validBiome(), chunk);
        if (site.isEmpty()) return Optional.empty();

        PlacementSite placement = site.get();
        BlockPos anchor = new BlockPos(chunk.getMinBlockX(), placement.baseY(), chunk.getMinBlockZ());
        Optional<GenerationStub> generated = JigsawPlacement.addPieces(context, startPool, startJigsawName, maxDepth, anchor,
                false, Optional.empty(), maxDistanceFromCenter, PoolAliasLookup.EMPTY,
                DimensionPadding.ZERO, liquidSettings);
        return generated.map(stub -> new GenerationStub(
                new BlockPos(stub.position().getX(), placement.surfaceY(), stub.position().getZ()),
                builder -> stub.getPiecesBuilder().build().pieces().forEach(builder::addPiece)));
    }

    public Optional<PlacementSite> findPlacementSite(ChunkGenerator generator, LevelHeightAccessor heightAccessor,
            RandomState randomState, java.util.function.Predicate<Holder<Biome>> validBiome, ChunkPos chunk) {
        int anchorX = chunk.getMinBlockX();
        int anchorZ = chunk.getMinBlockZ();
        int centerHeight = generator.getFirstFreeHeight(
                anchorX, anchorZ, Heightmap.Types.OCEAN_FLOOR_WG,
                heightAccessor, randomState);
        Holder<Biome> centerBiome = generator.getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock(anchorX), QuartPos.fromBlock(centerHeight), QuartPos.fromBlock(anchorZ),
                randomState.sampler());
        if (!validBiome.test(centerBiome)) return Optional.empty();

        int minimum = centerHeight;
        int maximum = centerHeight;

        for (int x = -footprintRadius; x <= footprintRadius; x += sampleStep) {
            for (int z = -footprintRadius; z <= footprintRadius; z += sampleStep) {
                if (x == 0 && z == 0) continue;
                int height = generator.getFirstFreeHeight(
                        anchorX + x, anchorZ + z, Heightmap.Types.OCEAN_FLOOR_WG,
                        heightAccessor, randomState);
                minimum = Math.min(minimum, height);
                maximum = Math.max(maximum, height);
                if (maximum - minimum > maxTerrainVariation) return Optional.empty();
            }
        }

        return Optional.of(new PlacementSite(minimum + terrainOffset, centerHeight));
    }

    public record PlacementSite(int baseY, int surfaceY) {
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.FENRIR_DUNGEON.get();
    }
}
