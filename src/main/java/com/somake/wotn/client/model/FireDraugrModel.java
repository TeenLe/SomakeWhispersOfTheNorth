package com.somake.wotn.client.model;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.entity.FireDraugrEntity;

import net.minecraft.resources.Identifier;

public final class FireDraugrModel extends DefaultedEntityGeoModel<FireDraugrEntity> {
    public FireDraugrModel() {
        super(Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "fire_draugr"));
    }
}
