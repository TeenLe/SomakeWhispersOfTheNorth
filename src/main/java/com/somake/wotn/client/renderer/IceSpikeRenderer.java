package com.somake.wotn.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.somake.wotn.client.model.IceSpikeModel;
import com.somake.wotn.entity.IceSpikeEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;

@SuppressWarnings({"rawtypes", "unchecked"})
public class IceSpikeRenderer extends GeoEntityRenderer<IceSpikeEntity, EntityRenderState> {
    private static final DataTicket<Float> VISUAL_SCALE = DataTickets.create(
            "wotn_ice_spike_visual_scale", Float.class);
    private static final DataTicket<Boolean> ACTIVE = DataTickets.create(
            "wotn_ice_spike_active", Boolean.class);
    private static final DataTicket<Float> YAW = DataTickets.create(
            "wotn_ice_spike_yaw", Float.class);
    private static final DataTicket<Float> TWIST = DataTickets.create(
            "wotn_ice_spike_twist", Float.class);
    private static final DataTicket<Float> LEAN = DataTickets.create(
            "wotn_ice_spike_lean", Float.class);

    public IceSpikeRenderer(EntityRendererProvider.Context context) {
        super(context, new IceSpikeModel());
        this.shadowRadius = 0.0F;
    }

    @Override
    public RenderType getRenderType(EntityRenderState state, net.minecraft.resources.Identifier texture) {
        return net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(texture);
    }

    @Override
    public int getRenderColor(IceSpikeEntity entity, Void relatedObject, float partialTick) {
        return 0xFFE9FCFF;
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (geckoState(state).getOrDefaultGeckolibData(ACTIVE, true)) {
            super.submit(state, poseStack, collector, camera);
        }
    }

    @Override
    public void scaleModelForRender(RenderPassInfo renderPass, float widthScale,
            float heightScale) {
        PoseStack poseStack = renderPass.poseStack();
        float scale = (float) renderPass.getOrDefaultGeckolibData(VISUAL_SCALE, 1.0F);
        poseStack.scale(scale, scale, scale);
    }

    @Override
    protected void applyRotations(RenderPassInfo renderPass, PoseStack poseStack,
            float nativeScale) {
        float yaw = (float) renderPass.getOrDefaultGeckolibData(YAW, 0.0F);
        float twist = (float) renderPass.getOrDefaultGeckolibData(TWIST, 0.0F);
        float lean = (float) renderPass.getOrDefaultGeckolibData(LEAN, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw + twist));
        poseStack.mulPose(Axis.ZP.rotationDegrees(lean));
    }

    @Override
    public void addRenderData(IceSpikeEntity entity, Void relatedObject, EntityRenderState state,
            float partialTick) {
        float age = entity.tickCount + partialTick - entity.getEmergenceDelay();
        GeoRenderState geoState = geckoState(state);
        geoState.addGeckolibData(ACTIVE, age >= 0.0F);
        geoState.addGeckolibData(VISUAL_SCALE, entity.getVisualScale());
        geoState.addGeckolibData(YAW, entity.getYRot());
        geoState.addGeckolibData(TWIST, (float) Math.floorMod(entity.getVariantSeed(), 360));
        geoState.addGeckolibData(LEAN, (Math.floorMod(entity.getVariantSeed(), 11) - 5) * 0.9F);
    }

    private static GeoRenderState geckoState(EntityRenderState state) {
        return (GeoRenderState) (Object) state;
    }

    @Override
    protected AABB getBoundingBoxForCulling(IceSpikeEntity entity) {
        float scale = Math.max(1.0F, entity.getVisualScale());
        return entity.getBoundingBox().inflate(3.5D * scale, 3.5D * scale, 3.5D * scale);
    }
}
