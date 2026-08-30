package com.somake.wotn.alchemy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record JormungandrEffectState(long expiresAt, int tier, AlchemyPotionConfiguration configuration) {
    public static final JormungandrEffectState INACTIVE = new JormungandrEffectState(
            0L, 1, AlchemyPotionConfiguration.DEFAULT);
    public static final com.mojang.serialization.MapCodec<JormungandrEffectState> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.LONG.optionalFieldOf("expires_at", 0L).forGetter(JormungandrEffectState::expiresAt),
                    Codec.intRange(1, 3).optionalFieldOf("tier", 1).forGetter(JormungandrEffectState::tier),
                    AlchemyPotionConfiguration.CODEC.optionalFieldOf("configuration", AlchemyPotionConfiguration.DEFAULT)
                            .forGetter(JormungandrEffectState::configuration))
                    .apply(instance, JormungandrEffectState::new));
}
