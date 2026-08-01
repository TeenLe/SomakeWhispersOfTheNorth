package com.somake.wotn.client.sound;

import com.somake.wotn.entity.GroundWaveEntity;
import com.somake.wotn.registry.ModSounds;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class GroundWaveMovingSound extends AbstractTickableSoundInstance {
    private final GroundWaveEntity wave;

    public GroundWaveMovingSound(GroundWaveEntity wave) {
        super(ModSounds.FENRIR_UNDERGROUND_MOVING.get(), SoundSource.HOSTILE,
                RandomSource.create(wave.getVariantSeed()));
        this.wave = wave;
        this.looping = true;
        this.delay = 0;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.volume = 0.0F;
        this.pitch = 1.0F;
        this.x = wave.getX();
        this.y = wave.getY();
        this.z = wave.getZ();
    }

    @Override
    public boolean canPlaySound() {
        return !this.wave.isRemoved() && !this.wave.isSilent();
    }

    @Override
    public void tick() {
        if (this.wave.isRemoved() || this.wave.isSubmerging()) {
            this.stop();
            return;
        }

        this.x = this.wave.getX();
        this.y = this.wave.getY();
        this.z = this.wave.getZ();
        float intensity = this.wave.getVisualIntensity();
        this.volume = 0.28F + intensity * 0.72F;
        this.pitch = 0.84F + this.wave.getNormalizedSpeed() * 0.14F;
        if (this.wave.isAnticipatingEmergence()) {
            this.volume = Math.max(this.volume, 0.9F);
            this.pitch = 0.76F;
        }
    }
}
