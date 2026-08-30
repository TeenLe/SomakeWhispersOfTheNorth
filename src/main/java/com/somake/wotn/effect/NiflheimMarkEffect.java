package com.somake.wotn.effect;

import com.somake.wotn.WhispersOfTheNorth;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class NiflheimMarkEffect extends MobEffect {
    public NiflheimMarkEffect() {
        super(MobEffectCategory.HARMFUL, 0x72D8EA);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "effect.niflheim_mark"),
                -0.10D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
