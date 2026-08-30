package com.somake.wotn.alchemy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.UUIDUtil;

public record FenrirEffectState(long expiresAt, int tier, AlchemyPotionConfiguration configuration,
        Optional<UUID> preyId, long preyExpiresAt, int fury, long furyExpiresAt, long chainbreakerReadyAt) {
    public static final FenrirEffectState INACTIVE = new FenrirEffectState(
            0L, 1, AlchemyPotionConfiguration.DEFAULT, Optional.empty(), 0L, 0, 0L, 0L);
    public static final com.mojang.serialization.MapCodec<FenrirEffectState> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.LONG.optionalFieldOf("expires_at", 0L).forGetter(FenrirEffectState::expiresAt),
                    Codec.intRange(1, 3).optionalFieldOf("tier", 1).forGetter(FenrirEffectState::tier),
                    AlchemyPotionConfiguration.CODEC.optionalFieldOf("configuration", AlchemyPotionConfiguration.DEFAULT)
                            .forGetter(FenrirEffectState::configuration),
                    UUIDUtil.CODEC.optionalFieldOf("prey").forGetter(FenrirEffectState::preyId),
                    Codec.LONG.optionalFieldOf("prey_expires_at", 0L).forGetter(FenrirEffectState::preyExpiresAt),
                    Codec.intRange(0, 5).optionalFieldOf("fury", 0).forGetter(FenrirEffectState::fury),
                    Codec.LONG.optionalFieldOf("fury_expires_at", 0L).forGetter(FenrirEffectState::furyExpiresAt),
                    Codec.LONG.optionalFieldOf("chainbreaker_ready_at", 0L).forGetter(FenrirEffectState::chainbreakerReadyAt))
                    .apply(instance, FenrirEffectState::new));

    public FenrirEffectState withPrey(UUID prey, long expiresAt) {
        return new FenrirEffectState(this.expiresAt, tier, configuration, Optional.of(prey), expiresAt,
                fury, furyExpiresAt, chainbreakerReadyAt);
    }

    public FenrirEffectState withoutPrey() {
        return new FenrirEffectState(expiresAt, tier, configuration, Optional.empty(), 0L,
                fury, furyExpiresAt, chainbreakerReadyAt);
    }

    public FenrirEffectState withFury(int fury, long expiresAt) {
        return new FenrirEffectState(this.expiresAt, tier, configuration, preyId, preyExpiresAt,
                Math.clamp(fury, 0, 5), expiresAt, chainbreakerReadyAt);
    }

    public FenrirEffectState consumeFury(long readyAt) {
        return new FenrirEffectState(expiresAt, tier, configuration, preyId, preyExpiresAt,
                0, 0L, readyAt);
    }
}
