package com.somake.wotn.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.network.ActivateLeviathanSlotPayload;
import com.somake.wotn.registry.ModDataComponents;
import com.somake.wotn.registry.ModItems;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class LeviathanSkillSelection {
    public static final int EMPTY = 0;
    public static final int IMBUE = 1;
    public static final int ICE_SPIKES = 2;
    public static final int THROW = 3;
    public static final int SLOT_ONE = 1;
    public static final int SLOT_TWO = 2;

    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "skills"));
    private static final KeyMapping ACTIVATE_SLOT_ONE = new KeyMapping(
            "key.wotn.leviathan_skill_slot_one", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
    private static final KeyMapping ACTIVATE_SLOT_TWO = new KeyMapping(
            "key.wotn.leviathan_skill_slot_two", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    private static int cachedSlotOne = THROW;
    private static int cachedSlotTwo = EMPTY;

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(ACTIVATE_SLOT_ONE);
        event.register(ACTIVATE_SLOT_TWO);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (ACTIVATE_SLOT_ONE.consumeClick()) activate(minecraft, SLOT_ONE);
        while (ACTIVATE_SLOT_TWO.consumeClick()) activate(minecraft, SLOT_TWO);
    }

    private static void activate(Minecraft minecraft, int slot) {
        if (minecraft.player == null || minecraft.getConnection() == null || findAxe().isEmpty()) return;
        ClientPacketDistributor.sendToServer(new ActivateLeviathanSlotPayload(slot));
    }

    public static Component keyMessage(int slot) {
        return slot == SLOT_ONE ? ACTIVATE_SLOT_ONE.getTranslatedKeyMessage()
                : ACTIVATE_SLOT_TWO.getTranslatedKeyMessage();
    }

    public static Component keyMessageForSkill(int skillId) {
        int slot = slotForSkill(skillId);
        return slot == 0 ? Component.empty() : keyMessage(slot);
    }

    public static int equippedSkill(int slot) {
        ItemStack axe = findAxe();
        if (axe.isEmpty()) return slot == SLOT_ONE ? cachedSlotOne : cachedSlotTwo;
        cachedSlotOne = normalize(axe.getOrDefault(ModDataComponents.LEVIATHAN_PRIMARY_SKILL.get(), THROW));
        cachedSlotTwo = normalize(axe.getOrDefault(ModDataComponents.LEVIATHAN_SECONDARY_SKILL.get(), EMPTY));
        return slot == SLOT_ONE ? cachedSlotOne : cachedSlotTwo;
    }

    private static int normalize(int skill) {
        return skill >= IMBUE && skill <= THROW ? skill : EMPTY;
    }

    public static boolean isEquipped(int skillId) {
        return slotForSkill(skillId) != 0;
    }

    public static int slotForSkill(int skillId) {
        if (equippedSkill(SLOT_ONE) == skillId) return SLOT_ONE;
        if (equippedSkill(SLOT_TWO) == skillId) return SLOT_TWO;
        return 0;
    }

    private static ItemStack findAxe() {
        var player = Minecraft.getInstance().player;
        if (player == null) return ItemStack.EMPTY;
        if (player.getMainHandItem().is(ModItems.LEVIATHAN_AXE.get())) return player.getMainHandItem();
        return player.getOffhandItem().is(ModItems.LEVIATHAN_AXE.get())
                ? player.getOffhandItem() : ItemStack.EMPTY;
    }

    private LeviathanSkillSelection() {
    }
}
