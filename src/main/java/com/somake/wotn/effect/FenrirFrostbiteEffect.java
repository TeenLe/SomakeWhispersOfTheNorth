package com.somake.wotn.effect;

import com.somake.wotn.WhispersOfTheNorth;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class FenrirFrostbiteEffect extends MobEffect {
    public FenrirFrostbiteEffect() {
        super(MobEffectCategory.HARMFUL, 0x8DEBFF);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "effect.fenrir_frostbite"),
                -0.10D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
