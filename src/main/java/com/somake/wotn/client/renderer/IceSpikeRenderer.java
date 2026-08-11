package com.somake.wotn.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.client.model.IceSpikeModel;
import com.somake.wotn.client.renderer.state.IceSpikeRenderState;
import com.somake.wotn.entity.IceSpikeEntity;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;

public class IceSpikeRenderer extends EntityRenderer<IceSpikeEntity, IceSpikeRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            WhispersOfTheNorth.MODID, "textures/entity/ice.png");
    private final IceSpikeModel model;

    public IceSpikeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new IceSpikeModel(context.bakeLayer(IceSpikeModel.LAYER_LOCATION));
        this.shadowRadius = 0.0F;
    }

    @Override
    public void submit(IceSpikeRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (state.alpha <= 0.0F || state.visualScale <= 0.01F) return;
        poseStack.pushPose();
        poseStack.translate(0.0F, state.yOffset, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw + state.twist));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.lean));
        poseStack.scale(-state.visualScale, -state.visualScale, state.visualScale);
        poseStack.translate(0.0F, -1.5F, 0.0F);
        int alpha = Mth.clamp((int) (220.0F * state.alpha), 0, 255);
        collector.submitModel(this.model, Unit.INSTANCE, poseStack, this.model.renderType(TEXTURE),
                state.lightCoords, OverlayTexture.NO_OVERLAY, (alpha << 24) | 0xE9FCFF,
                null, state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    public IceSpikeRenderState createRenderState() {
        return new IceSpikeRenderState();
    }

    @Override
    public void extractRenderState(IceSpikeEntity entity, IceSpikeRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        float age = entity.tickCount + partialTick - entity.getEmergenceDelay();
        float emerge = Mth.clamp(age / IceSpikeEntity.EMERGE_TICKS, 0.0F, 1.0F);
        float endStart = IceSpikeEntity.EMERGE_TICKS + IceSpikeEntity.HOLD_TICKS;
        float shatter = Mth.clamp((age - endStart) / IceSpikeEntity.SHATTER_TICKS, 0.0F, 1.0F);
        float eased = 1.0F - (1.0F - emerge) * (1.0F - emerge) * (1.0F - emerge);
        float overshoot = emerge < 1.0F ? Mth.sin(emerge * Mth.PI) * 0.09F : 0.0F;
        state.visualScale = entity.getVisualScale() * (eased + overshoot) * (1.0F - shatter * 0.25F);
        state.yOffset = Mth.lerp(eased, -2.2F * entity.getVisualScale(), 0.0F) - shatter * 0.45F;
        state.alpha = age < 0.0F ? 0.0F : Mth.clamp(1.0F - shatter, 0.0F, 1.0F);
        state.yaw = entity.getYRot();
        state.twist = Math.floorMod(entity.getVariantSeed(), 360);
        state.lean = (Math.floorMod(entity.getVariantSeed(), 11) - 5) * 0.9F;
    }
}
