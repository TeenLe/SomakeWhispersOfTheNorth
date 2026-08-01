package com.somake.wotn;

import com.somake.wotn.entity.ModEntityEvents;
import com.somake.wotn.network.ModNetworking;
import com.somake.wotn.registry.ModEntities;
import com.somake.wotn.registry.ModItems;
import com.somake.wotn.registry.ModParticles;
import com.somake.wotn.registry.ModSounds;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(WhispersOfTheNorth.MODID)
public class WhispersOfTheNorth {
    public static final String MODID = "wotn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WhispersOfTheNorth(IEventBus modEventBus) {
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        modEventBus.addListener(ModItems::addCreativeTabContents);
        modEventBus.addListener(ModEntityEvents::registerAttributes);
        modEventBus.addListener(ModNetworking::register);

        LOGGER.info("Loading {}", MODID);
    }
}
