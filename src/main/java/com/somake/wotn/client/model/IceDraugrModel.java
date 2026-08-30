package com.somake.wotn.client.model;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.entity.IceDraugrEntity;

import net.minecraft.resources.Identifier;

public final class IceDraugrModel extends DefaultedEntityGeoModel<IceDraugrEntity> {
    public IceDraugrModel() {
        super(Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "ice_draugr"));
    }
}
