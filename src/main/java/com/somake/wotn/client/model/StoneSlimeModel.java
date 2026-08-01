package com.somake.wotn.client.model;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.entity.StoneSlimeEntity;

import net.minecraft.resources.Identifier;

public class StoneSlimeModel extends DefaultedEntityGeoModel<StoneSlimeEntity> {
    public StoneSlimeModel() {
        super(Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "stone_slime"));
    }
}
