package com.somake.wotn.client;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.client.particle.ImpactRingParticle;
import com.somake.wotn.client.model.IceShellModel;
import com.somake.wotn.client.renderer.FenrirRenderer;
import com.somake.wotn.client.renderer.FrozenBlockRenderer;
import com.somake.wotn.client.renderer.GolemRenderer;
import com.somake.wotn.client.renderer.GroundWaveRenderer;
import com.somake.wotn.client.renderer.StoneSlimeRenderer;
import com.somake.wotn.client.renderer.LeviathanAxeRenderer;
import com.somake.wotn.client.sound.GroundWaveSoundManager;
import com.somake.wotn.network.CameraShakePayload;
import com.somake.wotn.network.LeviathanAxeCooldownPayload;
import com.somake.wotn.network.LeviathanImbueStatePayload;
import com.somake.wotn.registry.ModEntities;
import com.somake.wotn.registry.ModParticles;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = WhispersOfTheNorth.MODID, dist = Dist.CLIENT)
public final class WotnClient {
    public WotnClient(IEventBus modEventBus) {
        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::registerParticleProviders);
        modEventBus.addListener(this::registerClientPayloadHandlers);
        modEventBus.addListener(this::registerLayerDefinitions);
        modEventBus.addListener(IceShellRenderer::initialize);
        modEventBus.addListener(LeviathanAxeClient::registerKeys);
        modEventBus.addListener(LeviathanAxeClient::registerHud);
        modEventBus.addListener(LeviathanImbueClient::registerKeys);
        modEventBus.addListener(LeviathanImbueClient::registerHud);
        NeoForge.EVENT_BUS.addListener(CameraShakeManager::onClientTick);
        NeoForge.EVENT_BUS.addListener(CameraShakeManager::onCameraAngles);
        NeoForge.EVENT_BUS.addListener(GroundWaveSoundManager::onClientTick);
        NeoForge.EVENT_BUS.addListener(IceShellRenderer::prepare);
        NeoForge.EVENT_BUS.addListener(IceShellRenderer::render);
        NeoForge.EVENT_BUS.addListener(IceShellRenderer::onFrozenEntityTick);
        NeoForge.EVENT_BUS.addListener(LeviathanAxeClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(LeviathanImbueClient::onClientTick);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GOLEM.get(), GolemRenderer::new);
        event.registerEntityRenderer(ModEntities.STONE_SLIME.get(), StoneSlimeRenderer::new);
        event.registerEntityRenderer(ModEntities.GROUND_WAVE.get(), GroundWaveRenderer::new);
        event.registerEntityRenderer(ModEntities.FENRIR.get(), FenrirRenderer::new);
        event.registerEntityRenderer(ModEntities.LEVIATHAN_AXE_PROJECTILE.get(), LeviathanAxeRenderer::new);
        event.registerEntityRenderer(ModEntities.FROZEN_BLOCK.get(), FrozenBlockRenderer::new);
    }

    private void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.IMPACT_RING.get(), ImpactRingParticle.Provider::new);
    }

    private void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(IceShellModel.LAYER_LOCATION, IceShellModel::createBodyLayer);
    }

    private void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(CameraShakePayload.TYPE, (payload, context) -> CameraShakeManager.add(payload));
        event.register(LeviathanAxeCooldownPayload.TYPE, (payload, context) -> LeviathanAxeClient.applyCooldown(payload));
        event.register(LeviathanImbueStatePayload.TYPE, (payload, context) -> LeviathanImbueClient.applyState(payload));
    }
}
