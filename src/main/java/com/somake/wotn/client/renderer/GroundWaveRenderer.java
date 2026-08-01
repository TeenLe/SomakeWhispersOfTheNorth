package com.somake.wotn.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.somake.wotn.client.model.GroundWaveModel;
import com.somake.wotn.entity.GroundWaveEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class GroundWaveRenderer extends GeoEntityRenderer<GroundWaveEntity, EntityRenderState> {
    public GroundWaveRenderer(EntityRendererProvider.Context context) {
        super(context, new GroundWaveModel());
        this.shadowRadius = 0.0F;
    }
}
