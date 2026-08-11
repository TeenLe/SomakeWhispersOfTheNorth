package com.somake.wotn.client.renderer;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.somake.wotn.client.renderer.state.ItemPreviewRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;

public final class ItemPreviewRenderer extends PictureInPictureRenderer<ItemPreviewRenderState> {
    public ItemPreviewRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<ItemPreviewRenderState> getRenderStateClass() {
        return ItemPreviewRenderState.class;
    }

    @Override
    protected void renderToTexture(ItemPreviewRenderState state, PoseStack poseStack) {
        ItemStackRenderState item = state.item();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gameRenderer.getLighting().setupFor(
                item.usesBlockLight() ? Lighting.Entry.ITEMS_3D : Lighting.Entry.ITEMS_FLAT);

        poseStack.scale(1.0F, -1.0F, -1.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.rotationX()));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.rotationY()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.rotationZ()));

        AABB bounds = item.getModelBoundingBox();
        poseStack.translate(-(bounds.minX + bounds.maxX) * 0.5D,
                -(bounds.minY + bounds.maxY) * 0.5D,
                -(bounds.minZ + bounds.maxZ) * 0.5D);

        FeatureRenderDispatcher dispatcher = minecraft.gameRenderer.getFeatureRenderDispatcher();
        SubmitNodeStorage submitNodes = dispatcher.getSubmitNodeStorage();
        item.submit(poseStack, submitNodes, 15728880, OverlayTexture.NO_OVERLAY, 0);
        dispatcher.renderAllFeatures();
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "wotn_item_preview";
    }
}
