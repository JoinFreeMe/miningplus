package org.hytale.miningplus.system;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the shape of blocks to break relative to a mined block.
 * Each pattern generates positions based on the face the player mined from.
 */
public enum ExcavationPattern {

    /** Connected ore flood fill (default vein mining behaviour). */
    ORES_ONLY("Ores Only"),

    /** Connected block flood fill - matches the exact block type mined. */
    TARGETED("Targeted Block"),

    /** Single block tunnel extending away from the player. */
    TUNNEL_1x1("Tunnel 1x1"),

    /** 2-high, 1-wide tunnel extending away from the player (origin + one above). */
    TUNNEL_2x1("Tunnel 2x1"),

    /** 3x3 tunnel extending away from the player. */
    TUNNEL_3x3("Tunnel 3x3"),

    /** 3x3 area, 3 blocks deep from the mined face. */
    CUBE_3x3x3("3x3x3 Cube"),

    /** 5x5 area, 5 blocks deep from the mined face. */
    CUBE_5x5x5("5x5x5 Cube");

    private final String displayName;

    ExcavationPattern(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Generates the list of block positions this pattern would break,
     * excluding the origin block itself.
     * Depth is automatically capped so the total block count stays within maxBlocks.
     *
     * @param origin    the block the player mined
     * @param playerPos the player's position (used to determine facing direction)
     * @param depth     how many blocks deep tunnels extend
     * @param maxBlocks the maximum number of blocks that can be broken
     * @return positions to break (does NOT include origin)
     */
    public List<Vector3i> getPositions(Vector3i origin, Vector3d playerPos, int depth, int maxBlocks) {
        return switch (this) {
            case ORES_ONLY -> List.of(); // handled separately by FloodFill
            case TARGETED -> List.of(); // handled separately by FloodFill
            case TUNNEL_1x1 -> centeredTunnel(origin, playerPos, 1, 1, capDepth(depth, 1, maxBlocks));
            case TUNNEL_2x1 -> bottomUpTunnel(origin, playerPos, 1, 2, capDepth(depth, 2, maxBlocks));
            case TUNNEL_3x3 -> centeredTunnel(origin, playerPos, 3, 3, capDepth(depth, 9, maxBlocks));
            case CUBE_3x3x3 -> centeredTunnel(origin, playerPos, 3, 3, capDepth(3, 9, maxBlocks));
            case CUBE_5x5x5 -> centeredTunnel(origin, playerPos, 5, 5, capDepth(5, 25, maxBlocks));
        };
    }

    /**
     * Caps the depth so that (depth * crossSection) does not exceed maxBlocks.
     * Ensures at least 1 block deep if any blocks are available.
     */
    private static int capDepth(int depth, int crossSection, int maxBlocks) {
        int maxDepth = Math.max(1, maxBlocks / crossSection);
        return Math.min(depth, maxDepth);
    }

    /**
     * Determines the mining direction based on which face of the block
     * the player is closest to. Returns a unit direction vector pointing
     * away from the player (into the wall).
     */
    static int[] getFaceDirection(Vector3i block, Vector3d playerPos) {
        double dx = playerPos.getX() - (block.getX() + 0.5);
        double dy = playerPos.getY() - (block.getY() + 0.5);
        double dz = playerPos.getZ() - (block.getZ() + 0.5);

        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);

        if (ax >= ay && ax >= az) {
            return new int[]{dx > 0 ? -1 : 1, 0, 0};
        } else if (ay >= ax && ay >= az) {
            return new int[]{0, dy > 0 ? -1 : 1, 0};
        } else {
            return new int[]{0, 0, dz > 0 ? -1 : 1};
        }
    }

    /**
     * Resolves the two perpendicular axes for a given mining direction.
     * Returns {right, up} where each is a unit direction vector.
     */
    private static int[][] getPerpendicularAxes(int[] dir) {
        if (dir[1] != 0) {
            return new int[][]{{1, 0, 0}, {0, 0, 1}};
        } else if (dir[0] != 0) {
            return new int[][]{{0, 0, 1}, {0, 1, 0}};
        } else {
            return new int[][]{{1, 0, 0}, {0, 1, 0}};
        }
    }

    /**
     * Generates a tunnel where the origin block is the center of the cross-section.
     * Used for 1x1, 3x3, and cube patterns.
     */
    private static List<Vector3i> centeredTunnel(Vector3i origin, Vector3d playerPos,
                                                  int width, int height, int depth) {
        int[] dir = getFaceDirection(origin, playerPos);
        int[][] axes = getPerpendicularAxes(dir);
        int[] right = axes[0], up = axes[1];
        List<Vector3i> positions = new ArrayList<>();

        int halfW = (width - 1) / 2;
        int halfH = (height - 1) / 2;
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();

        for (int d = 0; d < depth; d++) {
            for (int w = -halfW; w <= halfW; w++) {
                for (int h = -halfH; h <= halfH; h++) {
                    int x = ox + dir[0] * d + right[0] * w + up[0] * h;
                    int y = oy + dir[1] * d + right[1] * w + up[1] * h;
                    int z = oz + dir[2] * d + right[2] * w + up[2] * h;

                    if (x == ox && y == oy && z == oz) continue;
                    if (y < 0 || y >= 320) continue;

                    positions.add(new Vector3i(x, y, z));
                }
            }
        }

        return positions;
    }

    /**
     * Generates a tunnel where the origin block is at the bottom of the cross-section.
     * Used for the 2x1 tunnel (player-height, origin is the foot block).
     */
    private static List<Vector3i> bottomUpTunnel(Vector3i origin, Vector3d playerPos,
                                                  int width, int height, int depth) {
        int[] dir = getFaceDirection(origin, playerPos);
        int[][] axes = getPerpendicularAxes(dir);
        int[] right = axes[0], up = axes[1];
        List<Vector3i> positions = new ArrayList<>();

        int halfW = (width - 1) / 2;
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();

        for (int d = 0; d < depth; d++) {
            for (int w = -halfW; w <= halfW; w++) {
                for (int h = 0; h < height; h++) {
                    int x = ox + dir[0] * d + right[0] * w + up[0] * h;
                    int y = oy + dir[1] * d + right[1] * w + up[1] * h;
                    int z = oz + dir[2] * d + right[2] * w + up[2] * h;

                    if (x == ox && y == oy && z == oz) continue;
                    if (y < 0 || y >= 320) continue;

                    positions.add(new Vector3i(x, y, z));
                }
            }
        }

        return positions;
    }
}
