package engine.util.mathematics.extras;

public enum Direction2Vector {

    /*
     * The eight horizontal directions with their offsets, packed coordinates
     * and lookups to 3D directions.
     */

    NORTH(0, 1),
    NORTHEAST(1, 1),
    EAST(1, 0),
    SOUTHEAST(1, -1),
    SOUTH(0, -1),
    SOUTHWEST(-1, -1),
    WEST(-1, 0),
    NORTHWEST(-1, 1);

    // Internal
    public final int index;
    public final int x, y;

    public final long coordinate2Long;
    public final long coordinate3Long;

    public static final Direction2Vector[] VALUES = values();
    public static final int LENGTH = values().length;

    private static final Direction3Vector[] TO_3D_LOOKUP = new Direction3Vector[LENGTH];
    private static final Direction2Vector[][] DIRECTION_LOOKUP = new Direction2Vector[3][3];

    static {
        TO_3D_LOOKUP[NORTH.ordinal()] = Direction3Vector.NORTH;
        TO_3D_LOOKUP[NORTHEAST.ordinal()] = null;
        TO_3D_LOOKUP[EAST.ordinal()] = Direction3Vector.EAST;
        TO_3D_LOOKUP[SOUTHEAST.ordinal()] = null;
        TO_3D_LOOKUP[SOUTH.ordinal()] = Direction3Vector.SOUTH;
        TO_3D_LOOKUP[SOUTHWEST.ordinal()] = null;
        TO_3D_LOOKUP[WEST.ordinal()] = Direction3Vector.WEST;
        TO_3D_LOOKUP[NORTHWEST.ordinal()] = null;
    }

    static {
        for (Direction2Vector direction : VALUES)
            DIRECTION_LOOKUP[direction.x + 1][direction.y + 1] = direction;
    }

    // Internal \\

    Direction2Vector(int x, int y) {

        // Internal
        this.index = this.ordinal();
        this.x = x;
        this.y = y;

        this.coordinate2Long = Coordinate2Long.pack(x, y);
        this.coordinate3Long = Coordinate3Long.pack(x, 0, y);
    }

    // Utility \\

    public Direction3Vector to3D() {
        return TO_3D_LOOKUP[this.ordinal()];
    }

    public static Direction2Vector getDirection(int x, int y) {
        return DIRECTION_LOOKUP[x + 1][y + 1];
    }
}
