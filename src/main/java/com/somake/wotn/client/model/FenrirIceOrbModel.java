package com.somake.wotn.client.model;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.entity.FenrirIceOrbEntity;

import net.minecraft.resources.Identifier;

public class FenrirIceOrbModel extends DefaultedEntityGeoModel<FenrirIceOrbEntity> {
    private static final Identifier ORB = Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "fenrir_ice_orb");

    public FenrirIceOrbModel() {
        super(ORB);
        withAltTexture(ORB);
    }
}
