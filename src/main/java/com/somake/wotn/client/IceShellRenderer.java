package com.somake.wotn.client;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.event.entity.CompileEntityRenderLayersEvent;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.client.model.IceShellModel;
import com.somake.wotn.registry.ModEffects;
import com.somake.wotn.particle.ParticleHelper;

import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class IceShellRenderer {
    private static final Identifier ICE_TEXTURE = Identifier.fromNamespaceAndPath(
            WhispersOfTheNorth.MODID, "textures/entity/ice.png");
    private static final ContextKey<Boolean> FROZEN = new ContextKey<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "frozen"));
    private static final DataTicket<Boolean> GEO_FROZEN = DataTickets.create(
            "wotn_ice_shell_frozen", Boolean.class);
    private static IceShellModel model;

    public static void initialize(EntityRenderersEvent.AddLayers event) {
        model = new IceShellModel(event.getEntityModels().bakeLayer(IceShellModel.LAYER_LOCATION));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier((Class) LivingEntityRenderer.class, (Entity entity, EntityRenderState state) -> {
            LivingEntity living = (LivingEntity) entity;
            LivingEntityRenderState livingState = (LivingEntityRenderState) state;
            boolean frozen = living.hasEffect(ModEffects.FROZEN);
            livingState.setRenderData(FROZEN, frozen);
            if (frozen) livingState.isFullyFrozen = false;
        });
    }

    public static void render(RenderLivingEvent.Post<?, ?, ?> event) {
        var state = event.getRenderState();
        if (model == null || !Boolean.TRUE.equals(state.getRenderData(FROZEN)) || state.isInvisible) {
            return;
        }

        submitShell(state, event.getPoseStack(), event.getSubmitNodeCollector());
    }

    public static void prepare(RenderLivingEvent.Pre<?, ?, ?> event) {
        LivingEntityRenderState state = event.getRenderState();
        boolean frozen = Boolean.TRUE.equals(state.getRenderData(FROZEN)) || state.isFullyFrozen;
        state.setRenderData(FROZEN, frozen);
        if (frozen) state.isFullyFrozen = false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void addGeoLayer(CompileEntityRenderLayersEvent event) {
        event.addLayer(new IceShellGeoLayer(event.getRenderer()));
    }

    private static void submitShell(EntityRenderState state, PoseStack poseStack,
            OrderedSubmitNodeCollector collector) {
        float padding = 1.08F;
        float horizontalScale = Math.max(0.35F, state.boundingBoxWidth) * padding;
        float verticalScale = Math.max(0.35F, state.boundingBoxHeight) * 0.5F * padding;
        poseStack.pushPose();
        poseStack.scale(-horizontalScale, -verticalScale, horizontalScale);
        poseStack.translate(0.0F, -1.5F, 0.0F);

        collector.submitModel(model, Unit.INSTANCE, poseStack, model.renderType(ICE_TEXTURE),
                state.lightCoords, OverlayTexture.NO_OVERLAY, 0xC8FFFFFF, null, state.outlineColor, null);
        poseStack.popPose();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class IceShellGeoLayer extends GeoRenderLayer {
        private IceShellGeoLayer(GeoRenderer renderer) {
            super(renderer);
        }

        @Override
        public void addRenderData(GeoAnimatable animatable, Object relatedObject, GeoRenderState state,
                float partialTick) {
            boolean frozen = animatable instanceof LivingEntity living && living.hasEffect(ModEffects.FROZEN);
            state.addGeckolibData(GEO_FROZEN, frozen);
            if (frozen) state.addGeckolibData(DataTickets.IS_SHAKING, false);
        }

        @Override
        public void submitRenderTask(RenderPassInfo renderPass, SubmitNodeCollector collector) {
            if (model == null || !Boolean.TRUE.equals(renderPass.getGeckolibData(GEO_FROZEN))
                    || !(renderPass.renderState() instanceof LivingEntityRenderState state)
                    || state.isInvisible) {
                return;
            }
            PoseStack poseStack = new PoseStack();
            poseStack.last().set(renderPass.getPreRenderMatrixPose());
            submitShell(state, poseStack, collector.order(1));
        }
    }

    public static void onFrozenEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || !living.level().isClientSide()
                || !living.hasEffect(ModEffects.FROZEN) || living.tickCount % 4 != 0) {
            return;
        }
        double radius = living.getBbWidth() * 0.55D;
        double angle = living.getRandom().nextDouble() * Math.PI * 2.0D;
        double x = living.getX() + Math.cos(angle) * radius;
        double y = living.getY() + living.getRandom().nextDouble() * living.getBbHeight();
        double z = living.getZ() + Math.sin(angle) * radius;
        living.level().addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0.0D, 0.012D, 0.0D);
        if (living.tickCount % 8 == 0) {
            ParticleHelper.spawnSnowflake(living.level(), ParticleHelper.SNOWFLAKE_AURA,
                    x, y, z, 0.0D, 0.01D, 0.0D);
        }
    }

    private IceShellRenderer() {
    }
}
