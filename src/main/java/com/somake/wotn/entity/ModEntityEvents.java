package com.somake.wotn.entity;

import com.somake.wotn.registry.ModEntities;

import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public final class ModEntityEvents {
    private ModEntityEvents() {
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GOLEM.get(), GolemEntity.createAttributes().build());
    }
}
