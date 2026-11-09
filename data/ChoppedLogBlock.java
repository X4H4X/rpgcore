package com.rpgcore.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChoppedLogBlock extends Block {

    // Стадия стесанного ствола: от 1 (слегка подрублен, 14x16x14) до 7 (тонкая сердцевина 2x16x2 перед падением дерева)
    public static final IntegerProperty CHOPS = IntegerProperty.create("chops", 1, 7);
    // Тип древесины (0=oak, 1=spruce, 2=birch, 3=jungle, 4=acacia, 5=dark_oak, 6=mangrove, 7=cherry, 8=crimson, 9=warped)
    public static final IntegerProperty WOOD_TYPE = IntegerProperty.create("wood_type", 0, 9);

    private static final VoxelShape[] SHAPES = new VoxelShape[8];

    static {
        for (int c = 1; c <= 7; c++) {
            double inset = c * 1.0D; // С каждым подрубом ствол сужается на 1 пиксель с каждой стороны!
            SHAPES[c] = Block.box(inset, 0.0D, inset, 16.0D - inset, 16.0D, 16.0D - inset);
        }
    }

    public ChoppedLogBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(CHOPS, 1)
                .setValue(WOOD_TYPE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHOPS, WOOD_TYPE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(CHOPS)];
    }

    public static int getWoodTypeIndex(Block logBlock) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(logBlock);
        String path = id.getPath();
        if (path.contains("spruce")) return 1;
        if (path.contains("birch")) return 2;
        if (path.contains("jungle")) return 3;
        if (path.contains("acacia")) return 4;
        if (path.contains("dark_oak")) return 5;
        if (path.contains("mangrove")) return 6;
        if (path.contains("cherry")) return 7;
        if (path.contains("crimson")) return 8;
        if (path.contains("warped")) return 9;
        return 0;
    }

    public static Block getLogBlockFromIndex(int index) {
        return switch (index) {
            case 1 -> Blocks.SPRUCE_LOG;
            case 2 -> Blocks.BIRCH_LOG;
            case 3 -> Blocks.JUNGLE_LOG;
            case 4 -> Blocks.ACACIA_LOG;
            case 5 -> Blocks.DARK_OAK_LOG;
            case 6 -> Blocks.MANGROVE_LOG;
            case 7 -> Blocks.CHERRY_LOG;
            case 8 -> Blocks.CRIMSON_STEM;
            case 9 -> Blocks.WARPED_STEM;
            default -> Blocks.OAK_LOG;
        };
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(getLogBlockFromIndex(state.getValue(WOOD_TYPE)));
    }
}