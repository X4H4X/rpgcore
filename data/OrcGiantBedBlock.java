package com.rpgcore.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class OrcGiantBedBlock extends BedBlock {

    public enum GiantBedSection implements StringRepresentable {
        FOOT("foot"),
        MIDDLE("middle"),
        HEAD("head");

        private final String name;

        GiantBedSection(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final EnumProperty<GiantBedSection> SECTION = EnumProperty.create("section", GiantBedSection.class);
    protected static final VoxelShape GIANT_BED_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 9.0D, 16.0D);

    public OrcGiantBedBlock(DyeColor color, Properties properties) {
        super(color, properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, BedPart.FOOT)
                .setValue(SECTION, GiantBedSection.FOOT)
                .setValue(OCCUPIED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SECTION);
    }

    /**
     * ИСПРАВЛЕНО (image_a5d2a3.jpg):
     * Возвращаем null вместо BedBlockEntity, чтобы ванильный BedRenderer НЕ рендерил вторую кровать
     * поверх наших 3 секций и НЕ вызывал мерцания (Z-fighting) текстур!
     */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return GIANT_BED_SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction dir = ctx.getHorizontalDirection();
        BlockPos footPos = ctx.getClickedPos();
        BlockPos midPos = footPos.relative(dir);
        BlockPos headPos = midPos.relative(dir);
        Level level = ctx.getLevel();

        if (level.getBlockState(midPos).canBeReplaced(ctx) && level.getWorldBorder().isWithinBounds(midPos)
                && level.getBlockState(headPos).canBeReplaced(ctx) && level.getWorldBorder().isWithinBounds(headPos)) {
            return this.defaultBlockState()
                    .setValue(FACING, dir)
                    .setValue(PART, BedPart.FOOT)
                    .setValue(SECTION, GiantBedSection.FOOT);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            Direction dir = state.getValue(FACING);
            BlockPos midPos = pos.relative(dir);
            BlockPos headPos = midPos.relative(dir);

            level.setBlock(midPos, state.setValue(PART, BedPart.FOOT).setValue(SECTION, GiantBedSection.MIDDLE), 3);
            level.setBlock(headPos, state.setValue(PART, BedPart.HEAD).setValue(SECTION, GiantBedSection.HEAD), 3);
            level.blockUpdated(pos, Blocks.AIR);
            state.updateNeighbourShapes(level, pos, 3);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            Direction dir = state.getValue(FACING);
            GiantBedSection sec = state.getValue(SECTION);

            BlockPos footPos = switch (sec) {
                case FOOT -> pos;
                case MIDDLE -> pos.relative(dir.getOpposite());
                case HEAD -> pos.relative(dir.getOpposite(), 2);
            };
            BlockPos midPos = footPos.relative(dir);
            BlockPos headPos = footPos.relative(dir, 2);

            if (!footPos.equals(pos) && level.getBlockState(footPos).is(this)) {
                level.setBlock(footPos, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, footPos, Block.getId(level.getBlockState(footPos)));
            }
            if (!midPos.equals(pos) && level.getBlockState(midPos).is(this)) {
                level.setBlock(midPos, Blocks.AIR.defaultBlockState(), 35);
            }
            if (!headPos.equals(pos) && level.getBlockState(headPos).is(this)) {
                level.setBlock(headPos, Blocks.AIR.defaultBlockState(), 35);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.CONSUME;
        }
        Direction dir = state.getValue(FACING);
        GiantBedSection sec = state.getValue(SECTION);

        BlockPos headPos = switch (sec) {
            case FOOT -> pos.relative(dir, 2);
            case MIDDLE -> pos.relative(dir);
            case HEAD -> pos;
        };

        BlockState headState = level.getBlockState(headPos);
        if (!headState.is(this)) {
            return InteractionResult.FAIL;
        }
        return super.useWithoutItem(headState, level, headPos, player, hit);
    }

    @Override
    public boolean isBed(BlockState state, BlockGetter level, BlockPos pos, @Nullable LivingEntity sleeper) {
        return true;
    }
}