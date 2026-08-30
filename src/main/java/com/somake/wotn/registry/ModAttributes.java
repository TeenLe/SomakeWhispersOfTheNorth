package com.somake.wotn.registry;

import com.somake.wotn.WhispersOfTheNorth;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(
            Registries.ATTRIBUTE, WhispersOfTheNorth.MODID);

    public static final Holder<Attribute> ICE_DAMAGE_BONUS = ATTRIBUTES.register(
            "ice_damage_bonus", () -> new RangedAttribute(
                    "attribute.name.wotn.ice_damage_bonus", 0.0D, -1.0D, 10.0D)
                    .setSyncable(true)
                    .setSentiment(Attribute.Sentiment.POSITIVE));

    public static final Holder<Attribute> FIRE_DAMAGE_RESISTANCE = ATTRIBUTES.register(
            "fire_damage_resistance", () -> new RangedAttribute(
                    "attribute.name.wotn.fire_damage_resistance", 0.0D, 0.0D, 0.8D)
                    .setSyncable(true)
                    .setSentiment(Attribute.Sentiment.POSITIVE));

    public static final Holder<Attribute> HEALING_RECEIVED_BONUS = ATTRIBUTES.register(
            "healing_received_bonus", () -> new RangedAttribute(
                    "attribute.name.wotn.healing_received_bonus", 0.0D, 0.0D, 10.0D)
                    .setSyncable(true)
                    .setSentiment(Attribute.Sentiment.POSITIVE));

    public static final Holder<Attribute> VENOM_DAMAGE_BONUS = ATTRIBUTES.register(
            "venom_damage_bonus", () -> new RangedAttribute(
                    "attribute.name.wotn.venom_damage_bonus", 0.0D, 0.0D, 10.0D)
                    .setSyncable(true)
                    .setSentiment(Attribute.Sentiment.POSITIVE));

    public static final Holder<Attribute> POISON_RESISTANCE = ATTRIBUTES.register(
            "poison_resistance", () -> new RangedAttribute(
                    "attribute.name.wotn.poison_resistance", 0.0D, 0.0D, 0.8D)
                    .setSyncable(true)
                    .setSentiment(Attribute.Sentiment.POSITIVE));

    public static void addPlayerAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ICE_DAMAGE_BONUS);
        event.add(EntityType.PLAYER, FIRE_DAMAGE_RESISTANCE);
        event.add(EntityType.PLAYER, HEALING_RECEIVED_BONUS);
        event.add(EntityType.PLAYER, VENOM_DAMAGE_BONUS);
        event.add(EntityType.PLAYER, POISON_RESISTANCE);
    }

    private ModAttributes() {
    }
}
