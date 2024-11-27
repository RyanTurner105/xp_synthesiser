package com.mcdart.xp_synthesiser.items;

import com.mcdart.xp_synthesiser.XPSynthesiser;
import com.mcdart.xp_synthesiser.util.HelperFunctions;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.sql.Time;
import java.time.LocalTime;
import java.util.List;

public class KillRecorderItem extends Item implements MutableDataComponentHolder {

    public static boolean recording;
    private final PatchedDataComponentMap components;

    // Overloads can be provided to supply the map itself
    public KillRecorderItem(boolean recording) {
        super(new Properties().stacksTo(1));
        KillRecorderItem.recording = recording;
        this.components = new PatchedDataComponentMap(DataComponentMap.EMPTY);
    }

    @Override
    public DataComponentMap getComponents() {
        return this.components;
    }

    @Nullable
    @Override
    public <T> T set(DataComponentType<? super T> componentType, @Nullable T value) {
        return this.components.set(componentType, value);
    }

    @Nullable
    @Override
    public <T> T remove(DataComponentType<? extends T> componentType) {
        return this.components.remove(componentType);
    }

    @Override
    public void applyComponents(DataComponentPatch patch) {
        this.components.applyPatch(patch);
    }

    @Override
    public void applyComponents(DataComponentMap components) {
        this.components.setAll(components);
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    // Listens to a user using this item
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack itemStack = pPlayer.getItemInHand(pUsedHand);

        // Only toggle on server side
        if (!pLevel.isClientSide()) {
            toggleRecording(pLevel, (ServerPlayer) pPlayer, itemStack);
            // ItemStack newItemStack = pPlayer.getItemInHand(pUsedHand);

        }
        return InteractionResultHolder.sidedSuccess(itemStack, pLevel.isClientSide());
    }

    private void toggleRecording(Level pLevel, ServerPlayer player, ItemStack itemStack) {
        LOGGER.info("Level: {}", pLevel);

        var currentData = getRecordingData(itemStack);

        if (currentData != null) {
            LOGGER.info("Is recording data? {}", getRecordingData(itemStack).getRecording());
            currentData.setRecording(!currentData.getRecording());
            // Reset XP and start time if we just started recording
            if (currentData.getRecording()) {
                currentData.setXP(0);
                currentData.setRecordingStart(pLevel.getGameTime());
                currentData.setRecordingEnd(0);
            } else {
                // Set time taken if we just finished recording
                currentData.setRecordingEnd(pLevel.getGameTime());
            }
            LOGGER.info("Recording data changed: Recording: {}, XP: {}, StartTime: {}, EndTime: {}", currentData.getRecording(), currentData.getXP(), currentData.getRecordingStart(), currentData.getRecordingEnd());
            setRecordingData(itemStack, currentData);
        } else {
            // This should only happen if it's the first time it gets turned on
            LOGGER.info("Couldn't get recording data!");
            setRecordingData(itemStack, new KillRecorderData(true, 0, pLevel.getGameTime(), 0));
            LOGGER.info("New recording data: Recording: {}, XP: {}, StartTime: {}, EndTime: {}", getRecordingData(itemStack).getRecording()
                    , getRecordingData(itemStack).getXP(), getRecordingData(itemStack).getRecordingStart(), getRecordingData(itemStack).getRecordingEnd());
        }


    }

    public static KillRecorderData getRecordingData(ItemStack stack) {
        return stack.get(XPSynthesiser.KILL_RECORDER_DATA_COMPONENT);
    }

    public static void setRecordingData(ItemStack stack, KillRecorderData data) {
        stack.set(XPSynthesiser.KILL_RECORDER_DATA_COMPONENT, data);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Item.TooltipContext ttc, List<Component> component, TooltipFlag ttf) {
        super.appendHoverText(itemStack, ttc, component, ttf);
        KillRecorderData krData = getRecordingData(itemStack);

        if (krData != null) {
            if (krData.getRecordingEnd() == 0) {
                component.add(Component.translatable("RECORDING").withStyle(ChatFormatting.RED));
            }

            component.add(Component.translatable((recording ? "Current " : "Stored ") + "XP: " + krData.getXP()).withStyle(ChatFormatting.BLUE));

            // Get current time somehow?
            if (krData.getRecordingEnd() > 0) {
                long timeTaken = krData.getRecordingEnd() - krData.getRecordingStart();
                component.add(Component.translatable("Ticks Elapsed: " + timeTaken).withStyle(ChatFormatting.BLUE));

                if (krData.getXP() > 0) {
                    int costPerTick = (int) HelperFunctions.getTickCost(krData.getXP(), (int)timeTaken);
                    component.add(Component.translatable("Cost Per Tick: " + costPerTick).withStyle(ChatFormatting.BLUE));
                }
            }
        }
    }

}
