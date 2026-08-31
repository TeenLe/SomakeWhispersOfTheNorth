package com.somake.wotn.registry;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.worldgen.structure.FenrirDungeonStructure;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {
    public static final ResourceKey<Structure> FENRIR_DUNGEON_KEY = ResourceKey.create(
            Registries.STRUCTURE, Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "fenrir_dungeon"));
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(
            Registries.STRUCTURE_TYPE, WhispersOfTheNorth.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<FenrirDungeonStructure>> FENRIR_DUNGEON =
            STRUCTURE_TYPES.register("fenrir_dungeon", () -> () -> FenrirDungeonStructure.CODEC);

    private ModStructures() {
    }
}
