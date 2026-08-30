package com.somake.wotn.registry;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.block.entity.PotionDisplayBlockEntity;
import com.somake.wotn.block.entity.FenrirSealBlockEntity;
import com.somake.wotn.block.entity.GolemEncounterControllerBlockEntity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, WhispersOfTheNorth.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PotionDisplayBlockEntity>> POTION_DISPLAY =
            BLOCK_ENTITY_TYPES.register("potion_display",
                    () -> new BlockEntityType<>(PotionDisplayBlockEntity::new, ModBlocks.POTION_DISPLAY.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FenrirSealBlockEntity>> FENRIR_SEAL =
            BLOCK_ENTITY_TYPES.register("fenrir_seal",
                    () -> new BlockEntityType<>(FenrirSealBlockEntity::new, ModBlocks.FENRIR_SEAL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GolemEncounterControllerBlockEntity>> GOLEM_ENCOUNTER_CONTROLLER =
            BLOCK_ENTITY_TYPES.register("golem_encounter_controller",
                    () -> new BlockEntityType<>(GolemEncounterControllerBlockEntity::new,
                            ModBlocks.GOLEM_ENCOUNTER_CONTROLLER.get()));

    private ModBlockEntities() {
    }
}
