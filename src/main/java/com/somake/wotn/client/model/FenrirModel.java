package com.somake.wotn.client.model;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.entity.FenrirEntity;

import net.minecraft.resources.Identifier;

public class FenrirModel extends DefaultedEntityGeoModel<FenrirEntity> {
    public FenrirModel() {
        super(Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "fenrir"));
    }
}
