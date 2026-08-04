package com.somake.wotn.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.client.model.IceShellModel;
import com.somake.wotn.client.renderer.state.FrozenBlockRenderState;
import com.somake.wotn.entity.FrozenBlockEntity;
import com.somake.wotn.effect.FreezeManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;

public class FrozenBlockRenderer extends EntityRenderer<FrozenBlockEntity, FrozenBlockRenderState> {
    private static final Identifier ICE_TEXTURE = Identifier.fromNamespaceAndPath(
            WhispersOfTheNorth.MODID, "textures/entity/ice.png");
    private final IceShellModel model;

    public FrozenBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new IceShellModel(context.bakeLayer(IceShellModel.LAYER_LOCATION));
        this.shadowRadius = 0.0F;
    }

    @Override
    public void submit(FrozenBlockRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera) {
        float padding = 1.09F * state.visualScale;
        poseStack.pushPose();
        poseStack.scale(-state.width * padding, -state.height * 0.5F * padding, state.depth * padding);
        poseStack.translate(0.0F, -1.5F, 0.0F);
        int alpha = Mth.clamp((int) (200.0F * state.alpha), 0, 255);
        collector.submitModel(this.model, Unit.INSTANCE, poseStack, this.model.renderType(ICE_TEXTURE),
                state.lightCoords, OverlayTexture.NO_OVERLAY, (alpha << 24) | 0xFFFFFF,
                null, state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    protected int getBlockLightLevel(FrozenBlockEntity entity, BlockPos pos) {
        return Math.max(6, this.sampleSurroundingLight(entity, pos, LightLayer.BLOCK));
    }

    @Override
    protected int getSkyLightLevel(FrozenBlockEntity entity, BlockPos pos) {
        return this.sampleSurroundingLight(entity, pos, LightLayer.SKY);
    }

    private int sampleSurroundingLight(FrozenBlockEntity entity, BlockPos pos, LightLayer layer) {
        int light = entity.level().getBrightness(layer, pos.above());
        for (Direction direction : Direction.values()) {
            light = Math.max(light, entity.level().getBrightness(layer, pos.relative(direction)));
        }
        return light;
    }

    @Override
    public FrozenBlockRenderState createRenderState() {
        return new FrozenBlockRenderState();
    }

    @Override
    public void extractRenderState(FrozenBlockEntity entity, FrozenBlockRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.width = entity.getFrozenWidth();
        state.height = entity.getFrozenHeight();
        state.depth = entity.getFrozenDepth();
        float age = entity.tickCount + partialTick;
        float grow = Mth.clamp(age / 7.0F, 0.0F, 1.0F);
        float remaining = FreezeManager.DEFAULT_DURATION_TICKS - age;
        float shrink = Mth.clamp(remaining / 6.0F, 0.0F, 1.0F);
        state.visualScale = Mth.sin(grow * Mth.HALF_PI) * (0.82F + 0.18F * shrink);
        state.alpha = Mth.clamp(Math.min(grow * 1.5F, shrink * 1.4F), 0.0F, 1.0F);
    }
}
