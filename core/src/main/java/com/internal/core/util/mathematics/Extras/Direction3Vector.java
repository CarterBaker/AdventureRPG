package com.internal.core.util.mathematics.Extras;

import com.internal.core.engine.EngineUtility.InternalException;

public enum Direction3Vector {

	NORTH(0, 0, 1),
	EAST(1, 0, 0),
	SOUTH(0, 0, -1),
	WEST(-1, 0, 0),
	UP(0, 1, 0),
	DOWN(0, -1, 0);

	// Internal
	public final int index;
	public final int x, y, z;

	public final long coordinate2Long;
	public final long coordinate3Long;
	public final short coordinate3Short;

	public static final Direction3Vector[] VALUES = values();
	public static final int LENGTH = values().length;

	// Internal \\

	Direction3Vector(int x, int y, int z) {

		// Internal
		this.index = this.ordinal();
		this.x = x;
		this.y = y;
		this.z = z;

		this.coordinate2Long = Coordinate2Long.pack(x, z);
		this.coordinate3Long = Coordinate3Long.pack(x, y, z);
		this.coordinate3Short = Coordinate3Short.pack(x, y, z);
	}

	// Utility \\

	public Direction2Vector to2D() {
		switch (this) {
			case NORTH:
				return Direction2Vector.NORTH;
			case EAST:
				return Direction2Vector.EAST;
			case SOUTH:
				return Direction2Vector.SOUTH;
			case WEST:
				return Direction2Vector.WEST;
			default:
				return null;
		}
	}

	public static Direction3Vector[] getTangents(Direction3Vector normal) {

		switch (normal) {

			case UP:
			case DOWN:
				return new Direction3Vector[] {
						EAST,
						NORTH };

			case NORTH:
			case SOUTH:
				return new Direction3Vector[] {
						EAST,
						UP };

			case EAST:
			case WEST:
				return new Direction3Vector[] {
						NORTH,
						UP };

			default:
				throw new InternalException("Unexpected normal: " + normal);
		}
	}
}
