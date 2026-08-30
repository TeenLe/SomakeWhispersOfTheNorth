package com.somake.wotn.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class AlchemyPotionEffect extends MobEffect {
    public AlchemyPotionEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }
}
