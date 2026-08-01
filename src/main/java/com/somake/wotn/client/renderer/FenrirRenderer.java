package com.somake.wotn.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.somake.wotn.client.model.FenrirModel;
import com.somake.wotn.entity.FenrirEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class FenrirRenderer extends GeoEntityRenderer<FenrirEntity, LivingEntityRenderState> {
    public FenrirRenderer(EntityRendererProvider.Context context) {
        super(context, new FenrirModel());
        this.shadowRadius = 1.1F;
    }
}
