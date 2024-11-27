package com.mcdart.xp_synthesiser.blocks;

import com.mcdart.xp_synthesiser.util.HelperFunctions;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.slf4j.Logger;

import static com.mcdart.xp_synthesiser.XPSynthesiser.*;

public class SynthesiserMenu extends AbstractContainerMenu {
    private static final int HOTBAR_XPOS = 8;
    private static final int HOTBAR_YPOS = 142;
    private static final int PLAYER_INVENTORY_XPOS = 8;
    private static final int PLAYER_INVENTORY_YPOS = 84;
    private static final int RECORDER_SLOT_XPOS = 80;
    private static final int RECORDER_SLOT_YPOS = 38;
    private static final int SLOT_X_SPACING = 18;
    private static final int SLOT_Y_SPACING = 18;

    private final XPSynthesiserBlockEntity synthesiser;
    public final int power;

    private static final Logger LOGGER = LogUtils.getLogger();

    public SynthesiserMenu(int windowId, Inventory invPlayer, FriendlyByteBuf extraData) {
        this(windowId, invPlayer, extraData.readBlockPos());
    }

    public SynthesiserMenu(int windowId, Inventory invPlayer, BlockPos pos) {
        super(SYNTHESISER_MENU.get(), windowId);

        this.synthesiser = invPlayer.player.level().getBlockEntity(pos, XP_SYNTHESISER_BLOCK_ENTITY.get())
                .orElseThrow(() -> new IllegalStateException("synthesiser missing at " + pos));

        power = synthesiser.getData(POWER);

        // Player's Hotbar
        for (int x = 0; x < 9; x++) {
            addSlot(new Slot(invPlayer, x, HOTBAR_XPOS + SLOT_X_SPACING * x, HOTBAR_YPOS));
        }

        // Player's Main Inventory
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
//
//        // item router buffer
//        addSlot(new SlotItemHandler(router.getBuffer(), BUFFER_SLOT, BUFFER_XPOS, BUFFER_YPOS));
//
//        // item router modules
//        for (int slot = 0; slot < router.getModuleSlotCount(); slot++) {
//            addSlot(new InstalledModuleSlot(router.getModules(), slot, MODULE_XPOS + slot * SLOT_X_SPACING, MODULE_YPOS));
//        }
//        // item router upgrades
//        for (int slot = 0; slot < router.getUpgradeSlotCount(); slot++) {
//            addSlot(new SlotItemHandler(router.getUpgrades(), slot, UPGRADE_XPOS + slot * SLOT_X_SPACING, UPGRADE_YPOS));
//        }
//
//        addDataSlots(data);
//
//        final var event = new RegisterRouterContainerData(router);
//        NeoForge.EVENT_BUS.post(event);
//        event.getData().entrySet()
//                .stream().sorted(Map.Entry.comparingByKey())
//                .forEach(entry -> addDataSlot(entry.getValue()));
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
        //return !router.isRemoved() && Vec3.atCenterOf(router.getBlockPos()).distanceToSqr(player.position()) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int sourceSlotIndex) {
        Slot sourceSlot = slots.get(sourceSlotIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

//        // Check if the slot clicked is one of the vanilla container slots
//        if (sourceSlotIndex < TE_FIRST_SLOT) {
//            // This is a vanilla container slot so merge the stack into the appropriate part of the router's inventory
//            if (sourceStack.getItem() instanceof ModuleItem) {
//                // shift-clicked a module: see if there's a free module slot
//                if (!moveItemStackTo(sourceStack, TE_FIRST_SLOT + MODULE_SLOT_START, TE_FIRST_SLOT + MODULE_SLOT_END + 1, false)) {
//                    return ItemStack.EMPTY;
//                }
//            } else if (sourceStack.getItem() instanceof UpgradeItem) {
//                // shift-clicked an upgrade: see if there's a free upgrade slot
//                if (!moveItemStackTo(sourceStack, TE_FIRST_SLOT + UPGRADE_SLOT_START, TE_FIRST_SLOT + UPGRADE_SLOT_END + 1, false)) {
//                    return ItemStack.EMPTY;
//                }
//            } else {
//                // try to merge item into the router's buffer slot
//                if (!moveItemStackTo(sourceStack, TE_FIRST_SLOT + BUFFER_SLOT, TE_FIRST_SLOT + BUFFER_SLOT + 1, false)) {
//                    return ItemStack.EMPTY;
//                }
//            }
//        } else if (sourceSlotIndex < TE_FIRST_SLOT + TE_LAST_SLOT) {
//            // This is a router slot, so merge the stack into the players inventory
//            if (!moveItemStackTo(sourceStack, 0, TE_FIRST_SLOT - 1, false)) {
//                return ItemStack.EMPTY;
//            }
//        } else {
//            System.err.print("Invalid moduleSlotIndex: " + sourceSlotIndex);
//            return ItemStack.EMPTY;
//        }
//
//        // If stack size == 0 (the entire stack was moved) set slot contents to null
//        if (sourceStack.isEmpty()) {
//            sourceSlot.set(ItemStack.EMPTY);
//        } else {
//            sourceSlot.setChanged();
//        }
//
//        sourceSlot.onTake(player, sourceStack);
        return copyOfSourceStack;
    }

    public XPSynthesiserBlockEntity getSynthesiser() {
        return synthesiser;
    }
//
//    public static class InstalledModuleSlot extends SlotItemHandler {
//        // this is just so the slot can be easily identified for item tooltip purposes
//        InstalledModuleSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
//            super(itemHandler, index, xPosition, yPosition);
//        }
//    }

    @Override
    public boolean clickMenuButton(Player pPlayer, int pId) {
        // Plus Button
        if (pId == 1) {
            addLevelToSynthesiser(pPlayer);
        } else if (pId == 2) {
            removeLevelFromSynthesiser(pPlayer);
        }
        return false;
    }

    public void addLevelToSynthesiser(Player player) {
        XPSynthesiserBlockEntity synthesiser = getSynthesiser();

        // Check if player has a level to give
        if (player instanceof ServerPlayer serverPlayer && (serverPlayer.experienceLevel > 0 || serverPlayer.experienceProgress > 0)) {

            // Deduct nearest level from player
            int toDeduct = serverPlayer.experienceProgress > 0 ?
                    // Multiply progress by difference between levels
                    (int) (serverPlayer.getXpNeededForNextLevel() * serverPlayer.experienceProgress) :
                    // Get raw experience value of difference between current a lower level
                    HelperFunctions.getXPfromLevel(serverPlayer.experienceLevel) - HelperFunctions.getXPfromLevel(serverPlayer.experienceLevel - 1);

            serverPlayer.setExperienceLevels(serverPlayer.experienceProgress > 0 ? serverPlayer.experienceLevel : serverPlayer.experienceLevel - 1);
            serverPlayer.setExperiencePoints(0);

            // Add equivalent experience to block entity
            synthesiser.trackedProgress.set(1, synthesiser.trackedProgress.get(1) + toDeduct);
        }

    }

    public void removeLevelFromSynthesiser(Player player) {
        XPSynthesiserBlockEntity synthesiser = getSynthesiser();

        // Check if Synthesiser has a level to give

        if (synthesiser.trackedProgress.get(1) != 0) {
            // Deduct nearest level from block entity
            double synthesiserXPLevel = HelperFunctions.getLevelFromXP(synthesiser.trackedProgress.get(1));
            int flatLevelXP = HelperFunctions.getXPfromLevel(Math.floor(synthesiserXPLevel));
            if (flatLevelXP == synthesiser.trackedProgress.get(1)) {
                // If you have a level exactly
                flatLevelXP = HelperFunctions.getXPfromLevel(Math.floor(synthesiserXPLevel - 1));
            }
            int amountToGive = synthesiser.trackedProgress.get(1) - flatLevelXP;
            synthesiser.trackedProgress.set(1, synthesiser.trackedProgress.get(1) - amountToGive);

            // Add equivalent experience to player
            if (player != null) {
                player.giveExperiencePoints(amountToGive);
            }

        }


    }

}