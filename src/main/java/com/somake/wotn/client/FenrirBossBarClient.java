package com.somake.wotn.client;

import com.somake.wotn.WhispersOfTheNorth;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

public final class FenrirBossBarClient {
    public static final String BOSS_BAR_TRANSLATION_KEY = "boss.wotn.fenrir";
    private static final Identifier HEALTH_EMPTY_TEXTURE = Identifier.fromNamespaceAndPath(
            WhispersOfTheNorth.MODID, "textures/gui/boss_bar/fenrir_health_empty.png");
    private static final Identifier HEALTH_FULL_TEXTURE = Identifier.fromNamespaceAndPath(
            WhispersOfTheNorth.MODID, "textures/gui/boss_bar/fenrir_health_full.png");
    private static final Identifier OVERLAY_TEXTURE = Identifier.fromNamespaceAndPath(
            WhispersOfTheNorth.MODID, "textures/gui/boss_bar/fenrir_overlay.png");
    private static final int VANILLA_BAR_WIDTH = 182;
    private static final int TEXTURE_WIDTH = 252;
    private static final int TEXTURE_HEIGHT = 30;
    private static final int TEXTURE_X_OFFSET = (TEXTURE_WIDTH - VANILLA_BAR_WIDTH) / 2;
    private static final int TEXTURE_Y_OFFSET = 11;
    private static final int HEALTH_X = 5;
    private static final int HEALTH_Y = 13;
    private static final int HEALTH_WIDTH = 241;
    private static final int HEALTH_BAR_HEIGHT = 5;
    private static final int BOSS_BAR_INCREMENT = 36;

    private FenrirBossBarClient() {
    }

    public static void render(CustomizeGuiOverlayEvent.BossEventProgress event) {
        Component name = event.getBossEvent().getName();
        if (!(name.getContents() instanceof TranslatableContents contents)
                || !BOSS_BAR_TRANSLATION_KEY.equals(contents.getKey())) {
            return;
        }

        event.setCanceled(true);
        event.setIncrement(BOSS_BAR_INCREMENT);

        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        int barX = event.getX();
        int barY = event.getY();
        int textureX = barX - TEXTURE_X_OFFSET;
        int textureY = barY - TEXTURE_Y_OFFSET;
        int progressWidth = Mth.clamp(
                Mth.lerpDiscrete(event.getBossEvent().getProgress(), 0, HEALTH_WIDTH), 0, HEALTH_WIDTH);

        graphics.blit(RenderPipelines.GUI_TEXTURED, HEALTH_EMPTY_TEXTURE,
                textureX, textureY, 0.0F, 0.0F,
                TEXTURE_WIDTH, TEXTURE_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
        if (progressWidth > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, HEALTH_FULL_TEXTURE,
                    textureX + HEALTH_X, textureY + HEALTH_Y, HEALTH_X, HEALTH_Y,
                    progressWidth, HEALTH_BAR_HEIGHT,
                    progressWidth, HEALTH_BAR_HEIGHT,
                    TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, OVERLAY_TEXTURE,
                textureX, textureY, 0.0F, 0.0F,
                TEXTURE_WIDTH, TEXTURE_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);

    }
}
