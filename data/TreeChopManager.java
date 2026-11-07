package com.rpgcore.data;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class TreeChopManager {

    private static final Long2IntOpenHashMap TREE_CHOP_COUNTS = new Long2IntOpenHashMap();

    public static boolean isLog(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(ModItemsAndBlocks.CHOPPED_LOG.get());
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.isShiftKeyDown()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        if (!isLog(state)) return;

        TreeScanResult tree = scanTree(level, pos);
        if (!tree.hasLeaves || tree.logs.size() <= 1) {
            if (state.is(ModItemsAndBlocks.CHOPPED_LOG.get())) {
                event.setCanceled(true);
                Block dropLog = ChoppedLogBlock.getLogBlockFromIndex(state.getValue(ChoppedLogBlock.WOOD_TYPE));
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                if (!player.isCreative()) {
                    Block.popResource(level, pos, new ItemStack(dropLog));
                }
            }
            return;
        }

        event.setCanceled(true);

        int totalLogs = tree.logs.size();
        int requiredChops = Math.max(2, Math.min(24, (int) Math.round(Math.pow(totalLogs, 0.68D))));

        long treeKey = tree.basePos.asLong();
        int currentChops = TREE_CHOP_COUNTS.getOrDefault(treeKey, 0) + 1;

        ItemStack tool = player.getMainHandItem();
        if (!player.isCreative() && tool.isDamageableItem()) {
            tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }

        if (currentChops < requiredChops) {
            TREE_CHOP_COUNTS.put(treeKey, currentChops);

            int visualStage = Mth.clamp(Math.round(((float) currentChops / (float) requiredChops) * 7.0F), 1, 7);
            int woodType = state.is(ModItemsAndBlocks.CHOPPED_LOG.get())
                    ? state.getValue(ChoppedLogBlock.WOOD_TYPE)
                    : ChoppedLogBlock.getWoodTypeIndex(state.getBlock());

            BlockState choppedState = ModItemsAndBlocks.CHOPPED_LOG.get().defaultBlockState()
                    .setValue(ChoppedLogBlock.CHOPS, visualStage)
                    .setValue(ChoppedLogBlock.WOOD_TYPE, woodType);

            level.setBlock(pos, choppedState, 3);
            level.levelEvent(2001, pos, Block.getId(state));
            level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, 0.85F + (visualStage * 0.04F));
        } else {
            TREE_CHOP_COUNTS.remove(treeKey);
            fellEntireTree(level, player, pos, tree, tool);
        }
    }

    private static void fellEntireTree(ServerLevel level, Player player, BlockPos hitPos, TreeScanResult tree, ItemStack tool) {
        level.playSound(null, hitPos, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 0.85F, 0.75F);

        tree.logs.sort((a, b) -> Integer.compare(b.getY(), a.getY()));

        for (BlockPos logPos : tree.logs) {
            BlockState bs = level.getBlockState(logPos);
            if (!isLog(bs)) continue;

            Block dropBlock = bs.is(ModItemsAndBlocks.CHOPPED_LOG.get())
                    ? ChoppedLogBlock.getLogBlockFromIndex(bs.getValue(ChoppedLogBlock.WOOD_TYPE))
                    : bs.getBlock();

            level.levelEvent(2001, logPos, Block.getId(bs));
            level.setBlock(logPos, Blocks.AIR.defaultBlockState(), 3);

            if (!player.isCreative()) {
                Block.popResource(level, hitPos, new ItemStack(dropBlock));
            }
        }

        int leafParticles = 0;
        for (BlockPos leafPos : tree.leaves) {
            BlockState ls = level.getBlockState(leafPos);
            if (ls.is(BlockTags.LEAVES)) {
                if (leafParticles < 35) {
                    level.levelEvent(2001, leafPos, Block.getId(ls));
                    leafParticles++;
                }
                if (!player.isCreative()) {
                    Block.dropResources(ls, level, leafPos, null, player, tool);
                }
                level.setBlock(leafPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private record TreeScanResult(List<BlockPos> logs, List<BlockPos> leaves, BlockPos basePos, boolean hasLeaves) {}

    private static TreeScanResult scanTree(Level level, BlockPos startPos) {
        List<BlockPos> logs = new ArrayList<>();
        List<BlockPos> leaves = new ArrayList<>();
        Set<BlockPos> visitedLogs = new HashSet<>();
        Set<BlockPos> visitedLeaves = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        queue.add(startPos);
        visitedLogs.add(startPos);
        BlockPos lowestPos = startPos;
        boolean hasLeaves = false;

        int minY = startPos.getY() - 2;

        while (!queue.isEmpty() && logs.size() < 256) {
            BlockPos current = queue.poll();
            logs.add(current);
            if (current.getY() < lowestPos.getY()) {
                lowestPos = current;
            }

            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos next = current.offset(dx, dy, dz);
                        if (next.getY() < minY) continue;
                        if (Math.abs(next.getX() - startPos.getX()) > 10 || Math.abs(next.getZ() - startPos.getZ()) > 10) {
                            continue;
                        }

                        BlockState ns = level.getBlockState(next);
                        if (isLog(ns)) {
                            if (visitedLogs.add(next)) {
                                queue.add(next);
                            }
                        } else if (ns.is(BlockTags.LEAVES)) {
                            hasLeaves = true;
                            if (visitedLeaves.add(next)) {
                                leaves.add(next);
                            }
                        }
                    }
                }
            }
        }

        List<BlockPos> firstLayerLeaves = new ArrayList<>(leaves);
        for (BlockPos lf : firstLayerLeaves) {
            if (leaves.size() >= 450) break;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos nl = lf.offset(dx, dy, dz);
                        if (Math.abs(nl.getX() - startPos.getX()) <= 9 && Math.abs(nl.getZ() - startPos.getZ()) <= 9) {
                            BlockState ls = level.getBlockState(nl);
                            if (ls.is(BlockTags.LEAVES) && visitedLeaves.add(nl)) {
                                leaves.add(nl);
                            }
                        }
                    }
                }
            }
        }

        return new TreeScanResult(logs, leaves, lowestPos, hasLeaves);
    }
}