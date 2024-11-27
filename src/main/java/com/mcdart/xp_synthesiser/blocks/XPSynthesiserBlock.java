package com.mcdart.xp_synthesiser.blocks;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import static com.mcdart.xp_synthesiser.XPSynthesiser.XP_SYNTHESISER_BLOCK_ENTITY;

public class XPSynthesiserBlock extends Block implements EntityBlock {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Constructor deferring to super.
    public XPSynthesiserBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    // Return a new instance of our block entity here.
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new XPSynthesiserBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // You can return different tickers here, depending on whatever factors you want. A common use case would be
        // to return different tickers on the client or server, only tick one side to begin with,
        // or only return a ticker for some blockstates (e.g. when using a "my machine is working" blockstate property).
        return level.isClientSide() ? null : XPSynthesiserBlockEntity::tick;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            var hand = serverPlayer.getUsedItemHand();
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof XPSynthesiserBlockEntity blockEntity)) return InteractionResult.PASS;

            LOGGER.info("{},{},{}", be, pos, player);

            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, nplayer) -> new SynthesiserMenu(containerId, playerInventory, pos),
                    Component.translatable("block.xp_synthesiser.xp_synthesiser")
            ), pos);

            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;

    }

}
