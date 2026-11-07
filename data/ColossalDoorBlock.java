package com.rpgcore.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ColossalDoorBlock extends Block {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
    public static final IntegerProperty COL_X = IntegerProperty.create("col_x", 0, 2);
    public static final IntegerProperty LAYER_Y = IntegerProperty.create("layer_y", 0, 3);

    protected static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
    protected static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape WEST_AABB = Block.box(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape EAST_AABB = Block.box(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);

    private final BlockSetType type;

    public ColossalDoorBlock(BlockSetType type, Properties properties) {
        super(properties);
        this.type = type;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(HINGE, DoorHingeSide.LEFT)
                .setValue(COL_X, 0)
                .setValue(LAYER_Y, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, HINGE, COL_X, LAYER_Y);
    }

    // В закрытом состоянии дверь идёт вширь от петель (col_x = 0..2),
    // а в ОТКРЫТОМ состоянии дверь распахивается на 90 градусов и идёт вглубь вдоль боковой стены (facing)!
    public static Direction getSpanDirection(Direction facing, DoorHingeSide hinge, boolean open) {
        if (open) {
            return facing;
        }
        return hinge == DoorHingeSide.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
    }

    public static BlockPos getOriginPos(BlockPos pos, BlockState state) {
        Direction spanDir = getSpanDirection(state.getValue(FACING), state.getValue(HINGE), state.getValue(OPEN));
        int col = state.getValue(COL_X);
        int row = state.getValue(LAYER_Y);
        return pos.relative(spanDir.getOpposite(), col).below(row);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction dir = state.getValue(FACING);
        boolean closed = !state.getValue(OPEN);
        boolean rightHinge = (state.getValue(HINGE) == DoorHingeSide.RIGHT);

        return switch (dir) {
            case EAST -> closed ? EAST_AABB : (rightHinge ? NORTH_AABB : SOUTH_AABB);
            case SOUTH -> closed ? SOUTH_AABB : (rightHinge ? EAST_AABB : WEST_AABB);
            case WEST -> closed ? WEST_AABB : (rightHinge ? SOUTH_AABB : NORTH_AABB);
            default -> closed ? NORTH_AABB : (rightHinge ? WEST_AABB : EAST_AABB);
        };
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return switch (pathComputationType) {
            case LAND, AIR -> state.getValue(OPEN);
            case WATER -> false;
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos clickedPos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        Direction facing = ctx.getHorizontalDirection();
        DoorHingeSide hinge = getHinge(ctx, facing);

        BlockPos placeOrigin = clickedPos;
        if (hinge == DoorHingeSide.RIGHT) {
            Direction rightToLeft = getSpanDirection(facing, DoorHingeSide.RIGHT, false);
            BlockState leftNeighbor = level.getBlockState(clickedPos.relative(rightToLeft));
            if (leftNeighbor.getBlock() instanceof ColossalDoorBlock
                    && leftNeighbor.getValue(FACING) == facing
                    && leftNeighbor.getValue(HINGE) == DoorHingeSide.LEFT) {
                placeOrigin = clickedPos.relative(rightToLeft.getOpposite(), 2);
            }
        }

        if (placeOrigin.getY() >= level.getMaxBuildHeight() - 3) return null;

        Direction widthDir = getSpanDirection(facing, hinge, false);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 3; x++) {
                BlockPos target = placeOrigin.relative(widthDir, x).above(y);
                if (!level.getBlockState(target).canBeReplaced(ctx)) {
                    return null;
                }
                if (y == 0 && !level.getBlockState(target.below()).isFaceSturdy(level, target.below(), Direction.UP)) {
                    return null;
                }
            }
        }

        if (!placeOrigin.equals(clickedPos)) {
            if (!level.isClientSide()) {
                BlockState baseState = this.defaultBlockState()
                        .setValue(FACING, facing)
                        .setValue(HINGE, hinge)
                        .setValue(OPEN, false);
                for (int y = 0; y < 4; y++) {
                    for (int x = 0; x < 3; x++) {
                        BlockPos target = placeOrigin.relative(widthDir, x).above(y);
                        level.setBlock(target, baseState.setValue(COL_X, x).setValue(LAYER_Y, y), 3);
                    }
                }
            }
            return this.defaultBlockState()
                    .setValue(FACING, facing)
                    .setValue(HINGE, hinge)
                    .setValue(OPEN, false)
                    .setValue(COL_X, 2)
                    .setValue(LAYER_Y, 0);
        }

        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(HINGE, hinge)
                .setValue(OPEN, false)
                .setValue(COL_X, 0)
                .setValue(LAYER_Y, 0);
    }

    private DoorHingeSide getHinge(BlockPlaceContext ctx, Direction facing) {
        BlockGetter level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Direction ccw = facing.getCounterClockWise();
        Direction cw = facing.getClockWise();

        for (int d = 1; d <= 3; d++) {
            BlockState s = level.getBlockState(pos.relative(ccw, d));
            if (s.getBlock() instanceof ColossalDoorBlock && s.getValue(FACING) == facing && s.getValue(HINGE) == DoorHingeSide.LEFT) {
                return DoorHingeSide.RIGHT;
            }
        }
        for (int d = 1; d <= 3; d++) {
            BlockState s = level.getBlockState(pos.relative(cw, d));
            if (s.getBlock() instanceof ColossalDoorBlock && s.getValue(FACING) == facing && s.getValue(HINGE) == DoorHingeSide.RIGHT) {
                return DoorHingeSide.LEFT;
            }
        }

        Vec3 clickLoc = ctx.getClickLocation();
        double dx = clickLoc.x - pos.getX();
        double dz = clickLoc.z - pos.getZ();
        int stepX = facing.getStepX();
        int stepZ = facing.getStepZ();
        return (stepX < 0 && dz < 0.5D) || (stepX > 0 && dz > 0.5D) || (stepZ < 0 && dx > 0.5D) || (stepZ > 0 && dx < 0.5D)
                ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (state.getValue(COL_X) != 0) return;
        Direction facing = state.getValue(FACING);
        DoorHingeSide hinge = state.getValue(HINGE);
        Direction widthDir = getSpanDirection(facing, hinge, false);

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 3; x++) {
                if (x == 0 && y == 0) continue;
                BlockPos target = pos.relative(widthDir, x).above(y);
                level.setBlock(target, state.setValue(COL_X, x).setValue(LAYER_Y, y), 3);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!this.type.canOpenByHand()) {
            return InteractionResult.PASS;
        }

        BlockPos myOrigin = getOriginPos(pos, state);
        Direction facing = state.getValue(FACING);
        DoorHingeSide hinge = state.getValue(HINGE);
        boolean newOpen = !state.getValue(OPEN);

        // Ищем парную вторую дверь ДО переключения первой
        BlockPos partnerOrigin = findPartnerDoorOrigin(level, myOrigin, facing, hinge);

        if (!level.isClientSide()) {
            swingWholeDoor(level, myOrigin, facing, hinge, !newOpen, newOpen);
            if (partnerOrigin != null) {
                BlockState partnerState = level.getBlockState(partnerOrigin);
                if (partnerState.is(this)) {
                    swingWholeDoor(level, partnerOrigin, facing, partnerState.getValue(HINGE), partnerState.getValue(OPEN), newOpen);
                }
            }
        }

        level.playSound(player, pos, newOpen ? this.type.doorOpen() : this.type.doorClose(), SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        level.gameEvent(player, newOpen ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // Поворачивает всё полотно двери 3x4 целиком вокруг колонки петель (col_x = 0):
    // убирает старые блоки из проёма и выставляет единую распашную створку вдоль стены (исправлен image_3e8824.jpg!)
    private void swingWholeDoor(Level level, BlockPos hingeOrigin, Direction facing, DoorHingeSide hinge, boolean oldOpen, boolean newOpen) {
        Direction oldSpan = getSpanDirection(facing, hinge, oldOpen);
        Direction newSpan = getSpanDirection(facing, hinge, newOpen);

        // 1. Очищаем блоки, которые занимало полотно в предыдущем положении (кроме самой оси петель x == 0)
        for (int y = 0; y < 4; y++) {
            for (int x = 1; x < 3; x++) {
                BlockPos oldPos = hingeOrigin.relative(oldSpan, x).above(y);
                if (level.getBlockState(oldPos).is(this)) {
                    level.setBlock(oldPos, Blocks.AIR.defaultBlockState(), 35);
                }
            }
        }

        // 2. Выстраиваем цельное полотно 3x4 в новом положении (при открытии — вдоль стены, при закрытии — поперёк проёма)
        BlockState baseState = this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(HINGE, hinge)
                .setValue(OPEN, newOpen);

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 3; x++) {
                BlockPos newPos = hingeOrigin.relative(newSpan, x).above(y);
                BlockState existing = level.getBlockState(newPos);
                if (x == 0 || existing.isAir() || existing.canBeReplaced() || existing.is(this)) {
                    level.setBlock(newPos, baseState.setValue(COL_X, x).setValue(LAYER_Y, y), 10);
                }
            }
        }
    }

    @Nullable
    private BlockPos findPartnerDoorOrigin(Level level, BlockPos myOrigin, Direction facing, DoorHingeSide myHinge) {
        Direction closedSpan = getSpanDirection(facing, myHinge, false);
        // У двойных ворот 3x4 базы петель (col_x = 0) стоят на расстоянии 5 блоков друг от друга вдоль линии проёма!
        BlockPos expectedPartnerHinge = myOrigin.relative(closedSpan, 5);
        BlockState candidate = level.getBlockState(expectedPartnerHinge);
        if (candidate.is(this)
                && candidate.getValue(FACING) == facing
                && candidate.getValue(HINGE) != myHinge
                && candidate.getValue(COL_X) == 0) {
            return expectedPartnerHinge;
        }
        return null;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos origin = getOriginPos(pos, state);
            Direction spanDir = getSpanDirection(state.getValue(FACING), state.getValue(HINGE), state.getValue(OPEN));

            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 3; x++) {
                    BlockPos target = origin.relative(spanDir, x).above(y);
                    if (!target.equals(pos) && level.getBlockState(target).is(this)) {
                        level.setBlock(target, Blocks.AIR.defaultBlockState(), 35);
                        level.levelEvent(player, 2001, target, Block.getId(state));
                    }
                }
            }
            if (!player.isCreative()) {
                Block.popResource(level, origin, new ItemStack(this));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(LAYER_Y) == 0 && state.getValue(COL_X) == 0) {
            return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        }
        return true;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return mirror == Mirror.NONE ? state : state.rotate(mirror.getRotation(state.getValue(FACING))).cycle(HINGE);
    }
}