package com.mcdart.xp_synthesiser.blocks;

import com.mcdart.xp_synthesiser.XPSynthesiser;
import com.mcdart.xp_synthesiser.items.KillRecorderData;
import com.mcdart.xp_synthesiser.items.KillRecorderItem;
import com.mcdart.xp_synthesiser.util.HelperFunctions;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.Arrays;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;

public class SynthesiserScreen extends AbstractContainerScreen<SynthesiserMenu> {
    private static final int MAIN_SCREEN_HEIGHT = 166;
    private static final int MAIN_SCREEN_WIDTH = 176;
    private static final int LABEL_YPOS = 5;
    private static final int XP_LABEL_XPOS = 158;
    private static final int XP_LABEL_YPOS = 42;
    private static final int PROGRESS_LABEL_YPOS = 70;
    private static final int POWER_BAR_HEIGHT = 70;
    private static final int POWER_BAR_WIDTH = 16;
    private static final int POWER_BAR_X_OFFSET = 8;
    private static final int POWER_BAR_Y_OFFSET = 8;
    private static final int POWER_BAR_TEXTURE_X_OFFSET = 176;
    private static final int POWER_BAR_TEXTURE_Y_OFFSET = 2;
    private static final int PROGRESS_BAR_HEIGHT = 38;
    private static final int PROGRESS_BAR_WIDTH = 38;
    private static final int PROGRESS_BAR_X_OFFSET = 69;
    private static final int PROGRESS_BAR_Y_OFFSET = 27;
    private static final int PROGRESS_BAR_TEXTURE_X_OFFSET = 193;
    private static final int PROGRESS_BAR_TEXTURE_Y_OFFSET = 2;


    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation background =
            ResourceLocation.fromNamespaceAndPath(XPSynthesiser.MODID, "textures/gui/xp_synthesiser.png");

    public SynthesiserScreen(SynthesiserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
        this.imageWidth = MAIN_SCREEN_WIDTH;
        this.imageHeight = MAIN_SCREEN_HEIGHT;

    }

    @Override
    protected void init() {
        super.init();

        // Add widgets and precomputed values
        //this.addRenderableWidget(new EditBox());
//        this.addRenderableWidget(new ImageButton(leftPos + 126, topPos + 25, 5, 5,
//                new WidgetSprites(
//                        ResourceLocation.fromNamespaceAndPath(XPSynthesiser.MODID, "button/plus_button"),
//                        ResourceLocation.fromNamespaceAndPath(XPSynthesiser.MODID, "button/plus_button")
//                ),
//                (element)->{
//                    LOGGER.info("pressed {}", element);
//
//                }
//        ));
    }


    @Override
    public void containerTick() {
        super.containerTick();

        // Execute some logic every frame
        //LOGGER.info("Screen is ticking");
    }

    // mouseX and mouseY indicate the scaled coordinates of where the cursor is in on the screen
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        // Background is typically rendered first
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // Render text here?

