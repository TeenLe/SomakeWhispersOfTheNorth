package com.somake.wotn.damage;

import com.somake.wotn.WhispersOfTheNorth;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

public final class AlchemyDamage {
    public static final ResourceKey<DamageType> JORMUNGANDR_VENOM = key("jormungandr_venom");
    public static final ResourceKey<DamageType> JORMUNGANDR_RUPTURE = key("jormungandr_rupture");
    public static final ResourceKey<DamageType> CHAINBREAKER = key("chainbreaker");

    public static boolean isMelee(DamageSource source, LivingEntity entity) {
        return source.getEntity() == entity && source.getDirectEntity() == entity
                && !isSecondary(source);
    }

    public static boolean isSecondary(DamageSource source) {
        return source.is(JORMUNGANDR_VENOM) || source.is(JORMUNGANDR_RUPTURE) || source.is(CHAINBREAKER);
    }

    public static DamageSource source(LivingEntity target, ResourceKey<DamageType> type, ServerPlayer owner) {
        return target.level().damageSources().source(type, null, owner);
    }

    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE,
                Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, path));
    }

    private AlchemyDamage() {
    }
}
