package application.bootstrap.mappipeline.mapmanager;

import java.nio.ByteBuffer;

import application.bootstrap.mappipeline.map.MapTileInstance;
import application.bootstrap.mappipeline.util.MapShadeUtility;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.LiquidColumnUtility;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.memory.BufferUtility;

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
     * every other gameplay query, under the budget MapManager hands in.
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
    int refreshTile(MapTileInstance tile, int budget) {

        int originChunkX = tile.getTileX() * chunksPerTile;
        int originChunkZ = tile.getTileZ() * chunksPerTile;

        for (int z = 0; z < chunksPerTile && budget > 0; z++) {
            for (int x = 0; x < chunksPerTile && budget > 0; x++) {

                int chunkIndex = z * chunksPerTile + x;
                ChunkInstance chunk = worldStreamManager.getChunkInstance(
                        Coordinate2Long.pack(originChunkX + x, originChunkZ + z));

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

    // Chunk \\

    private void drawChunk(MapTileInstance tile, ChunkInstance chunk, int chunkX, int chunkZ) {

        SubChunkInstance[] subChunks = chunk.getSubChunks();
        int topY = findHighestOccupiedY(subChunks);

        for (int z = 0; z < chunkSize; z++)
            for (int x = 0; x < chunkSize; x++)
                resolveColumn(subChunks, topY, x, z);

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

    private void resolveColumn(SubChunkInstance[] subChunks, int topY, int x, int z) {

        int column = z * chunkSize + x;
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
