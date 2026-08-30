package com.somake.wotn.client.property;

import com.mojang.serialization.MapCodec;
import com.somake.wotn.registry.ModDataComponents;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record LeviathanImbuedProperty() implements ConditionalItemModelProperty {
    public static final MapCodec<LeviathanImbuedProperty> MAP_CODEC = MapCodec.unit(new LeviathanImbuedProperty());

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed,
            ItemDisplayContext displayContext) {
        Long activeUntil = stack.get(ModDataComponents.ICE_IMBUED_UNTIL.get());
        return activeUntil != null && level != null && activeUntil > level.getGameTime();
    }

    @Override
    public MapCodec<LeviathanImbuedProperty> type() {
        return MAP_CODEC;
    }
}
