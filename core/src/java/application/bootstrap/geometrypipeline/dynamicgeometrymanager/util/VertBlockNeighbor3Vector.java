package application.bootstrap.geometrypipeline.dynamicgeometrymanager.util;

import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.extras.Coordinate3Long;
import engine.util.mathematics.extras.Direction2Vector;

public enum VertBlockNeighbor3Vector {

    /*
     * Encodes the eight block-space neighbors of a vertex corner, each offset
     * by (-1, 0) on each axis relative to the vertex. Used during ambient
     * occlusion color sampling to gather biome colors from surrounding blocks.
     * Public final fields are intentional — accessed directly in hot geometry
     * loops to avoid getter overhead.
     */

    UPPER_NORTH_EAST(0, 0, 0),
    UPPER_NORTH_WEST(-1, 0, 0),
    UPPER_SOUTH_EAST(0, 0, -1),
    UPPER_SOUTH_WEST(-1, 0, -1),
    LOWER_NORTH_EAST(0, -1, 0),
    LOWER_NORTH_WEST(-1, -1, 0),
    LOWER_SOUTH_EAST(0, -1, -1),
    LOWER_SOUTH_WEST(-1, -1, -1);

    // Internal
    public final int index;
    public final int x, y, z;
    public final long coordinate2Long;
    public final long coordinate3Long;
    public final int vertOffset3Int;

    public static final VertBlockNeighbor3Vector[] VALUES = values();
    public static final int LENGTH = values().length;

    private static final Direction2Vector[] TO_2D_LOOKUP = new Direction2Vector[LENGTH];

    static {
        TO_2D_LOOKUP[UPPER_NORTH_EAST.ordinal()] = Direction2Vector.NORTHEAST;
        TO_2D_LOOKUP[UPPER_NORTH_WEST.ordinal()] = Direction2Vector.NORTHWEST;
        TO_2D_LOOKUP[UPPER_SOUTH_EAST.ordinal()] = Direction2Vector.SOUTHEAST;
        TO_2D_LOOKUP[UPPER_SOUTH_WEST.ordinal()] = Direction2Vector.SOUTHWEST;
        TO_2D_LOOKUP[LOWER_NORTH_EAST.ordinal()] = Direction2Vector.NORTHEAST;
        TO_2D_LOOKUP[LOWER_NORTH_WEST.ordinal()] = Direction2Vector.NORTHWEST;
        TO_2D_LOOKUP[LOWER_SOUTH_EAST.ordinal()] = Direction2Vector.SOUTHEAST;
        TO_2D_LOOKUP[LOWER_SOUTH_WEST.ordinal()] = Direction2Vector.SOUTHWEST;
    }

    // Internal \\

    VertBlockNeighbor3Vector(int x, int y, int z) {

        // Internal
        this.index = this.ordinal();
        this.x = x;
        this.y = y;
        this.z = z;
        this.coordinate2Long = Coordinate2Long.pack(x, z);
        this.coordinate3Long = Coordinate3Long.pack(x, y, z);

        int vx = x < 0 ? 0 : 1;
        int vy = y < 0 ? 0 : 1;
        int vz = z < 0 ? 0 : 1;
        this.vertOffset3Int = Coordinate3Int.pack(vx, vy, vz);
    }

    public Direction2Vector to2D() {
        return TO_2D_LOOKUP[this.ordinal()];
    }
}