package com.internal.core.util.mathematics.Extras;

public enum Direction3Vector {

	NORTH(
			0,
			0,
			1),
	EAST(
			1,
			0,
			0),
	SOUTH(
			0,
			0,
			-1),
	WEST(
			-1,
			0,
			0),
	UP(
			0,
			1,
			0),
	DOWN(
			0,
			-1,
			0);

	// Internal
	public final int index;
	public final int x, y, z;

	public final long coordinate2Long;
	public final long coordinate3Long;
	public final int vertOffset3Int;

	public final boolean positive;
	public final boolean negative;

	public static final Direction3Vector[] VALUES = values();
	public static final int LENGTH = values().length;

	private static final Direction3Vector[][] TANGENTS_LOOKUP = new Direction3Vector[LENGTH][];
	private static final Direction2Vector[] TO_2D_LOOKUP = new Direction2Vector[LENGTH];

	// Direction lookup tables for fast retrieval
	private static final Direction3Vector[][][] DIRECTION_LOOKUP = new Direction3Vector[3][3][3];
	private static final Direction3Vector[] X_AXIS_LOOKUP = new Direction3Vector[3];
	private static final Direction3Vector[] Y_AXIS_LOOKUP = new Direction3Vector[3];
	private static final Direction3Vector[] Z_AXIS_LOOKUP = new Direction3Vector[3];

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

	static {

		// Build 3D lookup table for all combinations
		for (Direction3Vector dir : VALUES) {
			int xi = dir.x + 1;
			int yi = dir.y + 1;
			int zi = dir.z + 1;
			DIRECTION_LOOKUP[xi][yi][zi] = dir;
		}

		// Build axis-specific lookups
		X_AXIS_LOOKUP[0] = WEST; // -1 -> index 0
		X_AXIS_LOOKUP[1] = null; // 0 -> index 1
		X_AXIS_LOOKUP[2] = EAST; // 1 -> index 2

		Y_AXIS_LOOKUP[0] = DOWN; // -1 -> index 0
		Y_AXIS_LOOKUP[1] = null; // 0 -> index 1
		Y_AXIS_LOOKUP[2] = UP; // 1 -> index 2

		Z_AXIS_LOOKUP[0] = SOUTH; // -1 -> index 0
		Z_AXIS_LOOKUP[1] = null; // 0 -> index 1
		Z_AXIS_LOOKUP[2] = NORTH; // 1 -> index 2
	}

	// Internal \\
	Direction3Vector(
			int x,
			int y,
			int z) {

		// Internal

		this.index = this.ordinal();
		this.x = x;
		this.y = y;
		this.z = z;

		this.coordinate2Long = Coordinate2Long.pack(x, z);
		this.coordinate3Long = Coordinate3Long.pack(x, y, z);
		int vx = x > 0 ? 1 : 0;
		int vy = y > 0 ? 1 : 0;
		int vz = z > 0 ? 1 : 0;
		this.vertOffset3Int = Coordinate3Int.pack(vx, vy, vz);

		positive = (x > 0 ||
				y > 0 ||
				z > 0);
		negative = (!positive);
	}

	// Utility \\

	public Direction2Vector to2D() {
		return TO_2D_LOOKUP[this.ordinal()];
	}

	public static Direction3Vector[] getTangents(Direction3Vector normal) {
		return TANGENTS_LOOKUP[normal.ordinal()];
	}

	public static Direction3Vector getDirection(int x, int y, int z) {
		return DIRECTION_LOOKUP[x + 1][y + 1][z + 1];
	}

	public static Direction3Vector getDirectionX(int x) {
		return X_AXIS_LOOKUP[x + 1];
	}

	public static Direction3Vector getDirectionY(int y) {
		return Y_AXIS_LOOKUP[y + 1];
	}

	public static Direction3Vector getDirectionZ(int z) {
		return Z_AXIS_LOOKUP[z + 1];
	}
}