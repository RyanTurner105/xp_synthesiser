package com.mcdart.xp_synthesiser.blocks;

import com.mcdart.xp_synthesiser.XPSynthesiser;
import com.mcdart.xp_synthesiser.items.KillRecorderData;
import com.mcdart.xp_synthesiser.items.KillRecorderItem;
import com.mcdart.xp_synthesiser.util.HelperFunctions;
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
import org.jetbrains.annotations.NotNull;
import java.util.List;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;

public class SynthesiserScreen extends AbstractContainerScreen<SynthesiserMenu> {
    // Positioning
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

    // Buttons
    public static final int PLUS_ONE_BUTTON_ID = 1;
    public static final int MINUS_ONE_BUTTON_ID = 2;
    public static final int PLUS_TEN_BUTTON_ID = 3;
    public static final int MINUS_TEN_BUTTON_ID = 4;
    private static final int BUTTON_ONE_X_OFFSET = 149;
    private static final int BUTTON_TEN_X_OFFSET = 146;
    private static final int BUTTON_ONE_HEIGHT = 13;
    private static final int BUTTON_ONE_WIDTH = 17;
    private static final int BUTTON_TEN_HEIGHT = 13;
    private static final int BUTTON_TEN_WIDTH = 23;

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

        if (minecraft != null && minecraft.gameMode != null) {
            this.addRenderableWidget(new ImageButton(leftPos + BUTTON_ONE_X_OFFSET, topPos + 25, BUTTON_ONE_WIDTH, BUTTON_ONE_HEIGHT,
                    new WidgetSprites(
                            ResourceLocation.fromNamespaceAndPath(XPSynthesiser.MODID, "button/plus_button_one"),
                            ResourceLocation.fromNamespaceAndPath(XPSynthesiser.MODID, "button/plus_button_one")
                    ),
                    (element)->minecraft.gameMode.handleInventoryButtonClick(menu.containerId, PLUS_ONE_BUTTON_ID)
            ));
            this.addRenderableWidget(new ImageButton(leftPos + BUTTON_ONE_X_OFFSET, topPos + XP_LABEL_YPOS + 11, BUTTON_ONE_WIDTH, BUTTON_ONE_HEIGHT,
                    new WidgetSprites(
                            ResourceLocation.fromNamespaceAndPath(XPSynthesiser.MODID, "button/minus_button_one"),
                            ResourceLocation.fromNamespaceAndPath(XPSynthesiser.MODID, "button/minus_button_one")
                    ),
                    (element)->minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MINUS_ONE_BUTTON_ID)
            ));
            this.addRenderableWidget(new ImageButton(leftPos + BUTTON_TEN_X_OFFSET, topPos + 10, BUTTON_TEN_WIDTH, BUTTON_TEN_HEIGHT,
                    new WidgetSprites(
                            ResourceLocation.fromNamespaceAndPath(XPSynthesiser.MODID, "button/plus_button_ten"),
                            ResourceLocation.fromNamespaceAndPath(XPSynthesiser.MODID, "button/plus_button_ten")
                    ),
                    (element)->minecraft.gameMode.handleInventoryButtonClick(menu.containerId, PLUS_TEN_BUTTON_ID)
            ));
            this.addRenderableWidget(new ImageButton(leftPos + BUTTON_TEN_X_OFFSET, topPos + XP_LABEL_YPOS + 26, BUTTON_TEN_WIDTH, BUTTON_TEN_HEIGHT,
                    new WidgetSprites(
                            ResourceLocation.fromNamespaceAndPath(XPSynthesiser.MODID, "button/minus_button_ten"),
                            ResourceLocation.fromNamespaceAndPath(XPSynthesiser.MODID, "button/minus_button_ten")
                    ),
                    (element)->minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MINUS_TEN_BUTTON_ID)
            ));
        }
    }

    // mouseX and mouseY indicate the scaled coordinates of where the cursor is in on the screen
    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        // Background is typically rendered first
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // Then the widgets if this is a direct child of the Screen
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void renderBg(GuiGraphics graphics, float pPartialTick, int mouseX, int mouseY) {
        // Render things here before widgets (background textures)
        graphics.blit(background, leftPos, topPos, 0, 0, MAIN_SCREEN_WIDTH, MAIN_SCREEN_HEIGHT);

        XPSynthesiserBlockEntity synthesiser = menu.getSynthesiser();

        // Energy Bar
        if (synthesiser.trackedEnergy.get(1) > 0) {
            double powerPercentage = ((double) synthesiser.trackedEnergy.get(1)) / synthesiser.energyStorage.getMaxEnergyStored();
            graphics.blit(background,
                    leftPos + POWER_BAR_X_OFFSET,
                    topPos + POWER_BAR_Y_OFFSET + (int) floor(POWER_BAR_HEIGHT - POWER_BAR_HEIGHT * powerPercentage),
                    POWER_BAR_TEXTURE_X_OFFSET,
                    POWER_BAR_TEXTURE_Y_OFFSET,
                    POWER_BAR_WIDTH,
                    (int) ceil(POWER_BAR_HEIGHT * powerPercentage));
        }

        // Progress "Bar"
        KillRecorderData killRecorderData = synthesiser.itemHandler.getStackInSlot(0).getOrDefault(XPSynthesiser.KILL_RECORDER_DATA_COMPONENT.get(), KillRecorderData.createEmpty());
        if (synthesiser.trackedProgress.get(0) > 0 && killRecorderData.recordingEnd() > 0) {
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
        // Power tooltip
        int energyToDisplay = synthesiser.trackedEnergy.get(1) > synthesiser.energyStorage.getMaxEnergyStored() - 2 ?
                synthesiser.energyStorage.getMaxEnergyStored() : synthesiser.trackedEnergy.get(1);
        if (mouseX > leftPos + POWER_BAR_X_OFFSET && mouseX < leftPos + POWER_BAR_X_OFFSET + POWER_BAR_WIDTH &&
                mouseY > topPos + POWER_BAR_Y_OFFSET && mouseY < topPos + POWER_BAR_Y_OFFSET + POWER_BAR_HEIGHT)
            graphics.renderTooltip(font, Language.getInstance().getVisualOrder(List.of(
                    Component.literal((energyToDisplay > 1000 ? energyToDisplay / 1000 + " k" : energyToDisplay) + "FE/1000 kFE")
            )), mouseX, mouseY);

        // Needs kill recorder tooltip
        if (!(synthesiser.itemHandler.getStackInSlot(0).getItem() instanceof KillRecorderItem) &&
                mouseX > leftPos + SynthesiserMenu.RECORDER_SLOT_XPOS && mouseX < leftPos + SynthesiserMenu.RECORDER_SLOT_XPOS + SynthesiserMenu.SLOT_X_SPACING &&
                mouseY > topPos + SynthesiserMenu.RECORDER_SLOT_YPOS && mouseY < topPos + SynthesiserMenu.RECORDER_SLOT_YPOS + SynthesiserMenu.SLOT_Y_SPACING)
            graphics.renderTooltip(font, Language.getInstance().getVisualOrder(List.of(
                    Component.literal("Kill Recorder necessary to run")
            )), mouseX, mouseY);

        // Experience tooltip
        if (mouseX > leftPos + XP_LABEL_XPOS - 15 && mouseX < leftPos + XP_LABEL_XPOS + 15 &&
                mouseY > topPos + XP_LABEL_YPOS - 2 && mouseY < topPos + XP_LABEL_YPOS + 8)
            graphics.renderTooltip(font, Language.getInstance().getVisualOrder(List.of(
                    Component.literal("XP Levels stored")
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

        // Draw XP
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
                    > synthesiser.trackedEnergy.get(1)) {
                MutableComponent notEnoughPower = Component.literal("NOT ENOUGH POWER");
                graphics.drawString(font, notEnoughPower,
                        (this.imageWidth - font.width(notEnoughPower)) / 2 + 1,
                        PROGRESS_LABEL_YPOS, 0xff0000, false);

                // Valid power
            } else {
                MutableComponent progressAmount = Component.literal(Math.min(
                        (int) ceil((((double) synthesiser.trackedProgress.get(0)) /
                                (killRecorderData.recordingEnd() - killRecorderData.recordingStart())) * 100 + 2)
                        , 100) + "%");
                graphics.drawString(font, progressAmount,
                        (this.imageWidth - font.width(progressAmount)) / 2 + 1,
                        PROGRESS_LABEL_YPOS, 0xFF404040, false);
            }
        }
    }
}
