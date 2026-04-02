package program.core.util.mathematics.extras;

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
	public final int vertOffset3Int;
	public final boolean positive;
	public final boolean negative;

	public static final Direction3Vector[] VALUES = values();
	public static final int LENGTH = values().length;

	// Lookup tables
	private static final Direction3Vector[][] TANGENTS_LOOKUP = new Direction3Vector[LENGTH][];
	private static final Direction2Vector[] TO_2D_LOOKUP = new Direction2Vector[LENGTH];
	private static final Direction3Vector[][][] DIRECTION_LOOKUP = new Direction3Vector[3][3][3];
	private static final Direction3Vector[] X_AXIS_LOOKUP = new Direction3Vector[3];
	private static final Direction3Vector[] Y_AXIS_LOOKUP = new Direction3Vector[3];
	private static final Direction3Vector[] Z_AXIS_LOOKUP = new Direction3Vector[3];

	// [orientation 0-23][worldFace 0-5] → local face index to sample texture from
	private static final int[][] FACE_REMAP = new int[24][LENGTH];

	// [orientation 0-23][worldFace 0-5] → encodedFace (0-23)
	private static final int[][] ENCODED_FACE_LOOKUP = new int[24][LENGTH];

	// Tangents \\

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

	// 2D \\

	static {
		TO_2D_LOOKUP[NORTH.ordinal()] = Direction2Vector.NORTH;
		TO_2D_LOOKUP[EAST.ordinal()] = Direction2Vector.EAST;
		TO_2D_LOOKUP[SOUTH.ordinal()] = Direction2Vector.SOUTH;
		TO_2D_LOOKUP[WEST.ordinal()] = Direction2Vector.WEST;
		TO_2D_LOOKUP[UP.ordinal()] = null;
		TO_2D_LOOKUP[DOWN.ordinal()] = null;
	}

	// Direction lookup \\

	static {
		for (Direction3Vector dir : VALUES) {
			int xi = dir.x + 1;
			int yi = dir.y + 1;
			int zi = dir.z + 1;
			DIRECTION_LOOKUP[xi][yi][zi] = dir;
		}

		X_AXIS_LOOKUP[0] = WEST;
		X_AXIS_LOOKUP[1] = null;
		X_AXIS_LOOKUP[2] = EAST;

		Y_AXIS_LOOKUP[0] = DOWN;
		Y_AXIS_LOOKUP[1] = null;
		Y_AXIS_LOOKUP[2] = UP;

		Z_AXIS_LOOKUP[0] = SOUTH;
		Z_AXIS_LOOKUP[1] = null;
		Z_AXIS_LOOKUP[2] = NORTH;
	}

	// FACE_REMAP \\

	static {
		// orientation = facing * 4 + spin

		// facing UP — spin rotates which side texture appears on each side face
		// spin 0 — identity
		for (int f = 0; f < LENGTH; f++)
			FACE_REMAP[UP.ordinal() * 4 + 0][f] = f;

		// spin 1 — 90° CW from above
		FACE_REMAP[UP.ordinal() * 4 + 1][NORTH.ordinal()] = WEST.ordinal();
		FACE_REMAP[UP.ordinal() * 4 + 1][EAST.ordinal()] = NORTH.ordinal();
		FACE_REMAP[UP.ordinal() * 4 + 1][SOUTH.ordinal()] = EAST.ordinal();
		FACE_REMAP[UP.ordinal() * 4 + 1][WEST.ordinal()] = SOUTH.ordinal();
		FACE_REMAP[UP.ordinal() * 4 + 1][UP.ordinal()] = UP.ordinal();
		FACE_REMAP[UP.ordinal() * 4 + 1][DOWN.ordinal()] = DOWN.ordinal();

		// spin 2 — 180°
		FACE_REMAP[UP.ordinal() * 4 + 2][NORTH.ordinal()] = SOUTH.ordinal();
		FACE_REMAP[UP.ordinal() * 4 + 2][EAST.ordinal()] = WEST.ordinal();
		FACE_REMAP[UP.ordinal() * 4 + 2][SOUTH.ordinal()] = NORTH.ordinal();
		FACE_REMAP[UP.ordinal() * 4 + 2][WEST.ordinal()] = EAST.ordinal();
		FACE_REMAP[UP.ordinal() * 4 + 2][UP.ordinal()] = UP.ordinal();
		FACE_REMAP[UP.ordinal() * 4 + 2][DOWN.ordinal()] = DOWN.ordinal();

		// spin 3 — 270° CW
		FACE_REMAP[UP.ordinal() * 4 + 3][NORTH.ordinal()] = EAST.ordinal();
		FACE_REMAP[UP.ordinal() * 4 + 3][EAST.ordinal()] = SOUTH.ordinal();
		FACE_REMAP[UP.ordinal() * 4 + 3][SOUTH.ordinal()] = WEST.ordinal();
		FACE_REMAP[UP.ordinal() * 4 + 3][WEST.ordinal()] = NORTH.ordinal();
		FACE_REMAP[UP.ordinal() * 4 + 3][UP.ordinal()] = UP.ordinal();
		FACE_REMAP[UP.ordinal() * 4 + 3][DOWN.ordinal()] = DOWN.ordinal();

		// facing NORTH — top→north, east/west uninvolved
		for (int s = 0; s < 4; s++) {
			int o = NORTH.ordinal() * 4 + s;
			FACE_REMAP[o][NORTH.ordinal()] = UP.ordinal();
			FACE_REMAP[o][SOUTH.ordinal()] = DOWN.ordinal();
			FACE_REMAP[o][UP.ordinal()] = SOUTH.ordinal();
			FACE_REMAP[o][DOWN.ordinal()] = NORTH.ordinal();
			FACE_REMAP[o][EAST.ordinal()] = EAST.ordinal();
			FACE_REMAP[o][WEST.ordinal()] = WEST.ordinal();
		}

		// facing SOUTH — top→south, east/west uninvolved
		for (int s = 0; s < 4; s++) {
			int o = SOUTH.ordinal() * 4 + s;
			FACE_REMAP[o][SOUTH.ordinal()] = UP.ordinal();
			FACE_REMAP[o][NORTH.ordinal()] = DOWN.ordinal();
			FACE_REMAP[o][UP.ordinal()] = NORTH.ordinal();
			FACE_REMAP[o][DOWN.ordinal()] = SOUTH.ordinal();
			FACE_REMAP[o][EAST.ordinal()] = EAST.ordinal();
			FACE_REMAP[o][WEST.ordinal()] = WEST.ordinal();
		}

		// facing EAST — top→east, north/south uninvolved
		for (int s = 0; s < 4; s++) {
			int o = EAST.ordinal() * 4 + s;
			FACE_REMAP[o][EAST.ordinal()] = UP.ordinal();
			FACE_REMAP[o][WEST.ordinal()] = DOWN.ordinal();
			FACE_REMAP[o][UP.ordinal()] = WEST.ordinal();
			FACE_REMAP[o][DOWN.ordinal()] = EAST.ordinal();
			FACE_REMAP[o][NORTH.ordinal()] = NORTH.ordinal();
			FACE_REMAP[o][SOUTH.ordinal()] = SOUTH.ordinal();
		}

		// facing WEST — top→west, north/south uninvolved
		for (int s = 0; s < 4; s++) {
			int o = WEST.ordinal() * 4 + s;
			FACE_REMAP[o][WEST.ordinal()] = UP.ordinal();
			FACE_REMAP[o][EAST.ordinal()] = DOWN.ordinal();
			FACE_REMAP[o][UP.ordinal()] = EAST.ordinal();
			FACE_REMAP[o][DOWN.ordinal()] = WEST.ordinal();
			FACE_REMAP[o][NORTH.ordinal()] = NORTH.ordinal();
			FACE_REMAP[o][SOUTH.ordinal()] = SOUTH.ordinal();
		}

		// facing DOWN — 180° flip
		for (int s = 0; s < 4; s++) {
			int o = DOWN.ordinal() * 4 + s;
			FACE_REMAP[o][DOWN.ordinal()] = UP.ordinal();
			FACE_REMAP[o][UP.ordinal()] = DOWN.ordinal();
			FACE_REMAP[o][NORTH.ordinal()] = SOUTH.ordinal();
			FACE_REMAP[o][SOUTH.ordinal()] = NORTH.ordinal();
			FACE_REMAP[o][EAST.ordinal()] = EAST.ordinal();
			FACE_REMAP[o][WEST.ordinal()] = WEST.ordinal();
		}
	}

	static {
		for (int facingIdx = 0; facingIdx < LENGTH; facingIdx++) {
			Direction3Vector facing = VALUES[facingIdx];

			for (int spin = 0; spin < 4; spin++) {
				int orientation = facingIdx * 4 + spin;

				for (int worldFaceIdx = 0; worldFaceIdx < LENGTH; worldFaceIdx++) {
					Direction3Vector worldFace = VALUES[worldFaceIdx];

					// Get texture face from remap
					Direction3Vector textureFace = VALUES[FACE_REMAP[orientation][worldFaceIdx]];

					// Compute texture spin
					int textureSpin = computeTextureSpin(worldFace, facing, spin);

					ENCODED_FACE_LOOKUP[orientation][worldFaceIdx] = textureFace.ordinal() * 4 + textureSpin;
				}
			}
		}
	}

	private static int computeTextureSpin(Direction3Vector worldFace, Direction3Vector facing, int blockSpin) {

		// Top or bottom face — use block spin directly
		if (worldFace == facing)
			return blockSpin;

		Direction3Vector opposite = getDirection(-facing.x, -facing.y, -facing.z);
		if (worldFace == opposite)
			return blockSpin;

		// Side face — find where facing points relative to this face's tangent axes
		Direction3Vector[] tangents = TANGENTS_LOOKUP[worldFace.ordinal()];
		Direction3Vector faceRight = tangents[0];
		Direction3Vector faceUp = tangents[1];

		if (facing == faceUp)
			return 0;
		if (facing == faceRight)
			return 1;

		Direction3Vector negUp = getDirection(-faceUp.x, -faceUp.y, -faceUp.z);
		Direction3Vector negRight = getDirection(-faceRight.x, -faceRight.y, -faceRight.z);

		if (facing == negUp)
			return 2;
		if (facing == negRight)
			return 3;

		return 0;
	}

	// Internal \\

	Direction3Vector(int x, int y, int z) {
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
		this.positive = (x > 0 || y > 0 || z > 0);
		this.negative = (!positive);
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

	public static Direction3Vector remapFace(int orientation, Direction3Vector worldFace) {
		return VALUES[FACE_REMAP[orientation][worldFace.ordinal()]];
	}

	public static int getEncodedFace(int orientation, Direction3Vector worldFace) {
		return ENCODED_FACE_LOOKUP[orientation][worldFace.ordinal()];
	}
}