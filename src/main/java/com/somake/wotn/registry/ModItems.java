package com.somake.wotn.registry;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.item.LeviathanAxeItem;
import com.somake.wotn.item.TieredPotionItem;
import com.somake.wotn.item.AlchemyRuneItem;
import com.somake.wotn.alchemy.AlchemyRune;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WhispersOfTheNorth.MODID);

    public static final DeferredItem<BlockItem> BILBERRY = ITEMS.registerItem(
            "bilberry",
            properties -> new BlockItem(ModBlocks.BILBERRY_BUSH.get(), properties.useItemDescriptionPrefix()),
            properties -> properties.food(new FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.2F)
                    .build()));

    public static final DeferredItem<Item> FENRIR_FUR = ITEMS.registerSimpleItem("fenrir_fur");
    public static final DeferredItem<Item> GOLEM_CORE = ITEMS.registerSimpleItem("golem_core");
    public static final DeferredItem<Item> ICE_BONE = ITEMS.registerSimpleItem("ice_bone");
    public static final DeferredItem<Item> WOLF_TOOTH = ITEMS.registerSimpleItem("wolf_tooth");
    public static final DeferredItem<Item> FENRIR_BLOOD = ITEMS.registerSimpleItem("fenrir_blood");
    public static final DeferredItem<Item> IDUNN_APPLE = ITEMS.registerSimpleItem("idunn_apple");
    public static final DeferredItem<Item> JORMUNGANDR_FANG = ITEMS.registerSimpleItem("jormungandr_fang");
    public static final DeferredItem<Item> JORMUNGANDR_VENOM = ITEMS.registerSimpleItem("jormungandr_venom");
    public static final DeferredItem<Item> DRAUGR_SWORD = ITEMS.registerSimpleItem(
            "draugr_sword",
            properties -> properties.sword(ToolMaterial.IRON, 3.0F, -2.4F));
    public static final DeferredItem<LeviathanAxeItem> LEVIATHAN_AXE = ITEMS.registerItem(
            "leviathan_axe",
            properties -> new LeviathanAxeItem(ToolMaterial.IRON,
                    properties.component(DataComponents.TOOLTIP_STYLE, tooltipStyle("frost"))
                             .component(DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE)));

    public static final DeferredItem<SpawnEggItem> GOLEM_SPAWN_EGG = registerSpawnEgg(
            "golem_spawn_egg", ModEntities.GOLEM);
    public static final DeferredItem<SpawnEggItem> STONE_SLIME_SPAWN_EGG = registerSpawnEgg(
            "stone_slime_spawn_egg", ModEntities.STONE_SLIME);
    public static final DeferredItem<SpawnEggItem> FENRIR_SPAWN_EGG = registerSpawnEgg(
            "fenrir_spawn_egg", ModEntities.FENRIR);
    public static final DeferredItem<SpawnEggItem> ICE_DRAUGR_SPAWN_EGG = registerSpawnEgg(
            "ice_draugr_spawn_egg", ModEntities.ICE_DRAUGR);
    public static final DeferredItem<SpawnEggItem> FIRE_DRAUGR_SPAWN_EGG = registerSpawnEgg(
            "fire_draugr_spawn_egg", ModEntities.FIRE_DRAUGR);
    public static final List<DeferredItem<SpawnEggItem>> SPAWN_EGGS = List.of(
            GOLEM_SPAWN_EGG,
            STONE_SLIME_SPAWN_EGG,
            FENRIR_SPAWN_EGG,
            ICE_DRAUGR_SPAWN_EGG,
            FIRE_DRAUGR_SPAWN_EGG);

    private static final Map<AlchemyRune, DeferredItem<AlchemyRuneItem>> RUNE_ITEMS = registerRunes();
    public static final List<DeferredItem<AlchemyRuneItem>> ALCHEMY_RUNES = AlchemyRune.ordered().stream()
            .map(RUNE_ITEMS::get).toList();

    public static final DeferredItem<TieredPotionItem> JORMUNGANDR_VENOM_TIER_1 = registerPotion(
            "jormungandr_venom_tier_1", "jormungandr", 1);
    public static final DeferredItem<TieredPotionItem> JORMUNGANDR_VENOM_TIER_2 = registerPotion(
            "jormungandr_venom_tier_2", "jormungandr", 2);
    public static final DeferredItem<TieredPotionItem> JORMUNGANDR_VENOM_TIER_3 = registerPotion(
            "jormungandr_venom_tier_3", "jormungandr", 3);

    public static final DeferredItem<TieredPotionItem> FENRIR_BLOOD_TIER_1 = registerPotion(
            "fenrir_blood_tier_1", "fenrir", 1);
    public static final DeferredItem<TieredPotionItem> FENRIR_BLOOD_TIER_2 = registerPotion(
            "fenrir_blood_tier_2", "fenrir", 2);
    public static final DeferredItem<TieredPotionItem> FENRIR_BLOOD_TIER_3 = registerPotion(
            "fenrir_blood_tier_3", "fenrir", 3);

    public static final DeferredItem<TieredPotionItem> NIFLHEIM_ESSENCE_TIER_1 = registerPotion(
            "niflheim_essence_tier_1", "niflheim", 1);
    public static final DeferredItem<TieredPotionItem> NIFLHEIM_ESSENCE_TIER_2 = registerPotion(
            "niflheim_essence_tier_2", "niflheim", 2);
    public static final DeferredItem<TieredPotionItem> NIFLHEIM_ESSENCE_TIER_3 = registerPotion(
            "niflheim_essence_tier_3", "niflheim", 3);

    public static final DeferredItem<TieredPotionItem> IDUNN_ELIXIR_TIER_1 = registerPotion(
            "idunn_elixir_tier_1", "idunn", 1);
    public static final DeferredItem<TieredPotionItem> IDUNN_ELIXIR_TIER_2 = registerPotion(
            "idunn_elixir_tier_2", "idunn", 2);
    public static final DeferredItem<TieredPotionItem> IDUNN_ELIXIR_TIER_3 = registerPotion(
            "idunn_elixir_tier_3", "idunn", 3);

    public static final List<DeferredItem<TieredPotionItem>> TIERED_POTIONS = List.of(
            JORMUNGANDR_VENOM_TIER_1,
            JORMUNGANDR_VENOM_TIER_2,
            JORMUNGANDR_VENOM_TIER_3,
            FENRIR_BLOOD_TIER_1,
            FENRIR_BLOOD_TIER_2,
            FENRIR_BLOOD_TIER_3,
            NIFLHEIM_ESSENCE_TIER_1,
            NIFLHEIM_ESSENCE_TIER_2,
            NIFLHEIM_ESSENCE_TIER_3,
            IDUNN_ELIXIR_TIER_1,
            IDUNN_ELIXIR_TIER_2,
            IDUNN_ELIXIR_TIER_3);

    public static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(BILBERRY);
            TIERED_POTIONS.forEach(event::accept);
        }

        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(FENRIR_FUR);
            event.accept(GOLEM_CORE);
            event.accept(ICE_BONE);
            event.accept(WOLF_TOOTH);
            event.accept(FENRIR_BLOOD);
            event.accept(IDUNN_APPLE);
            event.accept(JORMUNGANDR_FANG);
            event.accept(JORMUNGANDR_VENOM);
            ALCHEMY_RUNES.forEach(event::accept);
        }

        if (event.getTabKey() == CreativeModeTabs.COMBAT || event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(LEVIATHAN_AXE);
            event.accept(DRAUGR_SWORD);
        }

        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.YGGDRASIL_SHELF_ITEM);
            event.accept(ModBlocks.BOOK_PILE_ITEM);
            event.accept(ModBlocks.ALCHEMIST_CAULDRON_ITEM);
            event.accept(ModBlocks.FENRIR_SEAL_ITEM);
        }

        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            SPAWN_EGGS.forEach(event::accept);
        }
    }

    public static DeferredItem<AlchemyRuneItem> runeItem(AlchemyRune rune) {
        DeferredItem<AlchemyRuneItem> item = RUNE_ITEMS.get(rune);
        if (item == null) throw new IllegalArgumentException("Unknown alchemy rune: " + rune);
        return item;
    }

    private static Map<AlchemyRune, DeferredItem<AlchemyRuneItem>> registerRunes() {
        Map<AlchemyRune, DeferredItem<AlchemyRuneItem>> items = new EnumMap<>(AlchemyRune.class);
        for (AlchemyRune rune : AlchemyRune.ordered()) items.put(rune, registerRune(rune));
        return Map.copyOf(items);
    }

    private static DeferredItem<AlchemyRuneItem> registerRune(AlchemyRune rune) {
        return ITEMS.registerItem("rune_" + rune.id(),
                properties -> new AlchemyRuneItem(rune, properties.stacksTo(16)
                        .component(DataComponents.TOOLTIP_STYLE, tooltipStyle(
                                rune.family().equals("niflheim") ? "frost" : "alchemy/" + rune.family()))));
    }

    private static DeferredItem<TieredPotionItem> registerPotion(String name, String family, int tier) {
        return ITEMS.registerItem(
                name,
                properties -> new TieredPotionItem(
                        family, tier,
                        properties
                                .stacksTo(16)
                                .usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE)
                                .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
                                .component(DataComponents.TOOLTIP_STYLE, tooltipStyle(
                                        family.equals("niflheim") ? "frost" : "alchemy/" + family))));
    }

    private static DeferredItem<SpawnEggItem> registerSpawnEgg(
            String name, Supplier<? extends EntityType<?>> entityType) {
        return ITEMS.registerItem(name, SpawnEggItem::new,
                properties -> properties.spawnEgg(entityType.get()));
    }

    private static Identifier tooltipStyle(String path) {
        return Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, path);
    }

    private ModItems() {
    }
}