        // Then the widgets if this is a direct child of the Screen
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        addRenderableWidget(new ImageButton(leftPos + 158, topPos + 26, 5, 5,
                new WidgetSprites(
                        ResourceLocation.fromNamespaceAndPath(XPSynthesiser.MODID, "button/plus_button"),
                        ResourceLocation.fromNamespaceAndPath(XPSynthesiser.MODID, "button/plus_button")
                ),
                (element)->minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1)
                        //addLevelToSynthesiser()
        )).renderWidget(graphics, mouseX, mouseY, partialTick);
        addRenderableWidget(new ImageButton(leftPos + 158, topPos + 58, 5, 5,
                new WidgetSprites(
                        ResourceLocation.fromNamespaceAndPath(XPSynthesiser.MODID, "button/minus_button"),
                        ResourceLocation.fromNamespaceAndPath(XPSynthesiser.MODID, "button/minus_button")
                ),
                (element)->minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2)
                        //removeLevelFromSynthesiser()
        )).renderWidget(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBg(GuiGraphics graphics, float pPartialTick, int mouseX, int mouseY) {
        // Render things here before widgets (background textures)
        graphics.blit(background, leftPos, topPos, 0, 0, MAIN_SCREEN_WIDTH, MAIN_SCREEN_HEIGHT);

//        LOGGER.info("{}, {}", menu.getSynthesiser().trackedEnergy, menu.getSynthesiser().trackedEnergy.get(1));
        XPSynthesiserBlockEntity synthesiser = menu.getSynthesiser();

        // Energy Bar
        if (synthesiser.trackedEnergy.get(1) > 0) {
            double powerPercentage = ((double) synthesiser.trackedEnergy.get(1)) / synthesiser.energyStorage.getMaxEnergyStored();
            graphics.blit(background,
                    leftPos + POWER_BAR_X_OFFSET,
                    topPos + POWER_BAR_Y_OFFSET + (int) ceil(POWER_BAR_HEIGHT - POWER_BAR_HEIGHT * powerPercentage),
                    POWER_BAR_TEXTURE_X_OFFSET,
                    POWER_BAR_TEXTURE_Y_OFFSET,
                    POWER_BAR_WIDTH,
                    (int) ceil(POWER_BAR_HEIGHT * powerPercentage));
        }

        // Progress "Bar"
        KillRecorderData killRecorderData = synthesiser.itemHandler.getStackInSlot(0).getOrDefault(XPSynthesiser.KILL_RECORDER_DATA_COMPONENT.get(), KillRecorderData.createEmpty());
        //LOGGER.info("Progress {}, Target: {}, Percentage Complete: {}", progress, killRecorderData.getRecordingEnd() - killRecorderData.getRecordingStart(), completionPercentage);
        if (synthesiser.progress > 0 && killRecorderData.recordingEnd() > 0) {
            int progress = synthesiser.trackedProgress.get(0);
            double completionPercentage = ((double) progress) / (killRecorderData.recordingEnd() - killRecorderData.recordingStart());
            graphics.blit(background,
                    leftPos + PROGRESS_BAR_X_OFFSET,
                    topPos + PROGRESS_BAR_Y_OFFSET + (int) floor(PROGRESS_BAR_HEIGHT - PROGRESS_BAR_HEIGHT * completionPercentage),
                    PROGRESS_BAR_TEXTURE_X_OFFSET,
                    PROGRESS_BAR_TEXTURE_Y_OFFSET + (int) floor(PROGRESS_BAR_HEIGHT - PROGRESS_BAR_HEIGHT * completionPercentage),
                    PROGRESS_BAR_WIDTH,
                    (int) ceil(PROGRESS_BAR_HEIGHT * completionPercentage));
        }


        // Render things after widgets (tooltips)
        //this.renderWithTooltip(graphics, mouseX, mouseY, partialTick); // @mcp: renderTooltip = renderHoveredToolTip
        if (mouseX > leftPos + POWER_BAR_X_OFFSET && mouseX < leftPos + POWER_BAR_X_OFFSET + POWER_BAR_WIDTH && mouseY > topPos + POWER_BAR_Y_OFFSET && mouseY < topPos + POWER_BAR_Y_OFFSET + POWER_BAR_HEIGHT)
            graphics.renderTooltip(font, Language.getInstance().getVisualOrder(Arrays.asList(
                    Component.literal(String.valueOf(menu.getSynthesiser().trackedEnergy.get(1)))
            )), mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        XPSynthesiserBlockEntity synthesiser = menu.getSynthesiser();

        // Draw title
        MutableComponent title = Component.translatable("block.xp_synthesiser.xp_synthesiser");
        graphics.drawString(font, title,
                (this.imageWidth - font.width(title)) / 2,
                LABEL_YPOS, 0xFF404040, false);
        MutableComponent xpAmount = Component.literal(String.valueOf(
                HelperFunctions.roundTo(
                    HelperFunctions.getLevelFromXP(synthesiser.trackedProgress.get(1)), 2
                )
        ));
        graphics.drawString(font, xpAmount,
                XP_LABEL_XPOS - font.width(xpAmount) / 2,
                XP_LABEL_YPOS, 0x039904, false);

        // Draw progress
        if (!synthesiser.itemHandler.getStackInSlot(0).equals(ItemStack.EMPTY) &&
                synthesiser.itemHandler.getStackInSlot(0).getItem() instanceof KillRecorderItem) {

            KillRecorderData killRecorderData = synthesiser.itemHandler.getStackInSlot(0).getOrDefault(XPSynthesiser.KILL_RECORDER_DATA_COMPONENT.get(), KillRecorderData.createEmpty());

            // No recording
            if (killRecorderData.recordingEnd() == 0) {
                MutableComponent noValidRecording = Component.literal("NO VALID RECORDING");
                graphics.drawString(font, noValidRecording,
                        (this.imageWidth - font.width(noValidRecording)) / 2 + 1,
                        PROGRESS_LABEL_YPOS, 0xff0000, false);

                // Not enough power
            } else if (HelperFunctions.getTickCost(killRecorderData.xp(), (int) (killRecorderData.recordingEnd() - killRecorderData.recordingStart()))
                    > synthesiser.trackedEnergy.get(1) * 2) {
                MutableComponent notEnoughPower = Component.literal("NOT ENOUGH POWER");
                graphics.drawString(font, notEnoughPower,
                        (this.imageWidth - font.width(notEnoughPower)) / 2 + 1,
                        PROGRESS_LABEL_YPOS, 0xff0000, false);

                // Valid power
            } else {
                MutableComponent progressAmount = Component.literal(String.valueOf(
                        Math.min(
                                (int) ceil((((double) synthesiser.trackedProgress.get(0)) /
                                        (killRecorderData.recordingEnd() - killRecorderData.recordingStart())) * 100 + 2)
                                , 100)
                ) + "%");
                graphics.drawString(font, progressAmount,
                        (this.imageWidth - font.width(progressAmount)) / 2 + 1,
                        PROGRESS_LABEL_YPOS, 0xFF404040, false);
            }
        }
    }

    public void addLevelToSynthesiser() {
        XPSynthesiserBlockEntity synthesiser = menu.getSynthesiser();

        // Check if player has a level to give
        if (this.minecraft != null && this.minecraft.player != null && this.minecraft.player.totalExperience > 0) {
            // Deduct nearest level from player
            double playerXpLevel = HelperFunctions.getLevelFromXP(this.minecraft.player.totalExperience);
            int flatLevelXP = HelperFunctions.getXPfromLevel(Math.floor(playerXpLevel));
            if (flatLevelXP == this.minecraft.player.totalExperience) {
                // If you have a level exactly
                flatLevelXP = HelperFunctions.getXPfromLevel(Math.floor(playerXpLevel - 1));
            }
            int amountToGive = this.minecraft.player.totalExperience - flatLevelXP;
            //this.minecraft.player.totalExperience = this.minecraft.player.totalExperience - amountToGive;
            int newAmount = this.minecraft.player.totalExperience - amountToGive;
            this.minecraft.player.setExperienceValues( newAmount -
                    HelperFunctions.getXPfromLevel(Math.floor(HelperFunctions.getLevelFromXP(newAmount))),
                    newAmount,
                    (int)  Math.floor(HelperFunctions.getLevelFromXP(newAmount)));

            // Add equivalent experience to block entity
            synthesiser.trackedProgress.set(1, synthesiser.trackedProgress.get(1) + amountToGive);
        }

    }

    public void removeLevelFromSynthesiser() {
        XPSynthesiserBlockEntity synthesiser = menu.getSynthesiser();

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
            LOGGER.info("Player: {}, amount: {}", this.minecraft.player, amountToGive);
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.giveExperiencePoints(amountToGive);

            }

        }


    }
}
