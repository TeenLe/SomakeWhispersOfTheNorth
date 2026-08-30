package com.somake.wotn;

import com.somake.wotn.entity.ModEntityEvents;
import com.somake.wotn.effect.FreezeManager;
import com.somake.wotn.effect.FenrirFrostManager;
import com.somake.wotn.network.ModNetworking;
import com.somake.wotn.registry.ModEntities;
import com.somake.wotn.registry.ModEffects;
import com.somake.wotn.registry.ModItems;
import com.somake.wotn.registry.ModParticles;
import com.somake.wotn.registry.ModSounds;
import com.somake.wotn.registry.ModDataComponents;
import com.somake.wotn.registry.ModBlocks;
import com.somake.wotn.registry.ModBlockEntities;
import com.somake.wotn.registry.ModAttachments;
import com.somake.wotn.registry.ModAttributes;
import com.somake.wotn.registry.ModCreativeTabs;
import com.somake.wotn.registry.ModFeatures;
import com.somake.wotn.skill.LeviathanImbueSkill;
import com.somake.wotn.skill.LeviathanIceSpikesSkill;
import com.somake.wotn.command.WotnCommands;
import com.somake.wotn.dialogue.DialogueManager;
import com.somake.wotn.data.ModDataGenerators;
import com.somake.wotn.skilltree.ForgeSessionManager;
import com.somake.wotn.alchemy.AlchemyManager;
import com.somake.wotn.alchemy.NiflheimPotionEffects;
import com.somake.wotn.alchemy.FenrirPotionEffects;
import com.somake.wotn.alchemy.IdunnPotionEffects;
import com.somake.wotn.alchemy.JormungandrPotionEffects;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(WhispersOfTheNorth.MODID)
public class WhispersOfTheNorth {
    public static final String MODID = "wotn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WhispersOfTheNorth(IEventBus modEventBus) {
        ModAttachments.ATTACHMENTS.register(modEventBus);
        ModAttributes.ATTRIBUTES.register(modEventBus);
        modEventBus.addListener(ModAttributes::addPlayerAttributes);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        ModDataComponents.COMPONENTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        modEventBus.addListener(ModDataGenerators::gatherClientData);
        modEventBus.addListener(ModDataGenerators::gatherServerData);
        modEventBus.addListener(ModBlocks::addBlockEntityTypeBlocks);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        modEventBus.addListener(ModItems::addCreativeTabContents);
        modEventBus.addListener(ModEntityEvents::registerAttributes);
        modEventBus.addListener(ModNetworking::register);
        NeoForge.EVENT_BUS.addListener(FreezeManager::onEntityTickPre);
        NeoForge.EVENT_BUS.addListener(FreezeManager::onEntityTickPost);
        NeoForge.EVENT_BUS.addListener(FenrirFrostManager::onDamageApplied);
        NeoForge.EVENT_BUS.addListener(FenrirFrostManager::onEntityTickPost);
        NeoForge.EVENT_BUS.addListener(FenrirFrostManager::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(FenrirFrostManager::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(LeviathanImbueSkill::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(LeviathanImbueSkill::onDamageApplied);
        NeoForge.EVENT_BUS.addListener(LeviathanIceSpikesSkill::onServerTick);
        NeoForge.EVENT_BUS.addListener(WotnCommands::register);
        NeoForge.EVENT_BUS.addListener(DialogueManager::registerReloadListener);
        NeoForge.EVENT_BUS.addListener(AlchemyManager::registerReloadListener);
        NeoForge.EVENT_BUS.addListener(DialogueManager::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(ForgeSessionManager::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(AlchemyManager::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(AlchemyManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(NiflheimPotionEffects::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(NiflheimPotionEffects::onDamageApplied);
        NeoForge.EVENT_BUS.addListener(NiflheimPotionEffects::onServerTick);
        NeoForge.EVENT_BUS.addListener(NiflheimPotionEffects::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(NiflheimPotionEffects::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(FenrirPotionEffects::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(FenrirPotionEffects::onDamageApplied);
        NeoForge.EVENT_BUS.addListener(FenrirPotionEffects::onDeath);
        NeoForge.EVENT_BUS.addListener(FenrirPotionEffects::onServerTick);
        NeoForge.EVENT_BUS.addListener(FenrirPotionEffects::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(FenrirPotionEffects::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(IdunnPotionEffects::onHeal);
        NeoForge.EVENT_BUS.addListener(IdunnPotionEffects::onDamagePre);
        NeoForge.EVENT_BUS.addListener(IdunnPotionEffects::onDamageApplied);
        NeoForge.EVENT_BUS.addListener(IdunnPotionEffects::onServerTick);
        NeoForge.EVENT_BUS.addListener(IdunnPotionEffects::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(IdunnPotionEffects::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(JormungandrPotionEffects::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(JormungandrPotionEffects::onDamageApplied);
        NeoForge.EVENT_BUS.addListener(JormungandrPotionEffects::onDeath);
        NeoForge.EVENT_BUS.addListener(JormungandrPotionEffects::onServerTick);
        NeoForge.EVENT_BUS.addListener(JormungandrPotionEffects::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(JormungandrPotionEffects::onPlayerClone);

        LOGGER.info("Loading {}", MODID);
    }
}
