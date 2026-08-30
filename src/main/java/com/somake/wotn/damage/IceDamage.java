package com.somake.wotn.damage;

import com.somake.wotn.entity.IceSpikeEntity;
import com.somake.wotn.entity.LeviathanAxeEntity;
import com.somake.wotn.registry.ModItems;
import com.somake.wotn.skill.LeviathanImbueSkill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public final class IceDamage {
    public static ServerPlayer owner(DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return null;
        if (source.getDirectEntity() instanceof IceSpikeEntity) return player;
        if (source.getDirectEntity() instanceof LeviathanAxeEntity axe
                && LeviathanImbueSkill.isActive(axe.getItem(), player.level())) {
            return player;
        }
        var weapon = source.getWeaponItem();
        if (source.getDirectEntity() == player && weapon != null && weapon.is(ModItems.LEVIATHAN_AXE.get())
                && LeviathanImbueSkill.isActive(weapon, player.level())) return player;
        return null;
    }

    public static boolean isIceDamage(DamageSource source) {
        return owner(source) != null;
    }

    private IceDamage() {
    }
}
