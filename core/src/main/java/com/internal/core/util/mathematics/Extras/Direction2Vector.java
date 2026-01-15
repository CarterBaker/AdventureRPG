package com.internal.core.util.mathematics.Extras;

public enum Direction2Vector {

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
        public final short coordinate3Short;

        public static final Direction2Vector[] VALUES = values();
        public static final int LENGTH = values().length;

        // Internal \\

        Direction2Vector(int x, int y) {

                // Internal
                this.index = this.ordinal();
                this.x = x;
                this.y = y;

                this.coordinate2Long = Coordinate2Long.pack(x, y);
                this.coordinate3Long = Coordinate3Long.pack(x, 0, y);
                this.coordinate3Short = Coordinate3Short.pack(x, 0, y);
        }

        // Utility \\

        public Direction3Vector to3D() {
                switch (this) {
                        case NORTH:
                                return Direction3Vector.NORTH;
                        case EAST:
                                return Direction3Vector.EAST;
                        case SOUTH:
                                return Direction3Vector.SOUTH;
                        case WEST:
                                return Direction3Vector.WEST;
                        default:
                                return null;
                }
        }
}
