package application.bootstrap.worldpipeline.util;

import engine.util.mathematics.extras.Coordinate2Long;

public enum TickQuadrant {

    /*
     * Quarters the active chunk set by the sign of each chunk's absolute X/Z
     * coordinate, so LiquidTickBranch can visit one quadrant per firing
     * instead of every liquid-containing chunk at once.
     */

    POSITIVE_X_POSITIVE_Z,
    NEGATIVE_X_POSITIVE_Z,
    POSITIVE_X_NEGATIVE_Z,
    NEGATIVE_X_NEGATIVE_Z;

    public static final TickQuadrant[] VALUES = values();

    public static TickQuadrant fromChunkCoordinate(long chunkCoordinate) {

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        if (chunkX >= 0)
            return chunkZ >= 0 ? POSITIVE_X_POSITIVE_Z : POSITIVE_X_NEGATIVE_Z;

        return chunkZ >= 0 ? NEGATIVE_X_POSITIVE_Z : NEGATIVE_X_NEGATIVE_Z;
    }
}