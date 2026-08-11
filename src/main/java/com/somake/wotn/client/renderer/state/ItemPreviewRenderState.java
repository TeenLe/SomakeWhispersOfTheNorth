package com.somake.wotn.client.renderer.state;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public record ItemPreviewRenderState(
        ItemStackRenderState item,
        float rotationX,
        float rotationY,
        float rotationZ,
        int x0,
        int y0,
        int x1,
        int y1,
        float scale,
        Matrix3x2f pose,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {

    public ItemPreviewRenderState(ItemStackRenderState item, float rotationX, float rotationY, float rotationZ,
            int x0, int y0, int x1, int y1, float scale, Matrix3x2f pose,
            @Nullable ScreenRectangle scissorArea) {
        this(item, rotationX, rotationY, rotationZ, x0, y0, x1, y1, scale, pose, scissorArea,
                PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }
}
