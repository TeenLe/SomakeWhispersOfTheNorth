package com.somake.wotn.item;

import com.somake.wotn.alchemy.AlchemyRune;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public final class AlchemyRuneItem extends Item {
    private final AlchemyRune rune;

    public AlchemyRuneItem(AlchemyRune rune, Properties properties) {
        super(properties);
        this.rune = rune;
    }

    public AlchemyRune rune() {
        return rune;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(TooltipText.divider(rune.family()));
        tooltip.accept(TooltipText.heading("tooltip.wotn.section.alchemy_rune", rune.family()));
        tooltip.accept(Component.translatable(rune.descriptionKey()).withColor(TooltipText.text(rune.family())));
        tooltip.accept(Component.translatable("tooltip.wotn.rune.slots", rune.slots())
                .withColor(TooltipText.accent(rune.family())));
        if (rune.prerequisite() != null) {
            tooltip.accept(Component.translatable("tooltip.wotn.rune.requires",
                    Component.translatable(rune.prerequisite().translationKey()).withColor(TooltipText.text(rune.family())))
                    .withColor(TooltipText.muted(rune.family())));
        }
    }
}
