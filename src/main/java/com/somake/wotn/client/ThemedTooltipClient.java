package com.somake.wotn.client;

import com.mojang.datafixers.util.Either;
import com.somake.wotn.client.tooltip.TooltipDivider;
import com.somake.wotn.item.TieredPotionItem;
import com.somake.wotn.item.AlchemyRuneItem;
import com.somake.wotn.item.TooltipText;
import com.somake.wotn.registry.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class ThemedTooltipClient {
    private static final int ICE_START = 0x39DFFC;
    private static final int ICE_END = 0xF4FEFF;

    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        int[] colors = nameColors(stack);
        if (colors == null || event.getToolTip().isEmpty()) return;
        event.getToolTip().set(0, gradient(event.getToolTip().getFirst().getString(), colors[0], colors[1], true));
    }

    public static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        if (nameColors(event.getItemStack()) == null) return;
        event.setMaxWidth(230);
        int dividerColor = dividerColor(event.getItemStack());
        var elements = event.getTooltipElements();
        for (int index = 0; index < elements.size(); index++) {
            var text = elements.get(index).left();
            if (text.isPresent() && text.get().getString().equals("+------------------------+")) {
                elements.set(index, Either.right(new TooltipDivider(dividerColor)));
            }
        }
    }

    public static MutableComponent gradient(String text, int startRgb, int endRgb, boolean bold) {
        MutableComponent result = Component.empty();
        int count = text.codePointCount(0, text.length());
        int index = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            float progress = count <= 1 ? 0.0F : index / (float)(count - 1);
            int color = lerpRgb(startRgb, endRgb, progress);
            result.append(Component.literal(new String(Character.toChars(codePoint)))
                    .setStyle(Style.EMPTY.withColor(color).withBold(bold)));
            offset += Character.charCount(codePoint);
            index++;
        }
        return result;
    }

    private static int[] nameColors(ItemStack stack) {
        if (stack.is(ModItems.LEVIATHAN_AXE.get())) return new int[] {ICE_START, ICE_END};
        if (stack.getItem() instanceof AlchemyRuneItem runeItem) return familyColors(runeItem.rune().family());
        if (!(stack.getItem() instanceof TieredPotionItem potion)) return null;
        return familyColors(potion.family());
    }

    private static int[] familyColors(String family) {
        return switch (family) {
            case "jormungandr" -> new int[] {0x45D77D, 0xE9FF8A};
            case "fenrir" -> new int[] {0x375EBA, 0xB8D9FF};
            case "niflheim" -> new int[] {ICE_START, ICE_END};
            case "idunn" -> new int[] {0xFF9E35, 0xFFF0A8};
            default -> null;
        };
    }

    private static int dividerColor(ItemStack stack) {
        if (stack.getItem() instanceof TieredPotionItem potion) return TooltipText.accent(potion.family());
        if (stack.getItem() instanceof AlchemyRuneItem runeItem) return TooltipText.accent(runeItem.rune().family());
        return TooltipText.accent("niflheim");
    }

    private static int lerpRgb(int from, int to, float progress) {
        int red = Mth.lerpInt(progress, from >>> 16 & 0xFF, to >>> 16 & 0xFF);
        int green = Mth.lerpInt(progress, from >>> 8 & 0xFF, to >>> 8 & 0xFF);
        int blue = Mth.lerpInt(progress, from & 0xFF, to & 0xFF);
        return red << 16 | green << 8 | blue;
    }

    private ThemedTooltipClient() {
    }
}
