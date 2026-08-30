package com.somake.wotn.block.entity;

import com.somake.wotn.item.TieredPotionItem;
import com.somake.wotn.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class PotionDisplayBlockEntity extends BlockEntity {
    private static final String POTION_TAG = "potion";
    private ItemStack potion = ItemStack.EMPTY;

    public PotionDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POTION_DISPLAY.get(), pos, state);
    }

    public ItemStack getPotion() {
        return potion;
    }

    public void setPotion(ItemStack stack) {
        potion = stack.getItem() instanceof TieredPotionItem ? stack.copyWithCount(1) : ItemStack.EMPTY;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!potion.isEmpty()) {
            output.store(POTION_TAG, ItemStack.CODEC, potion);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        potion = input.read(POTION_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
