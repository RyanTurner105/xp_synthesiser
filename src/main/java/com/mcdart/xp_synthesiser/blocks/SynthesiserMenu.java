package com.mcdart.xp_synthesiser.blocks;

import com.mcdart.xp_synthesiser.items.KillRecorderItem;
import com.mcdart.xp_synthesiser.util.HelperFunctions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import static com.mcdart.xp_synthesiser.XPSynthesiser.*;

public class SynthesiserMenu extends AbstractContainerMenu {
    public static final int HOTBAR_XPOS = 8;
    public static final int HOTBAR_YPOS = 142;
    public static final int PLAYER_INVENTORY_XPOS = 8;
    public static final int PLAYER_INVENTORY_YPOS = 84;
    public static final int RECORDER_SLOT_XPOS = 80;
    public static final int RECORDER_SLOT_YPOS = 38;
    public static final int SLOT_X_SPACING = 18;
    public static final int SLOT_Y_SPACING = 18;
    public static final int INVENTORY_SLOTS = 36;

    private final XPSynthesiserBlockEntity synthesiser;

    public SynthesiserMenu(int windowId, Inventory invPlayer, FriendlyByteBuf extraData) {
        this(windowId, invPlayer, extraData.readBlockPos());
    }

    public SynthesiserMenu(int windowId, Inventory invPlayer, BlockPos pos) {
        super(SYNTHESISER_MENU.get(), windowId);

        this.synthesiser = invPlayer.player.level().getBlockEntity(pos, XP_SYNTHESISER_BLOCK_ENTITY.get())
                .orElseThrow(() -> new IllegalStateException("synthesiser missing at " + pos));

        // Player's Hotbar slots
        for (int x = 0; x < 9; x++) {
            addSlot(new Slot(invPlayer, x, HOTBAR_XPOS + SLOT_X_SPACING * x, HOTBAR_YPOS));
        }

        // Player's Main Inventory slots
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                int slotNumber = 9 + y * 9 + x;
                int xpos = PLAYER_INVENTORY_XPOS + x * SLOT_X_SPACING;
                int ypos = PLAYER_INVENTORY_YPOS + y * SLOT_Y_SPACING;
                addSlot(new Slot(invPlayer, slotNumber, xpos, ypos));
            }
        }

        // Kill Recorder Item Slot
        addSlot(new SlotItemHandler(synthesiser.itemHandler, 0, RECORDER_SLOT_XPOS, RECORDER_SLOT_YPOS));

        // Energy slot
        addDataSlots(synthesiser.trackedEnergy);
        // Add slot for current progress and saved XP
        addDataSlots(synthesiser.trackedProgress);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return !synthesiser.isRemoved() && Vec3.atCenterOf(synthesiser.getBlockPos()).distanceToSqr(player.position()) <= 64;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int sourceSlotIndex) {
        Slot sourceSlot = slots.get(sourceSlotIndex);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack sourceStack = sourceSlot.getItem();

        // Check if the slot clicked is one of the vanilla container slots
        if (sourceSlotIndex < INVENTORY_SLOTS) {
            // This is a vanilla container slot so merge the stack into the appropriate part of the router's inventory
            if (sourceStack.getItem() instanceof KillRecorderItem) {
                // shift-clicked a module: see if there's a free module slot
                if (!moveItemStackTo(sourceStack, INVENTORY_SLOTS, INVENTORY_SLOTS + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return  ItemStack.EMPTY;
            }
        } else if (sourceSlotIndex == INVENTORY_SLOTS) {
            // This is the Kill Recorder slot, so try and move it back to the player inventory
            if (!moveItemStackTo(sourceStack, 0, INVENTORY_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            System.err.print("Unknown slot: " + sourceSlotIndex);
            return ItemStack.EMPTY;
        }

        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(player, sourceStack);
        return sourceStack.copy();
    }

    public XPSynthesiserBlockEntity getSynthesiser() {
        return synthesiser;
    }

    @Override
    public boolean clickMenuButton(Player pPlayer, int pId) {
        // Plus 1 Button
        if (pId == SynthesiserScreen.PLUS_ONE_BUTTON_ID) {
            addLevelsToSynthesiser(pPlayer, 1);
        } else if (pId == SynthesiserScreen.MINUS_ONE_BUTTON_ID) {
            // Minus 1 Button
            removeLevelsFromSynthesiser(pPlayer, 1);
        } else if (pId == SynthesiserScreen.PLUS_TEN_BUTTON_ID) {
            // Plus 10 Button
            addLevelsToSynthesiser(pPlayer, 10);
        } else if (pId == SynthesiserScreen.MINUS_TEN_BUTTON_ID) {
            // Minus 10 Button
            removeLevelsFromSynthesiser(pPlayer, 10);
        }
        return false;
    }

    public void addLevelsToSynthesiser(Player player, int levels) {
        XPSynthesiserBlockEntity synthesiser = getSynthesiser();

        // Check if player has a level to give
        if (player instanceof ServerPlayer serverPlayer && (serverPlayer.experienceLevel > 0 || serverPlayer.experienceProgress > 0)) {

            // Get amount to deduct
            int currentXPProgress = (int) (serverPlayer.getXpNeededForNextLevel() * serverPlayer.experienceProgress); // Any extra progress to a level
            int toDeduct =
                    currentXPProgress +
                    HelperFunctions.getXPfromLevel(serverPlayer.experienceLevel) - // The amount of levels they can give, based on the amount they want to give
                            HelperFunctions.getXPfromLevel(Math.max(serverPlayer.experienceLevel - levels + (currentXPProgress > 0 ? 1 : 0), 0));

            // Deduct
            serverPlayer.setExperienceLevels(Math.max(serverPlayer.experienceLevel - levels + (currentXPProgress > 0 ? 1 : 0), 0));
            serverPlayer.setExperiencePoints(0);

            // Add equivalent experience to block entity
            synthesiser.trackedProgress.set(1, synthesiser.trackedProgress.get(1) + toDeduct);
        }

    }

    public void removeLevelsFromSynthesiser(Player player, int levels) {
        XPSynthesiserBlockEntity synthesiser = getSynthesiser();

        if (player instanceof ServerPlayer serverPlayer && synthesiser.trackedProgress.get(1) > 0) {
            int amountToGive = (int) Math.ceil(serverPlayer.getXpNeededForNextLevel() * (1 - serverPlayer.experienceProgress)) + // Amount for the first level
                    HelperFunctions.getXPfromLevel(serverPlayer.experienceLevel + levels) -
                    HelperFunctions.getXPfromLevel(serverPlayer.experienceLevel + 1); // Amount for any future levels

            // If Synthesiser has enough XP for the player to gain the rest of the levels
            if (synthesiser.trackedProgress.get(1) >= amountToGive) {
                int newAmount = (int) (HelperFunctions.getXPfromLevel(serverPlayer.experienceLevel) +
                        serverPlayer.getXpNeededForNextLevel() * serverPlayer.experienceProgress) +
                        amountToGive;

                // Player will always have a flat level now
                serverPlayer.setExperiencePoints(0);
                serverPlayer.setExperienceLevels((int) HelperFunctions.getLevelFromXP(newAmount));

                // Remove xp from synthesiser
                synthesiser.trackedProgress.set(1, synthesiser.trackedProgress.get(1) - amountToGive);

            } else {
                // Gain as much of a partial level as you can
                double newLevels = HelperFunctions.getLevelFromXP((int) (HelperFunctions.getXPfromLevel(serverPlayer.experienceLevel) +
                                serverPlayer.getXpNeededForNextLevel() * serverPlayer.experienceProgress) + synthesiser.trackedProgress.get(1));
                int newLevel = (int) Math.floor(newLevels);
                int newPoints = (int) Math.ceil((HelperFunctions.getXPfromLevel(newLevel + 1) - HelperFunctions.getXPfromLevel(newLevel)) *
                                (newLevels - newLevel));

                serverPlayer.setExperienceLevels(newLevel);
                serverPlayer.setExperiencePoints(newPoints);

                synthesiser.trackedProgress.set(1, 0); // Synthesiser now has nothing left
            }
        }

    }

}