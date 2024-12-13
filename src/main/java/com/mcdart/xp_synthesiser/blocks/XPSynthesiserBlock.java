package com.mcdart.xp_synthesiser.blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

public class XPSynthesiserBlock extends Block implements EntityBlock {
    // Constructor deferring to super.
    public XPSynthesiserBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    // Return a new instance of our block entity here.
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new XPSynthesiserBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return level.isClientSide() ? null : XPSynthesiserBlockEntity::tick;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            var hand = serverPlayer.getUsedItemHand();
            BlockEntity be = level.getBlockEntity(pos);
            if (hand != InteractionHand.MAIN_HAND || !(be instanceof XPSynthesiserBlockEntity)) return InteractionResult.PASS;

            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, nplayer) -> new SynthesiserMenu(containerId, playerInventory, pos),
                    Component.translatable("block.xp_synthesiser.xp_synthesiser")
            ), pos);

            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;

    }

    @Override
    public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (newState.getBlock() != this) {
            BlockEntity tileEntity = worldIn.getBlockEntity(pos);
            if (tileEntity instanceof XPSynthesiserBlockEntity synthesiser) {
                if (!synthesiser.itemHandler.getStackInSlot(0).equals(ItemStack.EMPTY)) {
                   Containers.dropItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), ((XPSynthesiserBlockEntity) tileEntity).itemHandler.getStackInSlot(0));
                }
            }
            super.onRemove(state, worldIn, pos, newState, isMoving);
        }
    }

}
