package com.somake.wotn.client;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.client.particle.ImpactRingParticle;
import com.somake.wotn.client.renderer.GolemRenderer;
import com.somake.wotn.network.CameraShakePayload;
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
        NeoForge.EVENT_BUS.addListener(CameraShakeManager::onClientTick);
        NeoForge.EVENT_BUS.addListener(CameraShakeManager::onCameraAngles);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GOLEM.get(), GolemRenderer::new);
    }

    private void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.IMPACT_RING.get(), ImpactRingParticle.Provider::new);
    }

    private void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(CameraShakePayload.TYPE, (payload, context) -> CameraShakeManager.add(payload));
    }
}
