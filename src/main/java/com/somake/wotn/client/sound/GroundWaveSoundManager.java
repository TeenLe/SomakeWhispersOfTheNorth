package com.somake.wotn.client.sound;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.somake.wotn.entity.GroundWaveEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class GroundWaveSoundManager {
    private static final Map<Integer, GroundWaveMovingSound> ACTIVE_SOUNDS = new HashMap<>();

    private GroundWaveSoundManager() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) {
            if (minecraft.level == null) {
                ACTIVE_SOUNDS.clear();
            }
            return;
        }

        Set<Integer> presentWaves = new HashSet<>();
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof GroundWaveEntity wave) || wave.isRemoved() || wave.isSubmerging()) {
                continue;
            }

            presentWaves.add(wave.getId());
            GroundWaveMovingSound existing = ACTIVE_SOUNDS.get(wave.getId());
            if (existing == null || existing.isStopped() || !minecraft.getSoundManager().isActive(existing)) {
                GroundWaveMovingSound sound = new GroundWaveMovingSound(wave);
                ACTIVE_SOUNDS.put(wave.getId(), sound);
                minecraft.getSoundManager().play(sound);
            }
        }

        Iterator<Map.Entry<Integer, GroundWaveMovingSound>> iterator = ACTIVE_SOUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, GroundWaveMovingSound> entry = iterator.next();
            if (!presentWaves.contains(entry.getKey()) || entry.getValue().isStopped()) {
                minecraft.getSoundManager().stop(entry.getValue());
                iterator.remove();
            }
        }
    }
}
