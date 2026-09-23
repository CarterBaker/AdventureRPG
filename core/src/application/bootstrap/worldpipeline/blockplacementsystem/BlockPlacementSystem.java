package application.bootstrap.worldpipeline.blockplacementsystem;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.liquidmanager.LiquidManager;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.ChunkCoordinate3Int;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.extras.Direction3Vector;

public class BlockPlacementSystem extends SystemPackage {

    /*
     * The engine's single entry point for editing a block in the loaded
     * world, whether breaking, building, or pouring liquid. It writes the
     * block, wakes any liquid the edit could set moving, and rebuilds and
     * re-merges every subchunk whose geometry the edit touched across
     * subchunk and chunk borders. Each chunk is written and rebuilt under its
     * own ChunkDataSyncContainer lock, since the streaming pool may be
     * building or merging that same chunk concurrently.
     */

    private static final Direction3Vector[] LATERAL_DIRECTIONS = {
            Direction3Vector.NORTH, Direction3Vector.EAST, Direction3Vector.SOUTH, Direction3Vector.WEST
    };

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private LiquidManager liquidManager;
    private DynamicGeometryManager dynamicGeometryManager;
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;
    private WorldRenderManager worldRenderManager;

    // Settings
    private int worldHeight;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.worldHeight = EngineSetting.WORLD_HEIGHT;
    }

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
        this.liquidManager = get(LiquidManager.class);
        this.dynamicGeometryManager = get(DynamicGeometryManager.class);
        this.dynamicGeometryAsyncContainer = dynamicGeometryManager.getDynamicGeometryAsyncInstance();
        this.worldRenderManager = get(WorldRenderManager.class);
    }

    // Placement \\

    public boolean replaceBlock(BlockCastStruct castStruct, short blockID) {

        ChunkInstance chunk = worldStreamManager.getChunkInstance(castStruct.getChunkCoordinate());

        if (chunk == null)
            return false;

        int packedXYZ = Coordinate3Int.pack(castStruct.getBlockX(), castStruct.getBlockY(), castStruct.getBlockZ());
        placeBlock(chunk, castStruct.getSubChunkY(), packedXYZ, blockID);

        return true;
    }

    public boolean placeBlockAgainstFace(BlockCastStruct castStruct, short blockID) {

        ChunkInstance chunk = worldStreamManager.getChunkInstance(castStruct.getChunkCoordinate());

        if (chunk == null)
            return false;

        Direction3Vector hitFace = castStruct.getHitFace();
        int subChunkY = castStruct.getSubChunkY();
        int hitPackedXYZ = Coordinate3Int.pack(castStruct.getBlockX(), castStruct.getBlockY(), castStruct.getBlockZ());
        int packedXYZ = ChunkCoordinate3Int.getNeighborAndWrap(hitPackedXYZ, hitFace);

        if (!ChunkCoordinate3Int.isAtEdge(hitPackedXYZ, hitFace)) {
            placeBlock(chunk, subChunkY, packedXYZ, blockID);
            return true;
        }

        if (hitFace == Direction3Vector.UP || hitFace == Direction3Vector.DOWN) {

            int targetSubChunkY = subChunkY + hitFace.y;

            if (targetSubChunkY < 0 || targetSubChunkY >= worldHeight)
                return false;

            placeBlock(chunk, targetSubChunkY, packedXYZ, blockID);
            return true;
        }

        ChunkInstance targetChunk = chunk.getChunkNeighbors().getNeighborChunk(hitFace.to2D().index);

        if (targetChunk == null)
            return false;

        placeBlock(targetChunk, subChunkY, packedXYZ, blockID);
        return true;
    }

    private void placeBlock(ChunkInstance chunk, int subChunkY, int packedXYZ, short blockID) {

        ChunkDataSyncContainer syncContainer = chunk.getChunkDataSyncContainer();
        syncContainer.acquire();

        try {
            writeBlock(chunk.getSubChunk(subChunkY), packedXYZ, blockID);
            liquidManager.wakeLiquid(chunk, subChunkY, packedXYZ);

            rebuildSubChunk(chunk, subChunkY);

            if (ChunkCoordinate3Int.isAtEdge(packedXYZ, Direction3Vector.DOWN) && subChunkY > 0)
                rebuildSubChunk(chunk, subChunkY - 1);

            if (ChunkCoordinate3Int.isAtEdge(packedXYZ, Direction3Vector.UP) && subChunkY < worldHeight - 1)
                rebuildSubChunk(chunk, subChunkY + 1);

            mergeAndRender(chunk);
        } finally {
            syncContainer.release();
        }

        invalidateChunk(chunk.getCoordinate());

        for (int d = 0; d < LATERAL_DIRECTIONS.length; d++)
            if (ChunkCoordinate3Int.isAtEdge(packedXYZ, LATERAL_DIRECTIONS[d]))
                rebuildNeighbourChunk(chunk, subChunkY, LATERAL_DIRECTIONS[d]);
    }

    private void writeBlock(SubChunkInstance subChunk, int packedXYZ, short blockID) {

        subChunk.setBlock(packedXYZ, blockID);

        if (blockManager.getGeometryFromBlockID(blockID) == DynamicGeometryType.LIQUID)
            subChunk.setLiquidLevel(packedXYZ, EngineSetting.LIQUID_LEVEL_MAX);
    }

    // Rebuild \\

    private void rebuildNeighbourChunk(ChunkInstance chunk, int subChunkY, Direction3Vector direction) {

        ChunkInstance neighbour = chunk.getChunkNeighbors().getNeighborChunk(direction.to2D().index);

        if (neighbour == null)
            return;

        ChunkDataSyncContainer neighbourSync = neighbour.getChunkDataSyncContainer();
        neighbourSync.acquire();

        try {
            rebuildSubChunk(neighbour, subChunkY);
            mergeAndRender(neighbour);
        } finally {
            neighbourSync.release();
        }

        invalidateChunk(neighbour.getCoordinate());
    }

    private void rebuildSubChunk(ChunkInstance chunk, int subChunkY) {
        chunk.getSubChunk(subChunkY).getDynamicPacketInstance().clear();
        dynamicGeometryManager.buildSubChunk(dynamicGeometryAsyncContainer, chunk, subChunkY);
    }

    private void mergeAndRender(ChunkInstance chunk) {
        chunk.merge();
        worldRenderManager.addChunkInstance(chunk);
    }

    private void invalidateChunk(long chunkCoordinate) {
        worldStreamManager.invalidateMegaForChunk(chunkCoordinate);
        worldStreamManager.invalidateChunkBatch(chunkCoordinate);
    }
}
