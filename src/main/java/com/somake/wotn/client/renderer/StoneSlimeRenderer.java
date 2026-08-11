package com.somake.wotn.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.somake.wotn.client.model.StoneSlimeModel;
import com.somake.wotn.entity.StoneSlimeEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class StoneSlimeRenderer extends GeoEntityRenderer<StoneSlimeEntity, LivingEntityRenderState> {
    public StoneSlimeRenderer(EntityRendererProvider.Context context) {
        super(context, new StoneSlimeModel());
        withScale(2.0F);
        this.shadowRadius = 1.1F;
    }
}
