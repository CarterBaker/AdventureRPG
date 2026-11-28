package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector3Double;

public class WorldWrapUtility extends EngineUtility {

    /*
     * Wraps chunk-local positions, chunk coordinates and block coordinates
     * around the world's edges, and measures the shortest wrapped distance
     * between two positions. A globe world reads its planetary phase,
     * latitude and the heading of true north from its true direction on the
     * ball instead (see GlobeUtility). A globe grid's coordinates are cells
     * of its own flat chart, which never wraps, so every wrap below leaves a
     * globe's positions as they are and every delta is a plain difference.
     */

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

    public static long wrapAroundWorld(WorldHandle worldHandle, long input) {

        if (worldHandle.isGlobe())
            return input;

        int maxX = spanX(worldHandle) / EngineSetting.CHUNK_SIZE;
        int maxY = spanZ(worldHandle) / EngineSetting.CHUNK_SIZE;

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

    public static long unwrapToGridCoordinate(
            WorldHandle worldHandle,
            long activeChunkCoordinate,
            long chunkCoordinate) {

        int worldWidthChunks = spanX(worldHandle) / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = spanZ(worldHandle) / EngineSetting.CHUNK_SIZE;

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
        int worldWidthChunks = spanX(worldHandle) / EngineSetting.CHUNK_SIZE;
        return wrappedDelta(a, b, worldWidthChunks);
    }

    public static double wrappedDeltaZ(WorldHandle worldHandle, double a, double b) {
        int worldHeightChunks = spanZ(worldHandle) / EngineSetting.CHUNK_SIZE;
        return wrappedDelta(a, b, worldHeightChunks);
    }

    // Block Wrap \\

    public static long wrapBlockX(WorldHandle worldHandle, long blockX) {
        return wrapPeriod(blockX, spanX(worldHandle));
    }

    public static long wrapBlockZ(WorldHandle worldHandle, long blockZ) {
        return wrapPeriod(blockZ, spanZ(worldHandle));
    }

    public static double wrapBlockX(WorldHandle worldHandle, double blockX) {
        return wrapPeriod(blockX, (double) spanX(worldHandle));
    }

    public static double wrapBlockZ(WorldHandle worldHandle, double blockZ) {
        return wrapPeriod(blockZ, (double) spanZ(worldHandle));
    }

    private static long wrapPeriod(long value, long period) {
        return period <= 0 ? value : Math.floorMod(value, period);
    }

    private static double wrapPeriod(double value, double period) {

        if (period <= 0.0)
            return value;

        double wrapped = value % period;

        return wrapped < 0.0 ? wrapped + period : wrapped;
    }

    public static long wrappedBlockDeltaX(WorldHandle worldHandle, long a, long b) {
        return (long) wrappedDelta(a, b, spanX(worldHandle));
    }

    public static long wrappedBlockDeltaZ(WorldHandle worldHandle, long a, long b) {
        return (long) wrappedDelta(a, b, spanZ(worldHandle));
    }

    public static double wrappedBlockDeltaX(WorldHandle worldHandle, double a, double b) {
        return wrappedDelta(a, b, spanX(worldHandle));
    }

    public static double wrappedBlockDeltaZ(WorldHandle worldHandle, double a, double b) {
        return wrappedDelta(a, b, spanZ(worldHandle));
    }

    // The copy of a block position, across the world's wrap, nearest a reference — the unwrapped frame a plan
    // measured around that reference keeps its positions in
    public static double unwrapBlockX(WorldHandle worldHandle, double blockX, double referenceX) {
        return referenceX + wrappedBlockDeltaX(worldHandle, blockX, referenceX);
    }

    public static double unwrapBlockZ(WorldHandle worldHandle, double blockZ, double referenceZ) {
        return referenceZ + wrappedBlockDeltaZ(worldHandle, blockZ, referenceZ);
    }

    // Span \\

    // The blocks a torus wraps after along each axis, none for a globe's chart
    private static int spanX(WorldHandle worldHandle) {
        return worldHandle.isGlobe() ? 0 : worldHandle.getWorldScale().x;
    }

    private static int spanZ(WorldHandle worldHandle) {
        return worldHandle.isGlobe() ? 0 : worldHandle.getWorldScale().y;
    }

    // The blocks once around the world: a torus's X span, or a great circle of a globe, four faces across
    public static double getCircumferenceBlocks(WorldHandle worldHandle) {
        return worldHandle.isGlobe()
                ? (double) GlobeUtility.getFaceBlocks(worldHandle) * EngineSetting.GLOBE_GREAT_CIRCLE_FACES
                : worldHandle.getWorldScale().x;
    }

    // Axis Fraction \\

    private static double wrappedXFraction(WorldHandle worldHandle, long chunkCoordinate) {
        return wrappedFraction(Coordinate2Long.unpackX(chunkCoordinate), worldHandle.getWorldScale().x);
    }

    private static double wrappedYFraction(WorldHandle worldHandle, long chunkCoordinate) {
        return wrappedFraction(Coordinate2Long.unpackY(chunkCoordinate), worldHandle.getWorldScale().y);
    }

    private static double wrappedFraction(long chunk, int worldSpanBlocks) {

        int worldSpanChunks = worldSpanBlocks / EngineSetting.CHUNK_SIZE;

        if (worldSpanChunks <= 0)
            return 0.0;

        long wrapped = ((chunk % worldSpanChunks) + worldSpanChunks) % worldSpanChunks;

        return (double) wrapped / worldSpanChunks;
    }

    // Planetary Phase \\

    public static double wrappedPlanetaryOffset(
            WorldHandle worldHandle,
            long chunkCoordinate,
            Vector3Double scratch) {

        double xFraction = worldHandle.isGlobe()
                ? GlobeUtility.resolveLongitudeFraction(resolveGlobeDirection(worldHandle, chunkCoordinate, scratch))
                : wrappedXFraction(worldHandle, chunkCoordinate);
        double offset = xFraction - worldHandle.getPlanetaryOffset();

        return (offset % 1.0 + 1.0) % 1.0;
    }

    // Latitude Bend \\

    public static double wrappedLatitudeFactor(
            WorldHandle worldHandle,
            long chunkCoordinate,
            Vector3Double scratch) {

        if (worldHandle.isGlobe())
            return GlobeUtility.resolveLatitudeFactor(resolveGlobeDirection(worldHandle, chunkCoordinate, scratch));

        double yFraction = wrappedYFraction(worldHandle, chunkCoordinate);

        return Math.sin(yFraction * TWO_PI);
    }

    // North Heading \

    // The angle in radians from the grid's +Z toward its +X at which true north lies — a torus grid's +Z is north
    public static double wrappedNorthHeading(
            WorldHandle worldHandle,
            long chunkCoordinate,
            Vector3Double scratch) {

        if (!worldHandle.isGlobe())
            return 0.0;

        return GlobeUtility.resolveNorthHeading(
                worldHandle,
                chunkCoordinate,
                resolveGlobeDirection(worldHandle, chunkCoordinate, scratch));
    }

    private static Vector3Double resolveGlobeDirection(
            WorldHandle worldHandle,
            long chunkCoordinate,
            Vector3Double scratch) {

        GlobeUtility.resolveChunkDirection(worldHandle, chunkCoordinate, scratch);

        return scratch;
    }
}