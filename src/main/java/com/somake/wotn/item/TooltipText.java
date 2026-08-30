package com.somake.wotn.item;

import com.somake.wotn.client.tooltip.TooltipDivider;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public final class TooltipText {
    public static Component divider() {
        return divider("niflheim");
    }

    public static Component heading(String key) {
        return heading(key, "niflheim");
    }

    public static Component divider(String family) {
        return Component.literal("+------------------------+").withColor(accent(family));
    }

    public static TooltipComponent dividerComponent() {
        return dividerComponent("niflheim");
    }

    public static TooltipComponent dividerComponent(String family) {
        return new TooltipDivider(accent(family));
    }

    public static Component heading(String key, String family) {
        return Component.literal("✦ ").withColor(accent(family))
                .append(Component.translatable(key).withColor(text(family))
                        .withStyle(style -> style.withBold(true)));
    }

    public static int accent(String family) {
        return switch (family) {
            case "jormungandr" -> 0x5ED785;
            case "fenrir" -> 0x628DDD;
            case "idunn" -> 0xEFA852;
            default -> 0x8DEBFF;
        };
    }

    public static int text(String family) {
        return switch (family) {
            case "jormungandr" -> 0xE7F5B4;
            case "fenrir" -> 0xD1E3FF;
            case "idunn" -> 0xFFE6AD;
            default -> 0xDDFBFF;
        };
    }

    public static int muted(String family) {
        return switch (family) {
            case "jormungandr" -> 0x91AA77;
            case "fenrir" -> 0x8199BE;
            case "idunn" -> 0xB29468;
            default -> 0x839E99;
        };
    }

    private TooltipText() {
    }
}
