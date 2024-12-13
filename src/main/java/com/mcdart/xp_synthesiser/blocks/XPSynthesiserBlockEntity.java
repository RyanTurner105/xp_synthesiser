package com.mcdart.xp_synthesiser.blocks;

import com.mcdart.xp_synthesiser.Config;
import com.mcdart.xp_synthesiser.XPSynthesiser;
import com.mcdart.xp_synthesiser.items.KillRecorderData;
import com.mcdart.xp_synthesiser.items.KillRecorderItem;
import com.mcdart.xp_synthesiser.util.HelperFunctions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.mcdart.xp_synthesiser.XPSynthesiser.*;

public class XPSynthesiserBlockEntity extends BlockEntity {

    public final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected int getStackLimit(int slot, @NotNull ItemStack stack) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if(level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {

            if (stack.getItem() instanceof KillRecorderItem) {
                return super.insertItem(slot, stack, simulate);
            }

            return stack;
        }
    };

    public ItemStack currentItem = ItemStack.EMPTY;
    public int progress = 0;

    public final TrackedProgress trackedProgress = new TrackedProgress();
    public final TrackedEnergy trackedEnergy = new TrackedEnergy();
    public final EnergyStorage energyStorage = new EnergyStorage(1000000);

    public XPSynthesiserBlockEntity(BlockPos pos, BlockState blockState) {
        super(XP_SYNTHESISER_BLOCK_ENTITY.get(), pos, blockState);
    }

    // Read values from the passed CompoundTag here.
    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        progress = tag.getInt("progress");

        // Load progress and XP
        trackedProgress.set(0, tag.getInt("progress"));
        trackedProgress.set(1, tag.getInt("savedXP"));

        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        if (itemHandler.getStackInSlot(0).getItem() instanceof KillRecorderItem) {
            currentItem = itemHandler.getStackInSlot(0);
        }
        if (tag.contains("energy") && tag.get("energy") != null) {
            energyStorage.deserializeNBT(registries, Objects.requireNonNull(tag.get("energy")));
        }
    }

    // Save values into the passed CompoundTag here.
    @Override
    public void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("progress", trackedProgress.get(0));
        tag.putInt("savedXP", trackedProgress.get(1));

        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.put("energy", energyStorage.serializeNBT(registries));
    }

    // Runs every tick
    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        if (!level.isClientSide()) {
                if (t instanceof XPSynthesiserBlockEntity) {
                    ((XPSynthesiserBlockEntity) t).tickSynthesiser();
                }
        }
    }

    public void tickSynthesiser() {

        if(this.itemHandler != null && !this.itemHandler.getStackInSlot(0).equals(ItemStack.EMPTY) &&
                this.itemHandler.getStackInSlot(0).getItem() instanceof KillRecorderItem) {
            // And it's a new item
            if (!currentItem.equals(this.itemHandler.getStackInSlot(0))) {
                // Setup that new item
                setupNewItem(this.itemHandler.getStackInSlot(0));
            } else {
                // Otherwise, progress current item
                progressCurrentItem();
            }
        } else {
            resetProgress();
        }

    }

    public void setupNewItem(ItemStack newItem) {
        currentItem = newItem;
        resetProgress();
    }

    public void progressCurrentItem() {

        KillRecorderData killRecorderData = this.itemHandler.getStackInSlot(0).getOrDefault(XPSynthesiser.KILL_RECORDER_DATA_COMPONENT.get(), KillRecorderData.createEmpty());
        if (killRecorderData.recordingEnd() > 0) {

            // Check if the cost can be paid
            int duration = (int)(killRecorderData.recordingEnd() - killRecorderData.recordingStart());
            int cost = Config.GENERAL.requiresPower.isTrue() ?
                    (int) HelperFunctions.getTickCost(killRecorderData.xp(), duration) : 0;

            if (cost <= this.trackedEnergy.get(1)) {
                // Pay cost
                this.trackedEnergy.set(1, this.trackedEnergy.get(1) - cost);

                progress++;
                trackedProgress.set(0, progress);
                // Check if progress bar has progressed enough
                if (progress >= duration) {
                    // If it has, generate the XP
                    generateXP(killRecorderData);
                    resetProgress();
                }
            }
        }
    }

    public void resetProgress() {
        progress = 0;
        trackedProgress.set(0, progress);
    }

    public void generateXP(KillRecorderData data) {
        trackedProgress.set(1, trackedProgress.get(1) + data.xp());
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider provider) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, provider);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider lookupProvider) {
        this.loadAdditional(tag, lookupProvider);
    }

    @Override
    public void onDataPacket(@NotNull Connection net, @NotNull ClientboundBlockEntityDataPacket pkt, HolderLookup.@NotNull Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);
    }

    public class TrackedEnergy implements ContainerData {
        @Override
        public int get(int idx) {
            return energyStorage.getEnergyStored();
        }

        @Override
        public void set(int idx, int val) {
            if (val > energyStorage.getEnergyStored()) {
                // Add
                energyStorage.receiveEnergy(val - energyStorage.getEnergyStored(), false);
            } else {
                // Remove
                energyStorage.extractEnergy(energyStorage.getEnergyStored() - val, false);
            }
        }

        @Override
        public int getCount() {
            return 1;
        }
    }

    public static class TrackedProgress implements ContainerData {
        int progress = 0; // ID 0
        int savedXP = 0; // ID 1

        @Override
        public int get(int idx) {
            if (idx == 0) {
                return progress;
            } else if (idx == 1) {
                return savedXP;
            }
            return -1;
        }

        @Override
        public void set(int idx, int val) {
            if (idx == 0) {
                progress = val;
            } else if (idx == 1) {
                savedXP = val;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
