package com.mcdart.xp_synthesiser.blocks;

import com.mcdart.xp_synthesiser.items.KillRecorderItem;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.slf4j.Logger;

import javax.annotation.Nonnull;

public class SynthesiserItemHandler implements IItemHandler {

    public static final SynthesiserItemHandler INSTANCE = new SynthesiserItemHandler();

    public ItemStack heldItem = ItemStack.EMPTY;

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public int getSlots() {
        return 1;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? heldItem : ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (heldItem != ItemStack.EMPTY) {
            LOGGER.info("Tried to insert but insert was rejected {}", heldItem);
            return stack;
        } else {
            heldItem = stack;
            LOGGER.info("New item should be: {}", heldItem);
            return ItemStack.EMPTY;
        }
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (heldItem != ItemStack.EMPTY) {
            var temp = heldItem.copy();
            heldItem = ItemStack.EMPTY;
            LOGGER.info("Extracting: {}, left behind: {}", temp, heldItem);
            return temp;
        } else {
            LOGGER.info("Extracting failed");
            return ItemStack.EMPTY;
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        //return stack.getItem() instanceof KillRecorderItem;
        return true;
    }
}