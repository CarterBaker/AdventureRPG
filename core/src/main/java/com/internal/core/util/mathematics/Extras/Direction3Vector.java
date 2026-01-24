package com.internal.core.util.mathematics.Extras;

import com.internal.bootstrap.worldpipeline.util.Coordinate3Short;
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
	public final short vertOffset3Short;

	public static final Direction3Vector[] VALUES = values();
	public static final int LENGTH = values().length;

	private static final Direction3Vector[][] TANGENTS_LOOKUP = new Direction3Vector[LENGTH][];
	private static final Direction2Vector[] TO_2D_LOOKUP = new Direction2Vector[LENGTH];

	static {
		Direction3Vector[] horizontalTangents = new Direction3Vector[] { EAST, NORTH };
		Direction3Vector[] northSouthTangents = new Direction3Vector[] { EAST, UP };
		Direction3Vector[] eastWestTangents = new Direction3Vector[] { NORTH, UP };

		TANGENTS_LOOKUP[NORTH.ordinal()] = northSouthTangents;
		TANGENTS_LOOKUP[EAST.ordinal()] = eastWestTangents;
		TANGENTS_LOOKUP[SOUTH.ordinal()] = northSouthTangents;
		TANGENTS_LOOKUP[WEST.ordinal()] = eastWestTangents;
		TANGENTS_LOOKUP[UP.ordinal()] = horizontalTangents;
		TANGENTS_LOOKUP[DOWN.ordinal()] = horizontalTangents;
	}

	static {
		TO_2D_LOOKUP[NORTH.ordinal()] = Direction2Vector.NORTH;
		TO_2D_LOOKUP[EAST.ordinal()] = Direction2Vector.EAST;
		TO_2D_LOOKUP[SOUTH.ordinal()] = Direction2Vector.SOUTH;
		TO_2D_LOOKUP[WEST.ordinal()] = Direction2Vector.WEST;
		TO_2D_LOOKUP[UP.ordinal()] = null;
		TO_2D_LOOKUP[DOWN.ordinal()] = null;
	}

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

		int vx = x > 0 ? 1 : 0;
		int vy = y > 0 ? 1 : 0;
		int vz = z > 0 ? 1 : 0;
		this.vertOffset3Short = Coordinate3Short.pack(vx, vy, vz);
	}

	// Utility \\

	public Direction2Vector to2D() {
		return TO_2D_LOOKUP[this.ordinal()];
	}

	public static Direction3Vector[] getTangents(Direction3Vector normal) {
		return TANGENTS_LOOKUP[normal.ordinal()];
	}
}
