package com.somake.wotn.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.somake.wotn.client.model.FenrirModel;
import com.somake.wotn.client.renderer.layer.FenrirPawTrailLayer;
import com.somake.wotn.entity.FenrirEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.AABB;

public class FenrirRenderer extends GeoEntityRenderer<FenrirEntity, LivingEntityRenderState> {
    public FenrirRenderer(EntityRendererProvider.Context context) {
        super(context, new FenrirModel());
        withScale(3.0F);
        withRenderLayer(new FenrirPawTrailLayer(this));
        this.shadowRadius = 1.1F;
    }

    @Override
    protected AABB getBoundingBoxForCulling(FenrirEntity entity) {
        return entity.getBoundingBox().inflate(5.0D);
    }
}
