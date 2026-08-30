package com.somake.wotn.item;

import com.somake.wotn.alchemy.AlchemyPotionConfiguration;
import com.somake.wotn.alchemy.NiflheimPotionEffects;
import com.somake.wotn.alchemy.FenrirPotionEffects;
import com.somake.wotn.alchemy.IdunnPotionEffects;
import com.somake.wotn.alchemy.JormungandrPotionEffects;
import com.somake.wotn.alchemy.AlchemyRune;
import com.somake.wotn.block.PotionDisplayBlock;
import com.somake.wotn.block.entity.PotionDisplayBlockEntity;
import com.somake.wotn.registry.ModBlocks;
import com.somake.wotn.registry.ModDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.function.Consumer;

public class TieredPotionItem extends Item {
    private final int tier;
    private final String family;

    public TieredPotionItem(String family, int tier, Properties properties) {
        super(properties);
        this.family = family;
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    public String family() {
        return family;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.isSecondaryUseActive()) {
            return super.useOn(context);
        }

        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.FAIL;
        }

        Level level = context.getLevel();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos placePos = placeContext.getClickedPos();
        BlockState placedState = ModBlocks.POTION_DISPLAY.get().defaultBlockState()
                .setValue(PotionDisplayBlock.FAMILY, PotionDisplayBlock.PotionFamily.fromId(family))
                .setValue(PotionDisplayBlock.TIER, tier)
                .setValue(PotionDisplayBlock.FACING, placeContext.getHorizontalDirection().getOpposite());
        if (!placeContext.canPlace()
                || !placedState.canSurvive(level, placePos)
                || !level.isUnobstructed(placedState, placePos, CollisionContext.placementContext(context.getPlayer()))) {
            return InteractionResult.FAIL;
        }

        ItemStack potion = context.getItemInHand().copyWithCount(1);
        if (!level.setBlock(placePos, placedState, 11)) {
            return InteractionResult.FAIL;
        }

        if (!(level.getBlockEntity(placePos) instanceof PotionDisplayBlockEntity display)) {
            level.removeBlock(placePos, false);
            return InteractionResult.FAIL;
        }
        display.setPotion(potion);

