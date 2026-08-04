package com.somake.wotn.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.somake.wotn.client.renderer.state.LeviathanAxeRenderState;
import com.somake.wotn.entity.LeviathanAxeEntity;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

public class LeviathanAxeRenderer extends EntityRenderer<LeviathanAxeEntity, LeviathanAxeRenderState> {
    private final ItemModelResolver itemModelResolver;

    public LeviathanAxeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.shadowRadius = 0.2F;
    }

    @Override
    public void submit(LeviathanAxeRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(1.35F, 1.35F, 1.35F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.trajectoryYaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-state.trajectoryPitch));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.spin));
        float pulseScale = 1.0F + state.runePulse * 0.035F;
        poseStack.scale(pulseScale, pulseScale, pulseScale);
        state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY,
                state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    public LeviathanAxeRenderState createRenderState() {
        return new LeviathanAxeRenderState();
    }

    @Override
    public void extractRenderState(LeviathanAxeEntity entity, LeviathanAxeRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        this.itemModelResolver.updateForNonLiving(state.item, entity.getItem(), ItemDisplayContext.GROUND, entity);
        Vec3 movement = entity.getDeltaMovement();
        double horizontalSpeed = movement.horizontalDistance();
        if (movement.lengthSqr() > 1.0E-6D) {
            state.trajectoryYaw = (float) (Mth.atan2(movement.x, movement.z) * Mth.RAD_TO_DEG);
            state.trajectoryPitch = (float) (Mth.atan2(movement.y, horizontalSpeed) * Mth.RAD_TO_DEG);
        } else {
            state.trajectoryYaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
            state.trajectoryPitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        }
        state.returning = entity.isReturning();
        state.spin = (entity.tickCount + partialTick) * (state.returning ? -40.0F : 32.0F);
        state.runePulse = 0.5F + 0.5F * Mth.sin((entity.tickCount + partialTick) * (state.returning ? 1.15F : 0.72F));
    }
}
