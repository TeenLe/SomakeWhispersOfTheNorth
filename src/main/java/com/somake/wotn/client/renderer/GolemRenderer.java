package com.somake.wotn.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.somake.wotn.client.model.GolemModel;
import com.somake.wotn.entity.GolemEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class GolemRenderer extends GeoEntityRenderer<GolemEntity, LivingEntityRenderState> {
    public GolemRenderer(EntityRendererProvider.Context context) {
        super(context, new GolemModel());
        this.shadowRadius = 1.15F;
    }
}
