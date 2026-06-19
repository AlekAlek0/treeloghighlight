package net.alek.treeloghighlight.client;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.*;

public class TreeLogHighlightManager {
    private static final Set<BlockPos> highlightedLogs = Collections.synchronizedSet(new HashSet<>());
    private static Block targetBlockType = null;
    private static BlockPos lastActionPos = null;
    private static long lastUpdateTime = 0;
    
    private static final int MAX_TREE_SIZE = 2000;
    private static final double MAX_PLAYER_DIST_SQ = 2500.0;
    private static final long IDLE_TIMEOUT_MS = 20000;

    public static boolean isHighlighted(BlockPos pos) {
        return highlightedLogs.contains(pos);
    }

    public static void highlightTree(Level world, BlockPos startPos) {
        BlockState startState = world.getBlockState(startPos);
        if (!startState.is(BlockTags.LOGS)) return;

        Block newType = startState.getBlock();
        String blockId = BuiltInRegistries.BLOCK.getKey(newType).toString();
        TreeLogHighlightConfig config = TreeLogHighlightClient.getConfig();

        if (!config.woodTypeToggles.containsKey(blockId)) {
            config.setWoodEnabled(blockId, true);
            config.save();
        }
        
        if (!config.isWoodEnabled(blockId)) {
            highlightedLogs.clear();
            return;
        }

        if (targetBlockType == newType && highlightedLogs.contains(startPos)) {
            lastUpdateTime = System.currentTimeMillis();
            lastActionPos = startPos;
            return;
        }

        Set<BlockPos> authorizedRoots = findRoots(world, startPos, newType);
        
        Set<BlockPos> foundLogs = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        boolean foundNaturalLeaves = false;
        
        queue.add(startPos);
        foundLogs.add(startPos);

        while (!queue.isEmpty() && foundLogs.size() < MAX_TREE_SIZE) {
            BlockPos current = queue.poll();
            
            boolean isNearLeaves = false;
            for (BlockPos leafCheck : BlockPos.betweenClosed(current.offset(-1, -1, -1), current.offset(1, 1, 1))) {
                BlockState state = world.getBlockState(leafCheck);
                if (state.is(BlockTags.LEAVES) || state.is(BlockTags.WART_BLOCKS) || state.is(net.minecraft.world.level.block.Blocks.SHROOMLIGHT)) {
                    isNearLeaves = true;
                    if (state.is(BlockTags.WART_BLOCKS)) {
                        foundNaturalLeaves = true;
                    } else if (state.hasProperty(LeavesBlock.PERSISTENT)) {
                        if (!state.getValue(LeavesBlock.PERSISTENT)) {
                            foundNaturalLeaves = true;
                        }
                    } else {
                        foundNaturalLeaves = true;
                    }
                }
            }

            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        int dist = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
                        if (dist > 1 && (y < 0 || !isNearLeaves)) continue;

                        BlockPos neighbor = current.offset(x, y, z);
                        if (!foundLogs.contains(neighbor)) {
                            BlockState state = world.getBlockState(neighbor);
                            if (state.is(newType)) {
                                if (isOnGround(world, neighbor) && !authorizedRoots.contains(neighbor)) continue; 
                                foundLogs.add(neighbor);
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }
        }
        
        if (!foundNaturalLeaves && !highlightedLogs.contains(startPos)) return;

        targetBlockType = newType;
        lastActionPos = startPos;
        lastUpdateTime = System.currentTimeMillis();
        highlightedLogs.clear();
        highlightedLogs.addAll(foundLogs);
    }

    public static void clear() {
        highlightedLogs.clear();
        targetBlockType = null;
    }

    private static Set<BlockPos> findRoots(Level world, BlockPos start, Block type) {
        Set<BlockPos> roots = new HashSet<>();
        Set<BlockPos> checked = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(start);
        checked.add(start);
        while(!queue.isEmpty() && checked.size() < 200) {
            BlockPos curr = queue.poll();
            if (isOnGround(world, curr)) roots.add(curr);
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos next = curr.offset(x, y, z);
                        if (!checked.contains(next) && world.getBlockState(next).is(type)) {
                            checked.add(next);
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return roots;
    }

    private static boolean isOnGround(Level world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.below());
        return !below.isAir() && !below.is(BlockTags.LOGS) && !below.is(BlockTags.LEAVES) && !below.is(BlockTags.WART_BLOCKS);
    }

    public static Set<BlockPos> getHighlightedLogs() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || highlightedLogs.isEmpty()) return Collections.emptySet();
        
        long timeSinceUpdate = System.currentTimeMillis() - lastUpdateTime;
        double distSq = (lastActionPos != null) ? client.player.distanceToSqr(lastActionPos.getX(), lastActionPos.getY(), lastActionPos.getZ()) : 0;
        
        if (timeSinceUpdate > IDLE_TIMEOUT_MS || (lastActionPos != null && distSq > MAX_PLAYER_DIST_SQ)) {
            highlightedLogs.clear();
            targetBlockType = null;
            lastActionPos = null;
            return Collections.emptySet();
        }

        highlightedLogs.removeIf(pos -> client.level != null && !client.level.getBlockState(pos).is(targetBlockType));
        return highlightedLogs;
    }
}