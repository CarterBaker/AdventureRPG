package application.bootstrap.worldpipeline.util;

import engine.util.mathematics.extras.Coordinate2Long;

public enum TickQuadrant {

    /*
     * Quarters the active chunk set by the parity of each chunk's X and Z
     * coordinate rather than by sign — chunk coordinates are wrapped into
     * [0, worldSize) by WorldWrapUtility and are therefore never negative,
     * so a sign-based split would always land every chunk in the same
     * quadrant. Lets LiquidTickBranch visit one quarter of the world's
     * liquid-bearing chunks per firing instead of every one of them at once.
     */

    EVEN_X_EVEN_Z,
    ODD_X_EVEN_Z,
    EVEN_X_ODD_Z,
    ODD_X_ODD_Z;

    public static final TickQuadrant[] VALUES = values();

    public static TickQuadrant fromChunkCoordinate(long chunkCoordinate) {

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        boolean oddX = (chunkX & 1) != 0;
        boolean oddZ = (chunkZ & 1) != 0;

        if (!oddX)
            return oddZ ? EVEN_X_ODD_Z : EVEN_X_EVEN_Z;

        return oddZ ? ODD_X_ODD_Z : ODD_X_EVEN_Z;
    }
}