package com.somake.wotn.client.model;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.entity.GolemEntity;

import net.minecraft.resources.Identifier;

public class GolemModel extends DefaultedEntityGeoModel<GolemEntity> {
    public GolemModel() {
        super(Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "golem"));
    }
}
