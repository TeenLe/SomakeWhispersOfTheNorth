package com.somake.wotn.entity;

import com.somake.wotn.registry.ModEntities;
import com.somake.wotn.entity.npc.DialogueNpcEntity;

import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public final class ModEntityEvents {
    private ModEntityEvents() {
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GOLEM.get(), GolemEntity.createAttributes().build());
        event.put(ModEntities.STONE_SLIME.get(), StoneSlimeEntity.createAttributes().build());
        event.put(ModEntities.FENRIR.get(), FenrirEntity.createAttributes().build());
        event.put(ModEntities.ICE_DRAUGR.get(), DraugrEntity.createAttributes().build());
        event.put(ModEntities.FIRE_DRAUGR.get(), DraugrEntity.createAttributes().build());
        event.put(ModEntities.ALFRIGG.get(), DialogueNpcEntity.createAttributes().build());
        event.put(ModEntities.KVASIR.get(), DialogueNpcEntity.createAttributes().build());
    }
}
