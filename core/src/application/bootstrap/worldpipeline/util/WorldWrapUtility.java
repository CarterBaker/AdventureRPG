package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector2Int;
import engine.util.mathematics.vectors.Vector3;

public class WorldWrapUtility extends EngineUtility {

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

    public static long wrapAroundWorld(WorldHandle worldHandle, long input) {

        Vector2Int worldScale = worldHandle.getWorldScale();
        int maxX = worldScale.x;
        int maxY = worldScale.y;

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

    // Planetary Phase \\

    /*
     * Fractional phase offset (0-1) to add to the global raw time of day for
     * a location at the given chunk coordinate. Derived from that chunk's
     * position along the world's Y span relative to the world's own
     * planetaryOffset. Wraps cleanly at the world edges — a full traversal
     * of world height adds exactly 1.0, which is a no-op against a value
     * that's already cyclic mod 1, so there is no seam.
     */
    public static double wrappedPlanetaryOffset(WorldHandle worldHandle, long chunkCoordinate) {

        int worldHeightChunks = worldHandle.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        if (worldHeightChunks <= 0)
            return 0.0;

        long chunkY = Coordinate2Long.unpackY(chunkCoordinate);
        long wrappedY = ((chunkY % worldHeightChunks) + worldHeightChunks) % worldHeightChunks;
        double yFraction = (double) wrappedY / worldHeightChunks;

        double offset = yFraction - worldHandle.getPlanetaryOffset();

        return (offset % 1.0 + 1.0) % 1.0;
    }
}