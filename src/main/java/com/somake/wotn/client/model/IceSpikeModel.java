package com.somake.wotn.client.model;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.entity.IceSpikeEntity;

import net.minecraft.resources.Identifier;

public class IceSpikeModel extends DefaultedEntityGeoModel<IceSpikeEntity> {
    private static final Identifier ICE_SPIKE = Identifier.fromNamespaceAndPath(
            WhispersOfTheNorth.MODID, "ice_spike");

    public IceSpikeModel() {
        super(ICE_SPIKE);
        withAltTexture(Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "ice_spikes"));
    }
}
