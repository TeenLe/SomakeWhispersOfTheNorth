package com.somake.wotn.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

public final class ClientTooltipDivider implements ClientTooltipComponent {
    private final int color;

    public ClientTooltipDivider(TooltipDivider divider) {
        this.color = divider.color();
    }

    @Override
    public int getHeight(Font font) {
        return 7;
    }

    @Override
    public int getWidth(Font font) {
        return 0;
    }

    @Override
    public void extractImage(Font font, int x, int y, int tooltipWidth, int tooltipHeight,
            GuiGraphicsExtractor graphics) {
        graphics.fill(x + 1, y + 2, x + tooltipWidth - 1, y + 3, 0x30000000 | color);
        graphics.fill(x, y + 3, x + tooltipWidth, y + 4, 0xFF000000 | color);
        graphics.fill(x + 1, y + 4, x + tooltipWidth - 1, y + 5, 0x30000000 | color);
    }
}
