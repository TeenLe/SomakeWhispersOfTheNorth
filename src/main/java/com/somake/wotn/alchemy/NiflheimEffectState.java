package com.somake.wotn.alchemy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record NiflheimEffectState(long expiresAt, int tier, List<AlchemyRune> runes) {
    private static final Codec<List<AlchemyRune>> RUNES_CODEC = Codec.STRING.listOf().xmap(
            ids -> ids.stream().map(AlchemyRune::fromId).filter(java.util.Objects::nonNull)
                    .filter(rune -> rune.family().equals("niflheim")).toList(),
            runes -> runes.stream().map(AlchemyRune::id).toList());
    public static final NiflheimEffectState INACTIVE = new NiflheimEffectState(0L, 1, List.of());
    public static final com.mojang.serialization.MapCodec<NiflheimEffectState> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.LONG.optionalFieldOf("expires_at", 0L).forGetter(NiflheimEffectState::expiresAt),
                    Codec.intRange(1, 3).optionalFieldOf("tier", 1).forGetter(NiflheimEffectState::tier),
                    RUNES_CODEC.optionalFieldOf("runes", List.of()).forGetter(NiflheimEffectState::runes))
                    .apply(instance, NiflheimEffectState::new));

    public AlchemyPotionConfiguration configuration() {
        return new AlchemyPotionConfiguration(runes);
    }

    public AlchemyPotionConfiguration.Special special() {
        return configuration().special("niflheim");
    }

    public int potencyCatalysts() {
        return configuration().potency("niflheim");
    }
}
