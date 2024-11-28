package com.mcdart.xp_synthesiser.blocks;

import com.mcdart.xp_synthesiser.Config;
import com.mcdart.xp_synthesiser.XPSynthesiser;
import com.mcdart.xp_synthesiser.items.KillRecorderData;
import com.mcdart.xp_synthesiser.items.KillRecorderItem;
import com.mcdart.xp_synthesiser.util.HelperFunctions;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

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
            if(!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    private static int ticks = 0;
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
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
//        // Will default to 0 if absent. See the NBT article for more information.
        progress = tag.getInt("progress");

        // Not sure if necessary
        trackedProgress.set(0, progress);
        trackedProgress.set(1, tag.getInt("savedXP"));
//
//        LOGGER.info("Loaded {}", progress);
//        LOGGER.info("Loaded {}", tag.get("currentItem"));
//        if (tag.get("currentItem") != null) {
//            var optionalStack = ItemStack.parse(registries, tag.get("currentItem"));
//            LOGGER.info("Tried to load {}", optionalStack);
//            optionalStack.ifPresent(itemStack -> {currentItem = itemStack; loadSavedItem = true;});
//            // Load handler (somehow)
//        }
//        LOGGER.info("Tag {}", tag);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        if (tag.contains("energy") && tag.get("energy") != null) {
            energyStorage.deserializeNBT(registries, tag.get("energy"));
        }
    }

    // Save values into the passed CompoundTag here.
    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("progress", progress);
        tag.putInt("savedXP", trackedProgress.get(1));
//        LOGGER.info("Attempting to save {}, {}, {}", currentItem, ItemStack.EMPTY, currentItem.equals(ItemStack.EMPTY));
//        if (!currentItem.getItem().toString().equals(ItemStack.EMPTY.getItem().toString())) {
//            tag.put("currentItem", currentItem.save(registries));
//        }
//        LOGGER.info("Tried to save {}", tag);
//        setChanged(); //Necessary??
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.put("energy", energyStorage.serializeNBT(registries));
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    // Runs every tick
    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        if (!level.isClientSide()) {
            ticks++;
            // Make this run every so often
            if (ticks > 10) {
                if (t instanceof XPSynthesiserBlockEntity) {
                    ((XPSynthesiserBlockEntity) t).tickSynthesiser(level, blockPos, (XPSynthesiserBlockEntity) t);
                }
            }
        }
    }

    public void tickSynthesiser(Level level, BlockPos blockPos, XPSynthesiserBlockEntity XPSynthesiserEntity) {
        int power = this.trackedEnergy.get(1);

        this.trackedEnergy.set(1, power + 300);
        //LOGGER.info("Power: {}", power);

        setChanged();

        // If there is an item in the synthesiser, and it's a kill recorder
//        if (handler != null &&
//                !handler.getStackInSlot(0).equals(ItemStack.EMPTY) &&
//                handler.getStackInSlot(0).getItem() instanceof KillRecorderItem) {
//            // And it's a new item
//            if (!currentItem.equals(handler.getStackInSlot(0))) {
//                // Setup that new item
//                setupNewItem(handler.getStackInSlot(0));
//            } else {
//                // Otherwise, progress current item
//                progressCurrentItem();
//            }
//        }

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
        }

        //   handler.extractItem(0, 1, false);
        //// handler.insertItem(0, new ItemStack(level.players().getFirst().getMainHandItem().getItem()), false);

        ticks = 0;
    }

    public void setupNewItem(ItemStack newItem) {
        LOGGER.info("Item {} replaced by {}", currentItem, newItem);
        currentItem = newItem;
        progress = 0;
        trackedProgress.set(0, progress);
    }

    public void progressCurrentItem() {

        KillRecorderData killRecorderData = this.itemHandler.getStackInSlot(0).getOrDefault(XPSynthesiser.KILL_RECORDER_DATA_COMPONENT.get(), KillRecorderData.createEmpty());
        if (killRecorderData.recordingEnd() > 0) {
            //LOGGER.info("Progressing item {} {}, target: {}", killRecorderData, progress, killRecorderData.getRecordingEnd() - killRecorderData.getRecordingStart());

            // Check if the cost can be paid
            int duration = (int)(killRecorderData.recordingEnd() - killRecorderData.recordingStart());
            int cost = Config.GENERAL.requiresPower.isTrue() ?
                    (int) HelperFunctions.getTickCost(killRecorderData.xp(), duration) : 0;

            if (cost <= this.trackedEnergy.get(1)) {
                // Pay cost
                this.trackedEnergy.set(1, this.trackedEnergy.get(1) - cost);
               // LOGGER.info("Paid cost: {}, now: {}", cost, this.trackedEnergy.get(1));

                progress = progress + 10;
                trackedProgress.set(0, progress);
                // Check if progress bar has progressed enough
                if (progress >= duration) {
                    // If it has, generate the XP
                    generateXP(killRecorderData);
                    progress = 0;
                    trackedProgress.set(0, progress);
                }
            } else {
                //LOGGER.info("Not enough power: {}, {}", cost, this.trackedEnergy.get(1));
            }
        } else {
            LOGGER.info("Kill Recorder has no data");
        }
    }

    public void generateXP(KillRecorderData data) {
        LOGGER.info("Generating {} XP", data.xp());
        trackedProgress.set(1, trackedProgress.get(1) + data.xp());
        LOGGER.info("Total now {} XP", trackedProgress.get(1));

        // Generate XP in world
        //DropExperienceBlock
        //Minecraft.getInstance().setScreen(new SynthesiserScreen(Component.literal("Hello!")));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, provider);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        this.loadAdditional(tag, lookupProvider);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);
        //loadAdditional(pkt.getTag(), lookupProvider);
    }

    @Nullable
    public <T> T onRegisterCapability(BlockCapability<T, Direction> capability, @Nullable Direction side) {
        if (side == null) {
            return null;
        }

//        if (capability == Capabilities.EnergyStorage.BLOCK && hasType(EnergyPipeType.INSTANCE)) {
//            if (side != null) {
//                if (energyStorages[side.get3DDataValue()] == null) {
//                    energyStorages[side.get3DDataValue()] = new PipeEnergyStorage(this, side);
//                }
//                return (T) energyStorages[side.get3DDataValue()];
//            }
//        } else
        if (capability == Capabilities.ItemHandler.BLOCK) {
            return (T) SynthesiserItemHandler.INSTANCE;
        }
        return null;
    }

    public ItemStack getItem(int pSlot) {
        setChanged();
        return itemHandler.getStackInSlot(pSlot);
    }

    public void setItem(int pSlot, ItemStack pStack) {
        setChanged();
        itemHandler.insertItem(pSlot, pStack.copyWithCount(1), false);
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
            //LOGGER.info("Set {} energy for total of {}", val, energyStorage.getEnergyStored());
        }

        @Override
        public int getCount() {
            return 1;
        }
    }

    public class TrackedProgress implements ContainerData {
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
                savedXP = val; // may need to do the trickyness here for large ints
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
