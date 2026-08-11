package com.somake.wotn.skilltree;

import com.somake.wotn.registry.ModDataComponents;
import com.somake.wotn.registry.ModItems;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class WeaponSkillData {
    public static boolean isEligible(ItemStack stack) {
        return stack.is(ModItems.LEVIATHAN_AXE.get());
    }

    public static WeaponSkillProgress progress(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.WEAPON_SKILL_PROGRESS.get(), WeaponSkillProgress.DEFAULT);
    }

    public static void setProgress(ItemStack stack, WeaponSkillProgress progress) {
        stack.set(ModDataComponents.WEAPON_SKILL_PROGRESS.get(), progress);
    }

    public static UUID ensureIdentity(ItemStack stack) {
        UUID identity = stack.get(ModDataComponents.WEAPON_INSTANCE_ID.get());
        if (identity == null) {
            identity = UUID.randomUUID();
            stack.set(ModDataComponents.WEAPON_INSTANCE_ID.get(), identity);
        }
        return identity;
    }

    public static Optional<LocatedWeapon> find(ServerPlayer player, UUID identity) {
        ensureUniqueIdentities(player);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isEligible(stack) && identity.equals(stack.get(ModDataComponents.WEAPON_INSTANCE_ID.get()))) {
                return Optional.of(new LocatedWeapon(slot, stack));
            }
        }
        return Optional.empty();
    }

    public static void ensureUniqueIdentities(ServerPlayer player) {
        Set<UUID> seen = new HashSet<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!isEligible(stack)) continue;
            UUID identity = ensureIdentity(stack);
            if (!seen.add(identity)) {
                stack.set(ModDataComponents.WEAPON_INSTANCE_ID.get(), UUID.randomUUID());
                seen.add(stack.get(ModDataComponents.WEAPON_INSTANCE_ID.get()));
            }
        }
    }

    public record LocatedWeapon(int inventorySlot, ItemStack stack) {
    }

    private WeaponSkillData() {
    }
}
