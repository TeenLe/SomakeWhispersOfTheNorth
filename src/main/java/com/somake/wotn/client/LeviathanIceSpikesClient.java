package com.somake.wotn.client;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.network.LeviathanIceSpikesCooldownPayload;
import com.somake.wotn.registry.ModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class LeviathanIceSpikesClient {
    private static final Identifier HUD_LAYER = Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "ice_spikes_hud");
    private static final Identifier ICON = Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "textures/gui/ice_spikes.png");
    private static int remainingTicks;
    private static int totalTicks = 1;
    private static int deniedPulseTicks;
    private static int readyPulseTicks;
    private static boolean wasCoolingDown;

    public static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, HUD_LAYER, LeviathanIceSpikesClient::renderHud);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            remainingTicks = deniedPulseTicks = readyPulseTicks = 0;
            totalTicks = 1;
            wasCoolingDown = false;
            return;
        }
        if (remainingTicks > 0 && !minecraft.isPaused()) remainingTicks--;
        boolean cooling = remainingTicks > 0;
        if (wasCoolingDown && !cooling) {
            readyPulseTicks = 14;
            if (minecraft.player != null) minecraft.player.playSound(
                    net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 0.25F, 1.8F);
        }
        wasCoolingDown = cooling;
        if (deniedPulseTicks > 0) deniedPulseTicks--;
        if (readyPulseTicks > 0) readyPulseTicks--;
    }

    public static void applyCooldown(LeviathanIceSpikesCooldownPayload payload) {
        remainingTicks = Math.max(0, payload.remainingTicks());
        totalTicks = Math.max(1, payload.totalTicks());
        deniedPulseTicks = payload.denied() ? 14 : 0;
        wasCoolingDown = remainingTicks > 0;
    }

    private static boolean isHoldingAxe() {
        var player = Minecraft.getInstance().player;
        return player != null && (player.getMainHandItem().is(ModItems.LEVIATHAN_AXE.get())
                || player.getOffhandItem().is(ModItems.LEVIATHAN_AXE.get()));
    }

    private static void renderHud(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || !LeviathanSkillSelection.isEquipped(LeviathanSkillSelection.ICE_SPIKES)
                || (!isHoldingAxe() && remainingTicks <= 0)) return;
        int size = 20;
        int slot = LeviathanSkillSelection.slotForSkill(LeviathanSkillSelection.ICE_SPIKES);
        int x = graphics.guiWidth() / 2 + (slot == LeviathanSkillSelection.SLOT_ONE ? 102 : 130);
        int y = graphics.guiHeight() - 24;
        float denied = deniedPulseTicks > 0 ? 0.5F + 0.5F * Mth.sin((14 - deniedPulseTicks) * 0.58F) : 0;
        float ready = readyPulseTicks > 0 ? 0.5F + 0.5F * Mth.sin((14 - readyPulseTicks) * 0.45F) : 0;
        int border = deniedPulseTicks > 0 ? 0xFFFF4949 : readyPulseTicks > 0 ? 0xFFBFFBFF
                : remainingTicks > 0 ? 0xFF426B78 : 0xFF79E9FF;
        graphics.fill(x - 3, y - 3, x + size + 3, y + size + 3, 0xB0101D24);
        graphics.outline(x - 2, y - 2, size + 4, size + 4, border);
        if (denied > 0) graphics.fill(x - 4, y - 4, x + size + 4, y + size + 4,
                ((int) (denied * 105) << 24) | 0xFF3030);
        else if (ready > 0) graphics.fill(x - 4, y - 4, x + size + 4, y + size + 4,
                ((int) (ready * 80) << 24) | 0x8DEBFF);
        graphics.blit(RenderPipelines.GUI_TEXTURED, ICON, x, y, 0, 0, size, size, 16, 16, 16, 16);
        float fraction = Mth.clamp((remainingTicks - deltaTracker.getGameTimeDeltaPartialTick(true))
                / (float) totalTicks, 0, 1);
        if (fraction > 0) {
            int top = y + Mth.floor(size * (1 - fraction));
            graphics.fill(x, top, x + size, y + size, 0xA0183440);
            Component seconds = Component.literal(Integer.toString(Mth.ceil(remainingTicks / 20.0F)));
            graphics.text(minecraft.font, seconds, x + size / 2 - minecraft.font.width(seconds) / 2,
                    y + size / 2 - 4, 0xFFFFFFFF, true);
        }
        Component key = LeviathanSkillSelection.keyMessageForSkill(LeviathanSkillSelection.ICE_SPIKES);
        graphics.text(minecraft.font, key, x + size - minecraft.font.width(key), y - 10, 0xFFBFEFFF, true);
    }
    private LeviathanIceSpikesClient() {}
}
