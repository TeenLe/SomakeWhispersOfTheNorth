package com.somake.wotn.registry;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.entity.GolemEntity;

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

    private ModEntities() {
    }
}
