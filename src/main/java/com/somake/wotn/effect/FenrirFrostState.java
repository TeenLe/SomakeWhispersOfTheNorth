package com.somake.wotn.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record FenrirFrostState(int stacks, long stacksExpireAt, boolean awaitingThaw, long immuneUntil) {
    public static final FenrirFrostState EMPTY = new FenrirFrostState(0, 0L, false, 0L);
    public static final MapCodec<FenrirFrostState> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, FrostStackManager.MAX_STACKS).optionalFieldOf("stacks", 0)
                    .forGetter(FenrirFrostState::stacks),
            Codec.LONG.optionalFieldOf("stacks_expire_at", 0L).forGetter(FenrirFrostState::stacksExpireAt),
            Codec.BOOL.optionalFieldOf("awaiting_thaw", false).forGetter(FenrirFrostState::awaitingThaw),
            Codec.LONG.optionalFieldOf("immune_until", 0L).forGetter(FenrirFrostState::immuneUntil))
            .apply(instance, FenrirFrostState::new));
}
