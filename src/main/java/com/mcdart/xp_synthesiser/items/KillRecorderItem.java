package com.mcdart.xp_synthesiser.items;

import com.mcdart.xp_synthesiser.XPSynthesiser;
import com.mcdart.xp_synthesiser.util.HelperFunctions;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.font.providers.UnihexProvider;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.data.worldgen.DimensionTypes;
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
import net.minecraft.world.level.dimension.DimensionType;
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

public class KillRecorderItem extends Item {


    // Overloads can be provided to supply the map itself
    public KillRecorderItem(boolean recording) {
        super(new Properties().stacksTo(1));
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
            pPlayer.setItemInHand(pUsedHand, itemStack);
        }
        return InteractionResultHolder.sidedSuccess(itemStack, pLevel.isClientSide());
    }

    private void toggleRecording(Level pLevel, ServerPlayer player, ItemStack itemStack) {
        LOGGER.info("Level: {}", pLevel);
        KillRecorderData currentData = itemStack.getOrDefault(XPSynthesiser.KILL_RECORDER_DATA_COMPONENT.get(), KillRecorderData.createEmpty());

        if (!currentData.recording()) {
            // Start recording - clear previous data and start fresh
            long currentTime = pLevel.getGameTime();
            itemStack.update(
                    XPSynthesiser.KILL_RECORDER_DATA_COMPONENT.get(),
                    currentData,
                    data -> new KillRecorderData(true, 0, currentTime, 0L)
            );
            player.displayClientMessage(Component.literal("Kill Recording: Enabled (Data Cleared)"), true);
        } else {
            // Stop recording
            long currentTime = pLevel.getGameTime();
            itemStack.update(
                    XPSynthesiser.KILL_RECORDER_DATA_COMPONENT.get(),
                    currentData,
                    data -> new KillRecorderData(false, data.xp(), data.recordingStart(), currentTime)
            );
            player.displayClientMessage(Component.literal("Kill Recording: Disabled"), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Item.TooltipContext ttc, List<Component> component, TooltipFlag ttf) {
        super.appendHoverText(itemStack, ttc, component, ttf);
        KillRecorderData krData = itemStack.getOrDefault(XPSynthesiser.KILL_RECORDER_DATA_COMPONENT.get(), KillRecorderData.createEmpty());

        if (krData != null) {
            if (krData.recordingEnd() == 0) {
                component.add(Component.translatable("RECORDING").withStyle(ChatFormatting.RED));
            }

            component.add(Component.translatable((krData.recording() ? "Current " : "Stored ") + "XP: " + krData.xp()).withStyle(ChatFormatting.BLUE));

            // Get current time somehow?
            if (krData.recordingEnd() > 0) {
                long timeTaken = krData.recordingEnd() - krData.recordingStart();
                component.add(Component.translatable("Ticks Elapsed: " + timeTaken).withStyle(ChatFormatting.BLUE));

                if (krData.xp() > 0) {
                    int costPerTick = (int) HelperFunctions.getTickCost(krData.xp(), (int)timeTaken);
                    component.add(Component.translatable("Cost Per Tick: " + costPerTick).withStyle(ChatFormatting.BLUE));
                }
            }
        }
    }

}
