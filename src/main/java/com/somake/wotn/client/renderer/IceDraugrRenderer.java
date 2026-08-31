package com.somake.wotn.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import com.somake.wotn.client.model.IceDraugrModel;
import com.somake.wotn.entity.IceDraugrEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class IceDraugrRenderer extends GeoEntityRenderer<IceDraugrEntity, LivingEntityRenderState> {
    public IceDraugrRenderer(EntityRendererProvider.Context context) {
        super(context, new IceDraugrModel());
        withRenderLayer(new ItemInHandGeoLayer(context, this, null, "LeftHandItem"));
        this.shadowRadius = 0.5F;
    }
}
