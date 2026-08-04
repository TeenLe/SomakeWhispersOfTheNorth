package com.somake.wotn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.client.model.IceShellModel;
import com.somake.wotn.registry.ModEffects;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class IceShellRenderer {
    private static final Identifier ICE_TEXTURE = Identifier.fromNamespaceAndPath(
            WhispersOfTheNorth.MODID, "textures/entity/ice.png");
    private static final ContextKey<Boolean> FROZEN = new ContextKey<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "frozen"));
    private static IceShellModel model;

    public static void initialize(EntityRenderersEvent.AddLayers event) {
        model = new IceShellModel(event.getEntityModels().bakeLayer(IceShellModel.LAYER_LOCATION));
    }

    public static void prepare(RenderLivingEvent.Pre<?, ?, ?> event) {
        var state = event.getRenderState();
        state.setRenderData(FROZEN, state.isFullyFrozen);
        state.isFullyFrozen = false;
    }

    public static void render(RenderLivingEvent.Post<?, ?, ?> event) {
        var state = event.getRenderState();
        if (model == null || !Boolean.TRUE.equals(state.getRenderData(FROZEN)) || state.isInvisible) {
            return;
        }

        float padding = 1.08F;
        float horizontalScale = Math.max(0.35F, state.boundingBoxWidth) * padding;
        float verticalScale = Math.max(0.35F, state.boundingBoxHeight) * 0.5F * padding;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.scale(-horizontalScale, -verticalScale, horizontalScale);
        poseStack.translate(0.0F, -1.5F, 0.0F);

        event.getSubmitNodeCollector().submitModel(model, Unit.INSTANCE, poseStack, model.renderType(ICE_TEXTURE),
                state.lightCoords, OverlayTexture.NO_OVERLAY, 0xC8FFFFFF, null, state.outlineColor, null);
        poseStack.popPose();
    }

    public static void onFrozenEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || !living.level().isClientSide()
                || !living.hasEffect(ModEffects.FROZEN) || living.tickCount % 4 != 0) {
            return;
        }
        double radius = living.getBbWidth() * 0.55D;
        double angle = living.getRandom().nextDouble() * Math.PI * 2.0D;
        living.level().addParticle(ParticleTypes.SNOWFLAKE,
                living.getX() + Math.cos(angle) * radius,
                living.getY() + living.getRandom().nextDouble() * living.getBbHeight(),
                living.getZ() + Math.sin(angle) * radius,
                0.0D, 0.012D, 0.0D);
    }

    private IceShellRenderer() {
    }
}
