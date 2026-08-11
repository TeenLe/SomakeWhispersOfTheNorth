package com.somake.wotn.registry;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.entity.FenrirEntity;
import com.somake.wotn.entity.FrozenBlockEntity;
import com.somake.wotn.entity.GolemEntity;
import com.somake.wotn.entity.GroundWaveEntity;
import com.somake.wotn.entity.StoneSlimeEntity;
import com.somake.wotn.entity.LeviathanAxeEntity;
import com.somake.wotn.entity.IceSpikeEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(WhispersOfTheNorth.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<GolemEntity>> GOLEM = ENTITY_TYPES.registerEntityType("golem", GolemEntity::new, MobCategory.MONSTER,
            builder -> builder
                    .sized(1.6F, 3.3F)
                    .eyeHeight(2.9F)
                    .clientTrackingRange(10)
                    .updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<StoneSlimeEntity>> STONE_SLIME = ENTITY_TYPES.registerEntityType("stone_slime", StoneSlimeEntity::new, MobCategory.MONSTER,
            builder -> builder
                    .sized(1.35F, 1.15F)
                    .eyeHeight(0.72F)
                    .clientTrackingRange(8)
                    .updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<GroundWaveEntity>> GROUND_WAVE = ENTITY_TYPES.registerEntityType("ground_wave", GroundWaveEntity::new, MobCategory.MISC,
            builder -> builder
                    .sized(7.0F, 1.0F)
                    .clientTrackingRange(12)
                    .updateInterval(1));

    public static final DeferredHolder<EntityType<?>, EntityType<FenrirEntity>> FENRIR = ENTITY_TYPES.registerEntityType("fenrir", FenrirEntity::new, MobCategory.MONSTER,
            builder -> builder
                    .sized(2.2F, 2.3F)
                    .eyeHeight(1.85F)
                    .clientTrackingRange(16)
                    .updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<LeviathanAxeEntity>> LEVIATHAN_AXE_PROJECTILE = ENTITY_TYPES.registerEntityType("leviathan_axe_projectile", LeviathanAxeEntity::new, MobCategory.MISC,
            builder -> builder
                    .sized(0.8F, 0.8F)
                    .clientTrackingRange(12)
                    .updateInterval(1));

    public static final DeferredHolder<EntityType<?>, EntityType<FrozenBlockEntity>> FROZEN_BLOCK = ENTITY_TYPES.registerEntityType("frozen_block", FrozenBlockEntity::new, MobCategory.MISC,
            builder -> builder
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(10)
                    .updateInterval(1));

    public static final DeferredHolder<EntityType<?>, EntityType<IceSpikeEntity>> ICE_SPIKE = ENTITY_TYPES.registerEntityType("ice_spike", IceSpikeEntity::new, MobCategory.MISC,
            builder -> builder
                    .sized(0.8F, 2.6F)
                    .clientTrackingRange(10)
                    .updateInterval(2));

    private ModEntities() {
    }
}
