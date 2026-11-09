package com.rpgcore.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.TriState;

public class FarmlandSlabBlock extends SlabBlock {

    public static final IntegerProperty MOISTURE = BlockStateProperties.MOISTURE;

    protected static final VoxelShape BOTTOM_FARMLAND_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D);
    protected static final VoxelShape TOP_FARMLAND_SHAPE = Block.box(0.0D, 8.0D, 0.0D, 16.0D, 15.0D, 16.0D);
    protected static final VoxelShape DOUBLE_FARMLAND_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D);

    public FarmlandSlabBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(MOISTURE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MOISTURE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        SlabType type = state.getValue(TYPE);
        return switch (type) {
            case DOUBLE -> DOUBLE_FARMLAND_SHAPE;
            case TOP -> TOP_FARMLAND_SHAPE;
            default -> BOTTOM_FARMLAND_SHAPE;
        };
    }

    /**
     * Разрешаем сажать на вспаханный полублок АБСОЛЮТНО ЛЮБЫЕ семена, пшеницу, морковь, картофель, свеклу, цветы и саженцы!
     */
    @Override
    public TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos soilPosition,
                                    Direction facing, BlockState plant) {
        if (facing == Direction.UP) {
            return TriState.TRUE;
        }
        return super.canSustainPlant(state, level, soilPosition, facing, plant);
    }

    @Override
    public boolean isFertile(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(MOISTURE) > 0;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int moisture = state.getValue(MOISTURE);
        if (isNearWater(level, pos) || level.isRainingAt(pos.above())) {
            if (moisture < 7) {
                level.setBlock(pos, state.setValue(MOISTURE, 7), 2);
            }
        } else if (moisture > 0) {
            level.setBlock(pos, state.setValue(MOISTURE, moisture - 1), 2);
        }
    }

    private static boolean isNearWater(LevelReader level, BlockPos pos) {
        if (level.getBlockState(pos).getValue(WATERLOGGED)) return true;
        for (BlockPos check : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 1, 4))) {
            if (level.getFluidState(check).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }
}