package com.somake.wotn.client.model;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.entity.GroundWaveEntity;

import net.minecraft.resources.Identifier;

public class GroundWaveModel extends DefaultedEntityGeoModel<GroundWaveEntity> {
    private static final Identifier GROUND = Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "ground");

    public GroundWaveModel() {
        super(GROUND);
        withAltTexture(GROUND);
    }
}
