package com.somake.wotn.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import com.somake.wotn.client.model.FireDraugrModel;
import com.somake.wotn.entity.FireDraugrEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class FireDraugrRenderer extends GeoEntityRenderer<FireDraugrEntity, LivingEntityRenderState> {
    public FireDraugrRenderer(EntityRendererProvider.Context context) {
        super(context, new FireDraugrModel());
        withRenderLayer(new ItemInHandGeoLayer(context, this, null, "LeftHandItem"));
        this.shadowRadius = 0.5F;
    }
}
