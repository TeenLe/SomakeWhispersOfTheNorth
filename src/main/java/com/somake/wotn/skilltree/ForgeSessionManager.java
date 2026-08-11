package com.somake.wotn.skilltree;

import com.somake.wotn.network.ForgeWeaponSnapshot;
import com.somake.wotn.network.OpenLeviathanSkillsPayload;
import com.somake.wotn.network.UpdateForgeSessionPayload;
import com.somake.wotn.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ForgeSessionManager {
    public static final ForgeSessionManager INSTANCE = new ForgeSessionManager();
    private final Map<UUID, Session> sessions = new HashMap<>();

    public void open(ServerPlayer player) {
        WeaponSkillData.ensureUniqueIdentities(player);
        List<ForgeWeaponSnapshot> weapons = snapshots(player);
        UUID selected = chooseInitial(player, weapons);
        Session session = new Session(UUID.randomUUID(), selected);
        this.sessions.put(player.getUUID(), session);
        PacketDistributor.sendToPlayer(player, new OpenLeviathanSkillsPayload(session.id(), weapons, selected));
    }

    public void select(ServerPlayer player, UUID sessionId, UUID weaponId) {
        Session session = validSession(player, sessionId);
        if (session == null || WeaponSkillData.find(player, weaponId).isEmpty()) return;
        session.selectedWeaponId = weaponId;
        sync(player, session);
    }

    public void unlock(ServerPlayer player, UUID sessionId, int nodeId) {
        Session session = validSession(player, sessionId);
        if (session == null) return;
        ItemStack stack = selectedStack(player, session).orElse(null);
        LeviathanSkillTree.Node node = LeviathanSkillTree.byId(nodeId);
        if (stack == null || node == null || node.cost() <= 0 || !node.implemented()) return;
        WeaponSkillProgress progress = WeaponSkillData.progress(stack);
        if (node.canUnlock(progress)) {
            WeaponSkillData.setProgress(stack, progress.unlock(node));
            sync(player, session);
        }
    }

    public void equip(ServerPlayer player, UUID sessionId, int slot, int skillId) {
        Session session = validSession(player, sessionId);
        if (session == null || (slot != 1 && slot != 2) || skillId < 1 || skillId > 3) return;
        ItemStack stack = selectedStack(player, session).orElse(null);
        if (stack == null) return;
        int nodeId = switch (skillId) {
            case 1 -> LeviathanSkillTree.IMBUE;
            case 2 -> LeviathanSkillTree.ICE_SPIKES;
            default -> LeviathanSkillTree.THROW;
        };
        if (!WeaponSkillData.progress(stack).isUnlocked(nodeId)) return;
        var target = slot == 1 ? ModDataComponents.LEVIATHAN_PRIMARY_SKILL.get()
                : ModDataComponents.LEVIATHAN_SECONDARY_SKILL.get();
        var other = slot == 1 ? ModDataComponents.LEVIATHAN_SECONDARY_SKILL.get()
                : ModDataComponents.LEVIATHAN_PRIMARY_SKILL.get();
        stack.set(target, skillId);
        if (stack.getOrDefault(other, 0) == skillId) stack.set(other, 0);
        sync(player, session);
    }

    public void close(ServerPlayer player, UUID sessionId) {
        Session session = validSession(player, sessionId);
        if (session != null) this.sessions.remove(player.getUUID());
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        INSTANCE.sessions.remove(event.getEntity().getUUID());
    }

    private void sync(ServerPlayer player, Session session) {
        List<ForgeWeaponSnapshot> weapons = snapshots(player);
        if (session.selectedWeaponId != null && weapons.stream()
                .noneMatch(weapon -> weapon.weaponId().equals(session.selectedWeaponId))) {
            session.selectedWeaponId = chooseInitial(player, weapons);
        }
        PacketDistributor.sendToPlayer(player,
                new UpdateForgeSessionPayload(session.id(), weapons, session.selectedWeaponId));
    }

    private Session validSession(ServerPlayer player, UUID sessionId) {
        Session session = this.sessions.get(player.getUUID());
        return session != null && session.id().equals(sessionId) ? session : null;
    }

    private Optional<ItemStack> selectedStack(ServerPlayer player, Session session) {
        return session.selectedWeaponId == null ? Optional.empty()
                : WeaponSkillData.find(player, session.selectedWeaponId).map(WeaponSkillData.LocatedWeapon::stack);
    }

    private List<ForgeWeaponSnapshot> snapshots(ServerPlayer player) {
        WeaponSkillData.ensureUniqueIdentities(player);
        List<ForgeWeaponSnapshot> result = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!WeaponSkillData.isEligible(stack)) continue;
            UUID id = WeaponSkillData.ensureIdentity(stack);
            result.add(new ForgeWeaponSnapshot(id, slot, stack.copy(), WeaponSkillData.progress(stack),
                    stack.getOrDefault(ModDataComponents.LEVIATHAN_PRIMARY_SKILL.get(), 3),
                    stack.getOrDefault(ModDataComponents.LEVIATHAN_SECONDARY_SKILL.get(), 0)));
        }
        return List.copyOf(result);
    }

    private UUID chooseInitial(ServerPlayer player, List<ForgeWeaponSnapshot> weapons) {
        if (weapons.isEmpty()) return null;
        ItemStack main = player.getMainHandItem();
        if (WeaponSkillData.isEligible(main)) {
            UUID mainId = main.get(ModDataComponents.WEAPON_INSTANCE_ID.get());
            if (mainId != null) return mainId;
        }
        ItemStack off = player.getOffhandItem();
        if (WeaponSkillData.isEligible(off)) {
            UUID offId = off.get(ModDataComponents.WEAPON_INSTANCE_ID.get());
            if (offId != null) return offId;
        }
        return weapons.getFirst().weaponId();
    }

    private static final class Session {
        private final UUID id;
        private UUID selectedWeaponId;
        private Session(UUID id, UUID selectedWeaponId) {
            this.id = id;
            this.selectedWeaponId = selectedWeaponId;
        }
        UUID id() { return id; }
    }

    private ForgeSessionManager() {
    }
}
