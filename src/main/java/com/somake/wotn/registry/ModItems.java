package com.somake.wotn.registry;

import com.somake.wotn.WhispersOfTheNorth;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WhispersOfTheNorth.MODID);

    public static final DeferredItem<Item> BILBERRY = ITEMS.registerItem(
            "bilberry",
            Item::new,
            properties -> properties.food(new FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.2F)
                    .build()));

    public static final DeferredItem<Item> FENRIR_FUR = ITEMS.registerSimpleItem("fenrir_fur");
    public static final DeferredItem<Item> GOLEM_CORE = ITEMS.registerSimpleItem("golem_core");

    public static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(BILBERRY);
        }

        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(FENRIR_FUR);
            event.accept(GOLEM_CORE);
        }
    }

    private ModItems() {
    }
}
