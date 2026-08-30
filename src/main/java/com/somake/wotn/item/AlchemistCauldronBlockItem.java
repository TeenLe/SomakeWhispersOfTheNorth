package com.somake.wotn.item;

import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public final class AlchemistCauldronBlockItem extends BlockItem {
    public AlchemistCauldronBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @Nullable BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        if (context.getClickedFace() != Direction.UP) return null;
        var controllerPos = context.getClickedPos().above();
        if (!context.getLevel().getBlockState(controllerPos).canBeReplaced(context)) return null;
        BlockPlaceContext elevated = BlockPlaceContext.at(context, controllerPos, Direction.UP);
        return elevated.getClickedPos().equals(controllerPos) ? elevated : null;
    }
}
