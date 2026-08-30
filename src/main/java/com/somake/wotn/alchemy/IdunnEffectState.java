package com.somake.wotn.alchemy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record IdunnEffectState(long expiresAt, int tier, AlchemyPotionConfiguration configuration,
        boolean emergencyAvailable, long nextPulseAt, int pulseCount) {
    public static final IdunnEffectState INACTIVE = new IdunnEffectState(
            0L, 1, AlchemyPotionConfiguration.DEFAULT, false, 0L, 0);
    public static final com.mojang.serialization.MapCodec<IdunnEffectState> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.LONG.optionalFieldOf("expires_at", 0L).forGetter(IdunnEffectState::expiresAt),
                    Codec.intRange(1, 3).optionalFieldOf("tier", 1).forGetter(IdunnEffectState::tier),
                    AlchemyPotionConfiguration.CODEC.optionalFieldOf("configuration", AlchemyPotionConfiguration.DEFAULT)
                            .forGetter(IdunnEffectState::configuration),
                    Codec.BOOL.optionalFieldOf("emergency_available", false).forGetter(IdunnEffectState::emergencyAvailable),
                    Codec.LONG.optionalFieldOf("next_pulse_at", 0L).forGetter(IdunnEffectState::nextPulseAt),
                    Codec.INT.optionalFieldOf("pulse_count", 0).forGetter(IdunnEffectState::pulseCount))
                    .apply(instance, IdunnEffectState::new));

    public IdunnEffectState consumeEmergency() {
        return new IdunnEffectState(expiresAt, tier, configuration, false, nextPulseAt, pulseCount);
    }

    public IdunnEffectState afterPulse(long nextPulseAt) {
        return new IdunnEffectState(expiresAt, tier, configuration, emergencyAvailable, nextPulseAt, pulseCount + 1);
    }
}
