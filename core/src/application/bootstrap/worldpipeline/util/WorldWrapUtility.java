// WorldWrapUtility.java
package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector2Int;
import engine.util.mathematics.vectors.Vector3;

public class WorldWrapUtility extends EngineUtility {

    private static final double TWO_PI = Math.PI * 2.0;

    public static Vector3 wrapAroundChunk(Vector3 input) {

        float x = input.x % EngineSetting.CHUNK_SIZE;
        if (x < 0)
            x += EngineSetting.CHUNK_SIZE;

        float z = input.z % EngineSetting.CHUNK_SIZE;
        if (z < 0)
            z += EngineSetting.CHUNK_SIZE;

        input.x = x;
        input.z = z;

        return input;
    }

    /*
     * Wraps an absolute chunk coordinate around the world's chunk-space
     * bounds. worldScale is stored in blocks, so it must be divided down to
     * chunk units before use as the modulus here — every other chunk-space
     * wrap in this class already does this (see wrappedDeltaX/Z); this was
     * the one holdout still wrapping against the raw block-scale figure,
     * which made the modulus CHUNK_SIZE times too large and sent any chunk
     * near a real wrap seam — including the world's own origin — to a
     * coordinate nowhere near the seam it was supposed to land on.
     */
    public static long wrapAroundWorld(WorldHandle worldHandle, long input) {

        Vector2Int worldScale = worldHandle.getWorldScale();
        int maxX = worldScale.x / EngineSetting.CHUNK_SIZE;
        int maxY = worldScale.y / EngineSetting.CHUNK_SIZE;

        int inputX = Coordinate2Long.unpackX(input);
        int inputY = Coordinate2Long.unpackY(input);

        int x = inputX % maxX;
        if (x < 0)
            x += maxX;

        int y = inputY % maxY;
        if (y < 0)
            y += maxY;

        return Coordinate2Long.pack(x, y);
    }

    /*
     * Inverse of wrapAroundWorld/getChunkCoordinateForSlot: recovers the
     * signed grid-relative offset for an absolute (already-wrapped) chunk
     * coordinate against a grid's current active chunk coordinate. A naive
     * subtraction only works when the two coordinates never crossed a wrap
     * seam; whenever activeChunkCoordinate sits within render distance of a
     * seam, the raw difference lands far outside the small
     * -radius..+radius range gridSlots is keyed by, and every lookup keyed
     * off it silently misses. This walks each axis back to the shortest
     * signed delta on the world's circular chunk-space domain instead,
     * which always reproduces the exact grid coordinate the slot was
     * created under, seam or no seam.
     */
    public static long unwrapToGridCoordinate(
            WorldHandle worldHandle,
            long activeChunkCoordinate,
            long chunkCoordinate) {

        Vector2Int worldScale = worldHandle.getWorldScale();
        int worldWidthChunks = worldScale.x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = worldScale.y / EngineSetting.CHUNK_SIZE;

        int deltaX = unwrapAxisDelta(
                Coordinate2Long.unpackX(chunkCoordinate),
                Coordinate2Long.unpackX(activeChunkCoordinate),
                worldWidthChunks);

        int deltaY = unwrapAxisDelta(
                Coordinate2Long.unpackY(chunkCoordinate),
                Coordinate2Long.unpackY(activeChunkCoordinate),
                worldHeightChunks);

        return Coordinate2Long.pack(deltaX, deltaY);
    }

    private static int unwrapAxisDelta(int coordinate, int activeCoordinate, int worldSizeChunks) {

        if (worldSizeChunks <= 0)
            return coordinate - activeCoordinate;

        int delta = ((coordinate - activeCoordinate) % worldSizeChunks + worldSizeChunks) % worldSizeChunks;

        if (delta > worldSizeChunks / 2)
            delta -= worldSizeChunks;

        return delta;
    }

    // Wrapped Delta \\

    public static double wrappedDelta(double a, double b, double period) {

        if (period <= 0)
            return a - b;

        double delta = a - b;
        double halfPeriod = period * 0.5;

        return ((delta + halfPeriod) % period + period) % period - halfPeriod;
    }

    public static double wrappedDeltaX(WorldHandle worldHandle, double a, double b) {
        int worldWidthChunks = worldHandle.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        return wrappedDelta(a, b, worldWidthChunks);
    }

    public static double wrappedDeltaZ(WorldHandle worldHandle, double a, double b) {
        int worldHeightChunks = worldHandle.getWorldScale().y / EngineSetting.CHUNK_SIZE;
        return wrappedDelta(a, b, worldHeightChunks);
    }

    // Y-Axis Fraction \\

    private static double wrappedYFraction(WorldHandle worldHandle, long chunkCoordinate) {

        int worldHeightChunks = worldHandle.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        if (worldHeightChunks <= 0)
            return 0.0;

        long chunkY = Coordinate2Long.unpackY(chunkCoordinate);
        long wrappedY = ((chunkY % worldHeightChunks) + worldHeightChunks) % worldHeightChunks;

        return (double) wrappedY / worldHeightChunks;
    }

    // Planetary Phase \\

    public static double wrappedPlanetaryOffset(WorldHandle worldHandle, long chunkCoordinate) {

        double yFraction = wrappedYFraction(worldHandle, chunkCoordinate);
        double offset = yFraction - worldHandle.getPlanetaryOffset();

        return (offset % 1.0 + 1.0) % 1.0;
    }

    // Latitude Bend \\

    public static double wrappedLatitudeFactor(WorldHandle worldHandle, long chunkCoordinate) {

        double yFraction = wrappedYFraction(worldHandle, chunkCoordinate);

        return Math.sin(yFraction * TWO_PI);
    }
}