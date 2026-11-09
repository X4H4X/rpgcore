package com.rpgcore.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.TriState;

import java.util.function.Supplier;

public class TillableSlabBlock extends SlabBlock {

    private final Supplier<Block> farmlandSlabSupplier;

    public TillableSlabBlock(Properties properties, Supplier<Block> farmlandSlabSupplier) {
        super(properties);
        this.farmlandSlabSupplier = farmlandSlabSupplier;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // Вспахивание полублока земли или дерна любой мотыгой!
        if (stack.getItem() instanceof HoeItem && hit.getDirection() != Direction.DOWN
                && level.getBlockState(pos.above()).isAir()) {
            Block farmlandBlock = farmlandSlabSupplier.get();
            if (farmlandBlock != null) {
                level.playSound(player, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!level.isClientSide) {
                    SlabType currentType = state.getValue(TYPE);
                    boolean waterlogged = state.getValue(WATERLOGGED);
                    BlockState tilledState = farmlandBlock.defaultBlockState()
                            .setValue(TYPE, currentType)
                            .setValue(WATERLOGGED, waterlogged);
                    level.setBlock(pos, tilledState, 11);
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /**
     * Разрешаем сажать на полублок земли и дерна любые саженцы, цветы, грибы, кусты ягод и тростник!
     */
    @Override
    public TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos soilPosition,
                                    Direction facing, BlockState plant) {
        if (facing == Direction.UP) {
            return TriState.TRUE;
        }
        return super.canSustainPlant(state, level, soilPosition, facing, plant);
    }
}