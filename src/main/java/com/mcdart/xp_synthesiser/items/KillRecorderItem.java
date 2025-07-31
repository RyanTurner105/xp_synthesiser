package com.mcdart.xp_synthesiser.items;
import com.mcdart.xp_synthesiser.XPSynthesiser;
import com.mcdart.xp_synthesiser.util.HelperFunctions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class KillRecorderItem extends Item {

    public KillRecorderItem(boolean recording) {
        super(new Properties().stacksTo(1));
    }

    // Listens to a user using this item
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, @NotNull InteractionHand pUsedHand) {
        ItemStack itemStack = pPlayer.getItemInHand(pUsedHand);

        // Only toggle on server side
        if (!pLevel.isClientSide() && pPlayer.isShiftKeyDown()) {
            toggleRecording(pLevel, (ServerPlayer) pPlayer, itemStack);
            pPlayer.setItemInHand(pUsedHand, itemStack);
        } else if (pLevel.isClientSide()){
            return InteractionResultHolder.pass(itemStack);
        }
        return InteractionResultHolder.sidedSuccess(itemStack, pLevel.isClientSide());
    }

    private void toggleRecording(Level pLevel, ServerPlayer player, ItemStack itemStack) {
        KillRecorderData currentData = itemStack.getOrDefault(XPSynthesiser.KILL_RECORDER_DATA_COMPONENT.get(), KillRecorderData.createEmpty());

        long currentTime = pLevel.getGameTime();
        if (!currentData.recording()) {
            // Start recording - clear previous data and start fresh
            itemStack.update(
                    XPSynthesiser.KILL_RECORDER_DATA_COMPONENT.get(),
                    currentData,
                    data -> new KillRecorderData(true, 0, currentTime, 0L)
            );
            player.displayClientMessage(Component.literal("Kill Recording: Started (Data Cleared)"), true);
        } else {
            // Stop recording
            itemStack.update(
                    XPSynthesiser.KILL_RECORDER_DATA_COMPONENT.get(),
                    currentData,
                    data -> new KillRecorderData(false, data.xp(), data.recordingStart(), currentTime)
            );
            player.displayClientMessage(Component.literal("Kill Recording: Stopped"), true);
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemStack, Item.@NotNull TooltipContext ttc, @NotNull List<Component> component, @NotNull TooltipFlag ttf) {
        super.appendHoverText(itemStack, ttc, component, ttf);
        KillRecorderData krData = itemStack.getOrDefault(XPSynthesiser.KILL_RECORDER_DATA_COMPONENT.get(), KillRecorderData.createEmpty());

        if (krData.recordingStart() > 0) {
            if (krData.recordingEnd() == 0) {
                component.add(Component.literal("RECORDING").withStyle(ChatFormatting.RED));
            }

            component.add(Component.literal((krData.recording() ? "Current " : "Stored ") + "XP: §6" + krData.xp()).withStyle(ChatFormatting.BLUE));

            if (krData.recordingEnd() > 0) {
                long timeTaken = krData.recordingEnd() - krData.recordingStart();
                component.add(Component.literal("Ticks Elapsed: §6" + timeTaken).withStyle(ChatFormatting.BLUE));

                if (krData.xp() > 0) {
                    int costPerTick = (int) HelperFunctions.getTickCost(krData.xp(), (int) timeTaken);
                    component.add(Component.literal("Cost Per Tick: §6" + costPerTick).withStyle(ChatFormatting.BLUE));
                    if (costPerTick > 1000000) {
                        component.add(Component.literal("Very high cost detected. " +
                                "This recording is too powerful to work with the XP Synthesiser. " +
                                        "Increase duration of recording to lower power level.")
                                .withStyle(ChatFormatting.RED));
                    }
                }
            }
        } else {
            component.add(Component.literal("Shift and click to start recording").withStyle(ChatFormatting.GOLD));
        }
    }
}
