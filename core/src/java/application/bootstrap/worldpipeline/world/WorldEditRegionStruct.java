package application.bootstrap.worldpipeline.world;

import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.util.GlobeUtility;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector2Int;
import engine.util.mathematics.vectors.Vector3Double;

public class WorldEditRegionStruct extends StructPackage {

    /*
     * The chunks a run of live world edits reaches, grown edit by edit and
     * cleared once streamed. Each edited pixel span is widened by how far an
     * edit reaches terrain, and every test wraps around the world's edges, so
     * a chunk or macro tile is touched only when the edit can change it. On a
     * globe the edited pixels lie on one image face and bound a cap of the
     * globe, widened the same way, and a chunk is reached when the face cell
     * its grid's chart lays it on falls within the cap; edits spanning image
     * faces reach everything. A biome rebuild can change any part of the
     * field, so it reaches everything.
     */

    // World
    private WorldHandle worldHandle;
    private boolean everything;

    // Bounds — chunks, inclusive and unwrapped
    private int minChunkX;
    private int minChunkZ;
    private int maxChunkX;
    private int maxChunkZ;
    private boolean empty;

    // Cap — what a globe's edits reach
    private final GlobeCapStruct cap = new GlobeCapStruct();

    // Scratch
    private final Vector3Double directionScratch = new Vector3Double();
    private final GlobeFrameStruct frameScratch = new GlobeFrameStruct();

    // Constructor \\

    public WorldEditRegionStruct() {
        clear();
    }

    // Management \\

    public void clear() {
        cap.clear();
        this.worldHandle = null;
        this.everything = false;
        this.minChunkX = Integer.MAX_VALUE;
        this.minChunkZ = Integer.MAX_VALUE;
        this.maxChunkX = Integer.MIN_VALUE;
        this.maxChunkZ = Integer.MIN_VALUE;
        this.empty = true;
    }

    // Edits to two worlds in one run cannot share one box, so they reach everything
    public void includePixels(
            WorldHandle editedWorldHandle,
            int minPixelX,
            int minPixelZ,
            int maxPixelX,
            int maxPixelZ) {

        if (!empty && editedWorldHandle != worldHandle) {
            includeEverything();
            return;
        }

        if (editedWorldHandle.isGlobe()) {
            includeGlobePixels(editedWorldHandle, minPixelX, minPixelZ, maxPixelX, maxPixelZ);
            return;
        }

        int chunksPerPixel = EngineSetting.CHUNKS_PER_PIXEL;
        int margin = EngineSetting.WORLD_LIVE_REBUILD_MARGIN_CHUNKS;

        this.worldHandle = editedWorldHandle;
        this.minChunkX = Math.min(minChunkX, minPixelX * chunksPerPixel - margin);
        this.minChunkZ = Math.min(minChunkZ, minPixelZ * chunksPerPixel - margin);
        this.maxChunkX = Math.max(maxChunkX, (maxPixelX + 1) * chunksPerPixel - 1 + margin);
        this.maxChunkZ = Math.max(maxChunkZ, (maxPixelZ + 1) * chunksPerPixel - 1 + margin);
        this.empty = false;
    }

    // The cap round a span of one image face, widened by the rebuild margin
    private void includeGlobePixels(
            WorldHandle editedWorldHandle,
            int minPixelX,
            int minPixelZ,
            int maxPixelX,
            int maxPixelZ) {

        this.worldHandle = editedWorldHandle;

        cap.includeImagePixels(
                editedWorldHandle, minPixelX, minPixelZ, maxPixelX, maxPixelZ,
                EngineSetting.WORLD_LIVE_REBUILD_MARGIN_CHUNKS * EngineSetting.CHUNK_SIZE
                        / GlobeUtility.getRadiusBlocks(editedWorldHandle));
        this.empty = false;
    }

    public void includeEverything() {
        this.everything = true;
        this.empty = false;
    }

    // Reach \\

    // A square of sizeChunks chunks a side from its origin chunk in a grid, as a macro tile spans; on a globe the face
    // cell the grid's chart lays its middle on, tested against the cap
    public boolean reaches(GridInstance grid, long originChunkCoordinate, int sizeChunks) {

        if (empty)
            return false;

        if (everything)
            return true;

        WorldHandle streamedWorldHandle = grid.getWorldHandle();

        if (streamedWorldHandle != worldHandle)
            return false;

        if (worldHandle.isGlobe())
            return reachesCap(grid, originChunkCoordinate, sizeChunks);

        Vector2Int worldScale = worldHandle.getWorldScale();

        return overlapsWrapped(
                Coordinate2Long.unpackX(originChunkCoordinate),
                sizeChunks,
                minChunkX,
                maxChunkX,
                worldScale.x / EngineSetting.CHUNK_SIZE)
                && overlapsWrapped(
                        Coordinate2Long.unpackY(originChunkCoordinate),
                        sizeChunks,
                        minChunkZ,
                        maxChunkZ,
                        worldScale.y / EngineSetting.CHUNK_SIZE);
    }

    // The area's middle within the cap, widened by the angle from its middle to a corner
    private boolean reachesCap(GridInstance grid, long originChunkCoordinate, int sizeChunks) {

        grid.resolveFrame(
                Coordinate2Long.pack(
                        Coordinate2Long.unpackX(originChunkCoordinate) + sizeChunks / 2,
                        Coordinate2Long.unpackY(originChunkCoordinate) + sizeChunks / 2),
                frameScratch);
        GlobeUtility.resolveChunkDirection(worldHandle, frameScratch.getCanonicalCoordinate(), directionScratch);

        return cap.reaches(directionScratch.x, directionScratch.y, directionScratch.z,
                sizeChunks * EngineSetting.CHUNK_SIZE * Math.sqrt(0.5) / GlobeUtility.getRadiusBlocks(worldHandle));
    }

    // Tests the span against the bounds and their copies one world over on that axis
    private boolean overlapsWrapped(int origin, int size, int min, int max, int worldChunks) {

        int span = max - min + 1;

        if (span >= worldChunks)
            return true;

        return Math.floorMod(origin - min, worldChunks) < span
                || Math.floorMod(min - origin, worldChunks) < size;
    }

    // Accessible \\

    public boolean isEmpty() {
        return empty;
    }
}
