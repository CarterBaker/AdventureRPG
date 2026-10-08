package application.bootstrap.worldpipeline.world;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector2Int;

public class WorldEditRegionStruct extends StructPackage {

    /*
     * The chunks a run of live world edits reaches, grown edit by edit and
     * cleared once streamed. Each edited pixel span is widened by how far an
     * edit reaches terrain, and every test wraps around the world's edges, so
     * a chunk or macro tile is touched only when the edit can change it. A
     * biome rebuild can change any part of the field, so it reaches
     * everything.
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

    // Constructor \\

    public WorldEditRegionStruct() {
        clear();
    }

    // Management \\

    public void clear() {
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

        int chunksPerPixel = EngineSetting.CHUNKS_PER_PIXEL;
        int margin = EngineSetting.WORLD_LIVE_REBUILD_MARGIN_CHUNKS;

        this.worldHandle = editedWorldHandle;
        this.minChunkX = Math.min(minChunkX, minPixelX * chunksPerPixel - margin);
        this.minChunkZ = Math.min(minChunkZ, minPixelZ * chunksPerPixel - margin);
        this.maxChunkX = Math.max(maxChunkX, (maxPixelX + 1) * chunksPerPixel - 1 + margin);
        this.maxChunkZ = Math.max(maxChunkZ, (maxPixelZ + 1) * chunksPerPixel - 1 + margin);
        this.empty = false;
    }

    public void includeEverything() {
        this.everything = true;
        this.empty = false;
    }

    // Reach \\

    public boolean reachesChunk(WorldHandle streamedWorldHandle, long chunkCoordinate) {
        return reachesArea(streamedWorldHandle, chunkCoordinate, 1);
    }

    // A square of sizeChunks chunks a side from its origin chunk, as a macro tile spans
    public boolean reachesArea(WorldHandle streamedWorldHandle, long originChunkCoordinate, int sizeChunks) {

        if (empty)
            return false;

        if (everything)
            return true;

        if (streamedWorldHandle != worldHandle)
            return false;

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
