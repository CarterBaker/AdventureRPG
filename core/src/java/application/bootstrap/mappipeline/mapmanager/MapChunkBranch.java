package application.bootstrap.mappipeline.mapmanager;

import java.nio.ByteBuffer;

import application.bootstrap.mappipeline.map.MapTileInstance;
import application.bootstrap.mappipeline.util.MapShadeUtility;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.GlobeUtility;
import application.bootstrap.worldpipeline.util.LiquidColumnUtility;
import application.bootstrap.worldpipeline.util.SurfacePointStruct;
import application.bootstrap.worldpipeline.world.GlobeFrameStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.memory.BufferUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MapChunkBranch extends BranchPackage {

    /*
     * Main thread — draws the real world over the finest map tiles: for every
     * chunk a streaming grid holds under a finest tile some view shows, the
     * top block of each column, seen from straight above, overwrites the
     * generated terrain in that tile's texture. Water shows its depth over the
     * block it lies on, and land is lit by the same relief rule as the
     * generated tiles, so built, dug and grown blocks read in the same style.
     * A chunk is redrawn whenever its merge version moves, so edits appear as
     * soon as the chunk rebuilds. Blocks are read on the main thread, like
     * every other gameplay query, under the budget MapManager hands in. On a
     * globe a finest tile is a square of a block face: each of its chunks is
     * the one a streaming grid's chart lays on that face cell, read in the
     * chart's axes and turned into the face's as it is drawn.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private TextureManager textureManager;

    // Blocks
    private short airBlockID;

    // Settings
    private int chunkSize;
    private int chunksPerTile;

    // Globe
    private final SurfacePointStruct chartHome = new SurfacePointStruct();
    private final GlobeFrameStruct frameScratch = new GlobeFrameStruct();

    // Scratch
    private int[] columnHeights;
    private int[] columnTopColors;
    private int[] columnSideColors;
    private float[] columnWaterDepths;
    private byte[] chunkPixels;
    private ByteBuffer chunkBuffer;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.chunksPerTile = EngineSetting.MACRO_TILE_SIZE_BLOCKS / chunkSize;

        // Scratch
        int columnCount = chunkSize * chunkSize;

        this.columnHeights = new int[columnCount];
        this.columnTopColors = new int[columnCount];
        this.columnSideColors = new int[columnCount];
        this.columnWaterDepths = new float[columnCount];
        this.chunkPixels = new byte[columnCount * EngineSetting.COLOR_CHANNEL_COUNT];
        this.chunkBuffer = BufferUtility.newByteBuffer(chunkPixels.length);
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
        this.textureManager = get(TextureManager.class);
    }

    @Override
    protected void awake() {
        this.airBlockID = (short) blockManager.getBlockIDFromBlockName(EngineSetting.AIR_BLOCK_NAME);
    }

    // Tile \\

    // Redraws the tile's stale chunks, at most budget of them, and returns what is left of the budget
    int refreshTile(MapTileInstance tile, WorldHandle worldHandle, int budget) {

        int originChunkX = tile.getTileX() * chunksPerTile;
        int originChunkZ = tile.getTileZ() * chunksPerTile;
        GridInstance grid = worldHandle.isGlobe() ? findGrid(worldHandle) : null;

        if (worldHandle.isGlobe() && grid == null)
            return budget;

        if (grid != null)
            homeOnGrid(worldHandle, grid);

        for (int z = 0; z < chunksPerTile && budget > 0; z++) {
            for (int x = 0; x < chunksPerTile && budget > 0; x++) {

                int chunkIndex = z * chunksPerTile + x;
                long coordinate = Coordinate2Long.pack(originChunkX + x, originChunkZ + z);
                ChunkInstance chunk = grid != null
                        ? findChartChunk(grid, coordinate)
                        : worldStreamManager.getChunkInstance(coordinate);

                if (chunk == null || !chunk.getChunkDataSyncContainer().hasData(ChunkData.GENERATION_DATA))
                    continue;

                long version = chunk.getMergeVersion();

                if (tile.getChunkVersion(chunkIndex) == version)
                    continue;

                drawChunk(tile, chunk, x, z);
                tile.setChunkVersion(chunkIndex, version);
                budget--;
            }
        }

        return budget;
    }

    // Globe \\

    private GridInstance findGrid(WorldHandle worldHandle) {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();

        for (int i = 0; i < grids.size(); i++)
            if (grids.get(i).getWorldHandle() == worldHandle)
                return grids.get(i);

        return null;
    }

    // The chart's home at the grid's active chunk, so a face cell is found where the chart lays it
    private void homeOnGrid(WorldHandle worldHandle, GridInstance grid) {

        long activeChunk = grid.getActiveChunkCoordinate();

        grid.resolveFrame(activeChunk, frameScratch);
        chartHome.setHome(worldHandle, activeChunk, frameScratch.getCanonicalCoordinate(), frameScratch.getTurns());
    }

    // The chunk the grid holds on a face cell, none when the chart lays that cell elsewhere or does not hold it
    private ChunkInstance findChartChunk(GridInstance grid, long canonicalChunk) {

        long middleX = (long) Coordinate2Long.unpackX(canonicalChunk) * chunkSize + chunkSize / 2;
        long middleZ = (long) Coordinate2Long.unpackY(canonicalChunk) * chunkSize + chunkSize / 2;
        ChunkInstance chunk = grid.getActiveChunks().get(Coordinate2Long.pack(
                (int) Math.floorDiv(chartHome.toChartBlockX(middleX, middleZ), chunkSize),
                (int) Math.floorDiv(chartHome.toChartBlockZ(middleX, middleZ), chunkSize)));

        return chunk != null && chunk.getCanonicalCoordinate() == canonicalChunk ? chunk : null;
    }

    // Chunk \\

    // Every column read in the chunk's own axes and laid where it stands in its face cell's, turned by the chunk's
    // frame; a torus chunk is unturned
    private void drawChunk(MapTileInstance tile, ChunkInstance chunk, int chunkX, int chunkZ) {

        SubChunkInstance[] subChunks = chunk.getSubChunks();
        int topY = findHighestOccupiedY(subChunks);
        int turns = chunk.getFrameTurns();

        for (int z = 0; z < chunkSize; z++)
            for (int x = 0; x < chunkSize; x++)
                resolveColumn(subChunks, topY, x, z, toFaceColumn(x, z, turns));

        for (int z = 0; z < chunkSize; z++)
            for (int x = 0; x < chunkSize; x++)
                shadeColumn(x, z);

        chunkBuffer.clear();
        chunkBuffer.put(chunkPixels);
        textureManager.updateTexture2D(
                tile.getTexture(), chunkX * chunkSize, chunkZ * chunkSize, chunkSize, chunkSize, chunkBuffer);
    }

    // The top block Y of the highest subchunk holding anything, so a column search skips the open sky
    private int findHighestOccupiedY(SubChunkInstance[] subChunks) {

        for (int i = subChunks.length - 1; i >= 0; i--) {

            SubChunkInstance subChunk = subChunks[i];

            if (subChunk.isKnownEmpty())
                continue;

            if (!subChunk.isPopulated() && (!subChunk.isUniformFill() || subChunk.getUniformBlockID() == airBlockID))
                continue;

            return i * chunkSize + chunkSize - 1;
        }

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // A column of the chunk's own axes as its column of the face cell's, turned about the chunk's middle
    private int toFaceColumn(int x, int z, int turns) {

        int doubledX = 2 * x + 1 - chunkSize;
        int doubledZ = 2 * z + 1 - chunkSize;
        int faceX = (GlobeUtility.turnX(doubledX, doubledZ, turns) + chunkSize - 1) / 2;
        int faceZ = (GlobeUtility.turnZ(doubledX, doubledZ, turns) + chunkSize - 1) / 2;

        return faceZ * chunkSize + faceX;
    }

    private void resolveColumn(SubChunkInstance[] subChunks, int topY, int x, int z, int column) {

        int y = findBlockBelow(subChunks, topY, x, z);

        columnWaterDepths[column] = 0f;

        if (y == EngineSetting.INDEX_NOT_FOUND) {
            columnHeights[column] = 0;
            columnTopColors[column] = EngineSetting.MAP_COLOR_UNKNOWN;
            columnSideColors[column] = EngineSetting.MAP_COLOR_UNKNOWN;
            return;
        }

        BlockHandle block = getBlock(subChunks, x, y, z);
        int surfaceY = y;

        if (LiquidColumnUtility.isLiquid(block)) {

            int floorY = y;

            while (floorY >= 0 && LiquidColumnUtility.isLiquid(block)) {
                floorY = findBlockBelow(subChunks, floorY - 1, x, z);
                block = floorY == EngineSetting.INDEX_NOT_FOUND ? null : getBlock(subChunks, x, floorY, z);
            }

            columnWaterDepths[column] = surfaceY - floorY;
        }

        columnHeights[column] = surfaceY;
        columnTopColors[column] = resolveFaceColor(block, Direction3Vector.UP);
        columnSideColors[column] = resolveFaceColor(block, Direction3Vector.NORTH);
    }

    private int findBlockBelow(SubChunkInstance[] subChunks, int fromY, int x, int z) {

        for (int y = fromY; y >= 0; y--)
            if (subChunks[y / chunkSize].getBlock(x, y % chunkSize, z) != airBlockID)
                return y;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private BlockHandle getBlock(SubChunkInstance[] subChunks, int x, int y, int z) {
        return blockManager.getBlockHandleFromBlockID(subChunks[y / chunkSize].getBlock(x, y % chunkSize, z));
    }

    private int resolveFaceColor(BlockHandle block, Direction3Vector face) {
        return block != null && block.hasMapColor()
                ? block.getMapColorForFace(face)
                : EngineSetting.MAP_COLOR_UNKNOWN;
    }

    // Each column slopes toward its neighbours inside the chunk, one-sided along the chunk's edge
    private void shadeColumn(int x, int z) {

        int column = z * chunkSize + x;
        int color;

        if (columnWaterDepths[column] > 0f)
            color = MapShadeUtility.shadeWater(columnTopColors[column], columnWaterDepths[column]);
        else {
            int westX = Math.max(x - 1, 0);
            int eastX = Math.min(x + 1, chunkSize - 1);
            int northZ = Math.max(z - 1, 0);
            int southZ = Math.min(z + 1, chunkSize - 1);

            float slopeX = (columnHeights[z * chunkSize + eastX] - columnHeights[z * chunkSize + westX])
                    / (float) (eastX - westX);
            float slopeZ = (columnHeights[southZ * chunkSize + x] - columnHeights[northZ * chunkSize + x])
                    / (float) (southZ - northZ);

            color = MapShadeUtility.shadeLand(
                    columnTopColors[column], columnSideColors[column], slopeX, slopeZ, 1f);
        }

        MapShadeUtility.writeTexel(chunkPixels, column, color);
    }
}