        SoundType sound = placedState.getSoundType(level, placePos, context.getPlayer());
        level.playSound(context.getPlayer(), placePos, sound.getPlaceSound(),
                net.minecraft.sounds.SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F,
                sound.getPitch() * 0.8F);
        level.gameEvent(context.getPlayer(), GameEvent.BLOCK_PLACE, placePos);
        context.getItemInHand().consume(1, context.getPlayer());

        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        AlchemyPotionConfiguration configuration = stack.getOrDefault(
                ModDataComponents.ALCHEMY_CONFIGURATION.get(), AlchemyPotionConfiguration.DEFAULT);
        if (!level.isClientSide()) {
            switch (family) {
                case "niflheim" -> NiflheimPotionEffects.apply(entity, tier, configuration);
                case "fenrir" -> FenrirPotionEffects.apply(entity, tier, configuration);
                case "idunn" -> IdunnPotionEffects.apply(entity, tier, configuration);
                case "jormungandr" -> JormungandrPotionEffects.apply(entity, tier, configuration);
                default -> {
                }
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(TooltipText.divider(family));
        tooltip.accept(TooltipText.heading("tooltip.wotn.section.alchemical_essence", family));
        tooltip.accept(Component.literal("  ◆ ").withColor(TooltipText.accent(family))
                .append(Component.translatable("tooltip.wotn.potion.family",
                        Component.translatable("screen.wotn.alchemy.family." + family))
                        .withColor(TooltipText.text(family))));
        tooltip.accept(Component.literal("  ◆ ").withColor(TooltipText.accent(family))
                .append(Component.translatable("tooltip.wotn.potion.tier", romanTier(tier))
                        .withColor(TooltipText.text(family))));
        tooltip.accept(Component.literal("    ").append(Component.translatable(
                "tooltip.wotn.potion.flavor." + family).withColor(TooltipText.muted(family))
                .withStyle(style -> style.withItalic(true))));
        AlchemyPotionConfiguration configuration = stack.getOrDefault(
                ModDataComponents.ALCHEMY_CONFIGURATION.get(), AlchemyPotionConfiguration.DEFAULT);
        if (family.equals("niflheim")) {
            tooltip.accept(TooltipText.divider(family));
            tooltip.accept(TooltipText.heading("tooltip.wotn.section.glacial_properties", family));
            tooltip.accept(Component.literal("  ◆ ").withColor(0x72D8EA)
                    .append(Component.translatable("tooltip.wotn.niflheim.duration",
                            configuration.durationTicks(family, tier) / 20).withColor(0xC9EAE6)));
            tooltip.accept(Component.literal("  ◆ ").withColor(0x72D8EA)
                    .append(Component.translatable("tooltip.wotn.niflheim.ice_damage",
                            Math.round((configuration.iceDamageMultiplier(tier) - 1.0D) * 100.0D))
                            .withColor(0xC9EAE6)));
            tooltip.accept(Component.literal("  ◆ ").withColor(0x72D8EA)
                    .append(Component.translatable("tooltip.wotn.niflheim.fire_reduction",
                            Math.round(configuration.fireDamageReduction(tier) * 100.0D))
                            .withColor(0xC9EAE6)));
        } else {
            tooltip.accept(TooltipText.divider(family));
            tooltip.accept(TooltipText.heading("tooltip.wotn.section.alchemical_properties", family));
            tooltip.accept(propertyLine(family, "tooltip.wotn.alchemy.duration",
                    configuration.durationTicks(family, tier) / 20));
            if (family.equals("fenrir")) {
                tooltip.accept(propertyLine(family, "tooltip.wotn.fenrir.attack_damage",
                        Math.round(configuration.fenrirAttackDamageBonus(tier) * 100.0D)));
                tooltip.accept(propertyLine(family, "tooltip.wotn.fenrir.attack_speed",
                        Math.round(configuration.fenrirAttackSpeedBonus(tier) * 100.0D)));
            } else if (family.equals("idunn")) {
                tooltip.accept(propertyLine(family, "tooltip.wotn.idunn.healing",
                        Math.round(configuration.idunnHealingBonus(tier) * 100.0D)));
                tooltip.accept(propertyLine(family, "tooltip.wotn.idunn.max_health",
                        Math.round(configuration.idunnMaxHealthBonus(tier))));
            } else if (family.equals("jormungandr")) {
                tooltip.accept(propertyLine(family, "tooltip.wotn.jormungandr.venom_damage",
                        Math.round(configuration.venomDamageBonus(tier) * 100.0D)));
                tooltip.accept(propertyLine(family, "tooltip.wotn.jormungandr.poison_resistance",
                        Math.round(configuration.poisonResistance(tier) * 100.0D)));
            }
        }
        if (!configuration.runes().isEmpty()) {
            tooltip.accept(TooltipText.divider(family));
            tooltip.accept(TooltipText.heading("tooltip.wotn.section.equipped_runes", family));
            java.util.Map<AlchemyRune, Integer> counts = new java.util.LinkedHashMap<>();
            configuration.runes().forEach(rune -> counts.merge(rune, 1, Integer::sum));
            counts.forEach((rune, count) -> tooltip.accept(Component.literal("  ◇ ").withColor(TooltipText.accent(family))
                    .append(Component.translatable(rune.translationKey()).withColor(TooltipText.text(family)))
                    .append(count > 1 ? Component.literal(" x" + count).withColor(0xFFFFD47A) : Component.empty())));
        }
        tooltip.accept(TooltipText.divider(family));
        if (family.equals("niflheim")) {
            tooltip.accept(Component.translatable("tooltip.wotn.niflheim.identity", tier)
                    .withColor(0x73958E).withStyle(style -> style.withItalic(true)));
        }
    }


    private static String romanTier(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            default -> "III";
        };
    }

    private static Component propertyLine(String family, String key, Object value) {
        return Component.literal("  ◆ ").withColor(TooltipText.accent(family))
                .append(Component.translatable(key, value).withColor(TooltipText.text(family)));
    }
}
