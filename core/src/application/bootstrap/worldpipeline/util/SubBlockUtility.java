package application.bootstrap.worldpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.Direction3Vector;

public class SubBlockUtility extends EngineUtility {

    /*
     * Octant math for sub-blocks, the eighth-of-a-block cubes a subdivided
     * cell is built from. A cell's sub-blocks are an eight-bit mask, one bit
     * per octant, indexed Y-Z-X exactly like the block palette itself —
     * bit (oy << 2) | (oz << 1) | ox — so the lower layer is the low nibble
     * and the upper layer the high one. Every system that reads or writes a
     * mask (storage, generation, meshing, collision, raycasting, breaking)
     * resolves octants through here, so there is exactly one definition of
     * which bit is which.
     */

    // Settings
    public static final int DIVISIONS = EngineSetting.SUB_BLOCK_DIVISIONS;
    public static final int OCTANT_COUNT = EngineSetting.SUB_BLOCK_OCTANT_COUNT;
    public static final int MASK_EMPTY = EngineSetting.SUB_BLOCK_MASK_EMPTY;
    public static final int MASK_FULL = EngineSetting.SUB_BLOCK_MASK_FULL;
    public static final float SIZE = EngineSetting.SUB_BLOCK_SIZE;

    // Octant \\

    public static int getOctant(int octantX, int octantY, int octantZ) {
        return octantX | (octantZ << 1) | (octantY << 2);
    }

    public static int getOctantX(int octant) {
        return octant & EngineSetting.SUB_BLOCK_AXIS_BIT_X;
    }

    public static int getOctantY(int octant) {
        return (octant & EngineSetting.SUB_BLOCK_AXIS_BIT_Y) >> 2;
    }

    public static int getOctantZ(int octant) {
        return (octant & EngineSetting.SUB_BLOCK_AXIS_BIT_Z) >> 1;
    }

    public static int getOctantBit(int octant) {
        return 1 << octant;
    }

    // Stepping \\

    private static int getAxisBit(Direction3Vector direction) {

        if (direction.x != 0)
            return EngineSetting.SUB_BLOCK_AXIS_BIT_X;

        if (direction.y != 0)
            return EngineSetting.SUB_BLOCK_AXIS_BIT_Y;

        return EngineSetting.SUB_BLOCK_AXIS_BIT_Z;
    }

    // Whether stepping this octant one sub-block along a direction leaves its cell
    public static boolean leavesCell(int octant, Direction3Vector direction) {
        boolean onHighSide = (octant & getAxisBit(direction)) != 0;
        return direction.positive == onHighSide;
    }

    // The neighboring octant one sub-block along a direction, wrapped into the next cell when it leaves this one
    public static int stepOctant(int octant, Direction3Vector direction) {
        return octant ^ getAxisBit(direction);
    }

    // Mask \\

    public static boolean hasOctant(int mask, int octant) {
        return (mask & getOctantBit(octant)) != 0;
    }

    public static boolean isSubdivided(int mask) {
        return mask != MASK_EMPTY && mask != MASK_FULL;
    }
}
