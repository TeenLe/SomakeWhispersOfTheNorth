package com.somake.wotn.registry;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.effect.FrozenEffect;
import com.somake.wotn.effect.FenrirFrostbiteEffect;
import com.somake.wotn.effect.AlchemyPotionEffect;
import com.somake.wotn.effect.NiflheimMarkEffect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(
            BuiltInRegistries.MOB_EFFECT, WhispersOfTheNorth.MODID);

    public static final DeferredHolder<MobEffect, FrozenEffect> FROZEN = MOB_EFFECTS.register(
            "frozen", FrozenEffect::new);
    public static final DeferredHolder<MobEffect, FenrirFrostbiteEffect> FENRIR_FROSTBITE = MOB_EFFECTS.register(
            "fenrir_frostbite", FenrirFrostbiteEffect::new);
    public static final DeferredHolder<MobEffect, AlchemyPotionEffect> NIFLHEIM_ESSENCE = MOB_EFFECTS.register(
            "niflheim_essence", () -> new AlchemyPotionEffect(0x64DDF2));
    public static final DeferredHolder<MobEffect, AlchemyPotionEffect> FENRIR_BLOOD = MOB_EFFECTS.register(
            "fenrir_blood", () -> new AlchemyPotionEffect(0xB83232));
    public static final DeferredHolder<MobEffect, AlchemyPotionEffect> IDUNN_ELIXIR = MOB_EFFECTS.register(
            "idunn_elixir", () -> new AlchemyPotionEffect(0xE9BD45));
    public static final DeferredHolder<MobEffect, AlchemyPotionEffect> JORMUNGANDR_VENOM = MOB_EFFECTS.register(
            "jormungandr_venom", () -> new AlchemyPotionEffect(0x5FAE4A));
    public static final DeferredHolder<MobEffect, NiflheimMarkEffect> NIFLHEIM_MARK = MOB_EFFECTS.register(
            "niflheim_mark", NiflheimMarkEffect::new);

    private ModEffects() {
    }
}
