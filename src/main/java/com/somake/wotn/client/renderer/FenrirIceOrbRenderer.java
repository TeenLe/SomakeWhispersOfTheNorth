package com.somake.wotn.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.somake.wotn.client.model.FenrirIceOrbModel;
import com.somake.wotn.entity.FenrirIceOrbEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public class FenrirIceOrbRenderer extends GeoEntityRenderer<FenrirIceOrbEntity, EntityRenderState> {
    public FenrirIceOrbRenderer(EntityRendererProvider.Context context) {
        super(context, new FenrirIceOrbModel());
        this.shadowRadius = 0.0F;
    }

    @Override
    public RenderType getRenderType(EntityRenderState state, Identifier texture) {
        return net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(texture);
    }
}
