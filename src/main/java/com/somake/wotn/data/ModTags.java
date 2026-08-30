package com.somake.wotn.data;

import com.somake.wotn.WhispersOfTheNorth;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final TagKey<Block> YGGDRASIL_LOGS_BLOCK = TagKey.create(
            Registries.BLOCK, Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "yggdrasil_logs"));
    public static final TagKey<Item> YGGDRASIL_LOGS_ITEM = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "yggdrasil_logs"));
    public static final TagKey<Block> IDUNN_LOGS_BLOCK = TagKey.create(
            Registries.BLOCK, Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "idunn_logs"));
    public static final TagKey<Item> IDUNN_LOGS_ITEM = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "idunn_logs"));
    public static final TagKey<Item> NIFLHEIM_RUNES_ITEM = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "niflheim_runes"));

    private ModTags() {
    }
}
