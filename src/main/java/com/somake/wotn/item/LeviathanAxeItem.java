package com.somake.wotn.item;

import com.somake.wotn.registry.ModDataComponents;
import com.somake.wotn.skilltree.WeaponSkillData;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class LeviathanAxeItem extends AxeItem {
    public LeviathanAxeItem(ToolMaterial material, Properties properties) {
        super(material, 6.0F, -3.0F, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        var progress = WeaponSkillData.progress(stack);
        tooltip.accept(TooltipText.divider());
        tooltip.accept(TooltipText.heading("tooltip.wotn.leviathan.mastery"));
        tooltip.accept(Component.literal("  ◆ ").withColor(0x72D8EA)
                .append(Component.translatable("tooltip.wotn.leviathan.mastery_value",
                        progress.masteryLevel(), progress.masteryXp(),
                        progress.masteryLevel() >= 10 ? 0 : com.somake.wotn.skilltree.WeaponSkillProgress
                                .xpRequired(progress.masteryLevel())).withColor(0xC9EAE6)));
        tooltip.accept(TooltipText.divider());
        tooltip.accept(TooltipText.heading("tooltip.wotn.leviathan.equipped_skills"));
        appendSkill(tooltip, stack.getOrDefault(ModDataComponents.LEVIATHAN_PRIMARY_SKILL.get(), 3), 1);
        appendSkill(tooltip, stack.getOrDefault(ModDataComponents.LEVIATHAN_SECONDARY_SKILL.get(), 0), 2);
    }

    private void appendSkill(Consumer<Component> tooltip, int skillId, int slot) {
        if (skillId == 0) {
            tooltip.accept(Component.literal("  ◇ ").withColor(0x566B65)
                    .append(Component.translatable("tooltip.wotn.leviathan.empty_slot", slot).withColor(0x647973)));
            return;
        }
        String key = switch (skillId) {
            case 1 -> "imbue";
            case 2 -> "spikes";
            default -> "throw";
        };
        tooltip.accept(Component.literal("  ✦ ").withColor(0x8DEBFF)
                .append(Component.translatable("tooltip.wotn.leviathan.control",
                        Component.keybind(slot == 1 ? "key.wotn.leviathan_skill_slot_one"
                                : "key.wotn.leviathan_skill_slot_two"))
                        .withColor(0xFFD98A).withStyle(style -> style.withBold(true)))
                .append(Component.literal("  "))
                .append(Component.translatable("screen.wotn.node." + key).withColor(0xDDFBFF)));
        tooltip.accept(Component.literal("     ").append(Component.translatable(
                "screen.wotn.node." + key + ".description").withColor(0x90AAA4)));
    }

}
