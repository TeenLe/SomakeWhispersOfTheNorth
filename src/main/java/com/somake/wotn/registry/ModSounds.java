package com.somake.wotn.registry;

import com.somake.wotn.WhispersOfTheNorth;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(
            BuiltInRegistries.SOUND_EVENT, WhispersOfTheNorth.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> GROUND_SLAM = SOUND_EVENTS.register(
            "ground_slam",
            () -> SoundEvent.createVariableRangeEvent(
                     Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "ground_slam")));

    public static final DeferredHolder<SoundEvent, SoundEvent> FENRIR_UNDERGROUND_MOVING = SOUND_EVENTS.register(
            "fenrir_underground_moving",
            () -> SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "fenrir_underground_moving")));

    private ModSounds() {
    }
}
