package com.somake.wotn.client;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.network.LeviathanAxeCooldownPayload;
import com.somake.wotn.registry.ModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class LeviathanAxeClient {
    private static final Identifier HUD_LAYER = Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "leviathan_axe_hud");
    private static final Identifier ICON = Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "textures/gui/throw.png");
    private static int remainingTicks;
    private static int totalTicks = 1;
    private static int readyPulseTicks;
    private static int deniedPulseTicks;
    private static boolean wasCoolingDown;
    private static boolean hadLevel;

    public static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, HUD_LAYER, LeviathanAxeClient::renderHud);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            if (hadLevel) {
                remainingTicks = 0;
                totalTicks = 1;
                readyPulseTicks = 0;
                deniedPulseTicks = 0;
                wasCoolingDown = false;
            }
            hadLevel = false;
            return;
        }
        hadLevel = true;
        if (remainingTicks > 0 && !minecraft.isPaused()) {
            remainingTicks--;
        }
        boolean coolingDown = remainingTicks > 0;
        if (wasCoolingDown && !coolingDown) {
            readyPulseTicks = 14;
            if (minecraft.player != null) {
                minecraft.player.playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 0.25F, 1.8F);
            }
        }
        wasCoolingDown = coolingDown;
        if (readyPulseTicks > 0) {
            readyPulseTicks--;
        }
        if (deniedPulseTicks > 0) {
            deniedPulseTicks--;
        }
    }

    public static void applyCooldown(LeviathanAxeCooldownPayload payload) {
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
        if (minecraft.options.hideGui || (!LeviathanSkillSelection.isEquipped(LeviathanSkillSelection.THROW)
                && remainingTicks <= 0) || (!isHoldingAxe() && remainingTicks <= 0)) {
            return;
        }

        int size = 20;
        int slot = LeviathanSkillSelection.slotForSkill(LeviathanSkillSelection.THROW);
        int x = graphics.guiWidth() / 2 + (slot == LeviathanSkillSelection.SLOT_ONE ? 102 : 130);
        int y = graphics.guiHeight() - 24;
        float pulse = readyPulseTicks > 0 ? Mth.sin((14 - readyPulseTicks) * 0.45F) * 0.5F + 0.5F : 0.0F;
        float deniedPulse = deniedPulseTicks > 0
                ? Mth.sin((14 - deniedPulseTicks) * 0.58F) * 0.5F + 0.5F
                : 0.0F;
        int border = deniedPulseTicks > 0
                ? 0xFFFF4A4A
                : readyPulseTicks > 0 ? 0xFFBFFBFF : remainingTicks > 0 ? 0xFF426B78 : 0xFF79E9FF;
        graphics.fill(x - 3, y - 3, x + size + 3, y + size + 3, 0xB0101D24);
        graphics.outline(x - 2, y - 2, size + 4, size + 4, border);
        if (deniedPulse > 0.0F) {
            int alpha = (int) (deniedPulse * 105.0F) << 24;
            graphics.fill(x - 4, y - 4, x + size + 4, y + size + 4, alpha | 0xFF3030);
        } else if (pulse > 0.0F) {
            int alpha = (int) (pulse * 80.0F) << 24;
            graphics.fill(x - 4, y - 4, x + size + 4, y + size + 4, alpha | 0x8DEBFF);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, ICON, x, y, 0.0F, 0.0F,
                size, size, 16, 16, 16, 16);

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        float fraction = Mth.clamp((remainingTicks - partialTick) / (float) totalTicks, 0.0F, 1.0F);
        if (fraction > 0.0F) {
            int top = y + Mth.floor(size * (1.0F - fraction));
            graphics.fill(x, top, x + size, y + size, 0xA0183440);
            int seconds = Mth.ceil(remainingTicks / 20.0F);
            Component text = Component.literal(Integer.toString(seconds));
            int textX = x + size / 2 - minecraft.font.width(text) / 2;
            int textY = y + size / 2 - 4;
            graphics.text(minecraft.font, text, textX, textY, 0xFFFFFFFF, true);
        }
        Component key = LeviathanSkillSelection.keyMessageForSkill(LeviathanSkillSelection.THROW);
        graphics.text(minecraft.font, key, x + size - minecraft.font.width(key), y - 10, 0xFFBFEFFF, true);
    }

    private LeviathanAxeClient() {
    }
}
