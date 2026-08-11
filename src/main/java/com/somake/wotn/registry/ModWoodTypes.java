package com.somake.wotn.registry;

import com.somake.wotn.WhispersOfTheNorth;

import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public final class ModWoodTypes {
    public static final BlockSetType YGGDRASIL_SET = BlockSetType.register(
            new BlockSetType(WhispersOfTheNorth.MODID + ":yggdrasil"));
    public static final WoodType YGGDRASIL = WoodType.register(
            new WoodType(WhispersOfTheNorth.MODID + ":yggdrasil", YGGDRASIL_SET));

    private ModWoodTypes() {
    }
}
