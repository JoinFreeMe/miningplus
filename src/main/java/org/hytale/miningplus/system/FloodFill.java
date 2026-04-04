package org.hytale.miningplus.system;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Breadth-first flood fill engine for finding connected blocks in a world.
 * Uses 26-directional adjacency (includes diagonals) and respects chunk boundaries.
 */
public final class FloodFill {

    private static final int[][] DIRECTIONS;

    static {
        List<int[]> dirs = new ArrayList<>(26);
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++)
                    if (dx != 0 || dy != 0 || dz != 0)
                        dirs.add(new int[]{dx, dy, dz});
        DIRECTIONS = dirs.toArray(new int[0][]);
    }

    private FloodFill() {}

    /**
     * Searches for connected blocks matching the given predicate, starting from {@code start}.
     * The starting block itself is excluded from the results (it is assumed to already be handled).
     *
     * @param world     the world to search in
     * @param start     the origin block position
     * @param matcher   predicate to test whether a neighbouring block should be included
     * @param maxBlocks maximum number of blocks to return
     * @param maxDepth  maximum BFS depth (distance from origin)
     * @return list of matching block positions, excluding the start position
     */
    public static List<Vector3i> search(World world, Vector3i start, Predicate<BlockType> matcher,
                                        int maxBlocks, int maxDepth) {
        List<Vector3i> results = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Queue<long[]> queue = new LinkedList<>();

        int sx = start.getX(), sy = start.getY(), sz = start.getZ();
        long startPacked = packPos(sx, sy, sz);
        visited.add(startPacked);
        queue.add(new long[]{startPacked, 0});

        while (!queue.isEmpty() && results.size() < maxBlocks) {
            long[] entry = queue.poll();
            long packed = entry[0];
            int depth = (int) entry[1];

            int bx = unpackX(packed);
            int by = unpackY(packed);
            int bz = unpackZ(packed);

            if (bx != sx || by != sy || bz != sz) {
                results.add(new Vector3i(bx, by, bz));
            }

            if (depth >= maxDepth) continue;

            for (int[] dir : DIRECTIONS) {
                int nx = bx + dir[0];
                int ny = by + dir[1];
                int nz = bz + dir[2];

                if (ny < 0 || ny >= 320) continue;

                long neighborPacked = packPos(nx, ny, nz);
                if (visited.contains(neighborPacked)) continue;
                visited.add(neighborPacked);

                long chunkIndex = ChunkUtil.indexChunkFromBlock(nx, nz);
                WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
                if (chunk == null) continue;

                int localX = nx & ChunkUtil.SIZE_MASK;
                int localZ = nz & ChunkUtil.SIZE_MASK;
                int blockId = chunk.getBlock(localX, ny, localZ);
                if (blockId == 0) continue;

                BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
                if (blockType == null || !matcher.test(blockType)) continue;

                queue.add(new long[]{neighborPacked, depth + 1});
            }
        }

        return results;
    }

    // Packs block coordinates into a single long for fast hashing.
    // x: bits 0-25, y: bits 26-35, z: bits 36-61
    private static long packPos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF)) | ((long) (y & 0x3FF) << 26) | ((long) (z & 0x3FFFFFF) << 36);
    }

    private static int unpackX(long packed) {
        int raw = (int) (packed & 0x3FFFFFF);
        return (raw << 6) >> 6;
    }

    private static int unpackY(long packed) {
        return (int) ((packed >> 26) & 0x3FF);
    }

    private static int unpackZ(long packed) {
        int raw = (int) ((packed >> 36) & 0x3FFFFFF);
        return (raw << 6) >> 6;
    }
}
