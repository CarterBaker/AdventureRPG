package application.bootstrap.worldpipeline.liquidmanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.ChunkCoordinateUtility;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class LiquidManager extends ManagerPackage {

    /*
     * Owns the liquid simulation. A chunk tick runs flow() on each subchunk,
     * claims any neighbor chunk it reaches under that chunk's lock, and
     * rebuilds what changed. writeLiquid() and clearLiquid() wake the cells
     * that can now move, deferring wakes that cannot claim their chunk; tidal
     * writes re-level the ocean without waking anything.
     */

    private static final Direction3Vector[] WAKE_DIRECTIONS = {
            Direction3Vector.UP,
            Direction3Vector.NORTH, Direction3Vector.EAST, Direction3Vector.SOUTH, Direction3Vector.WEST
    };

    // Internal
    private BlockManager blockManager;
    private WorldStreamManager worldStreamManager;

    // Branches
    private LiquidFlowBranch liquidFlowBranch;
    private LiquidTideBranch liquidTideBranch;

    // Settings
    private int worldHeight;

    // Block IDs
    private short airBlockID;

    // Chunk Tick
    private ChunkInstance tickChunk;
    private ObjectArrayList<ChunkInstance> claimedChunks;
    private ObjectArrayList<ChunkInstance> touchedChunks;
    private IntArrayList touchedSubChunkY;

    // Deferred Wakes
    private ObjectArrayList<ChunkInstance> deferredWakeChunks;
    private LongArrayList deferredWakeChunkCoordinates;
    private IntArrayList deferredWakeSubChunkY;
    private IntArrayList deferredWakePackedXYZ;

    // Scratch
    private LiquidCellStruct editCell;
    private LiquidCellStruct wakeCell;

    // Base \\

    @Override
    protected void create() {

        // Branches
        this.liquidFlowBranch = create(LiquidFlowBranch.class);
        create(LiquidBasinBranch.class);
        this.liquidTideBranch = create(LiquidTideBranch.class);

        // Settings
        this.worldHeight = EngineSetting.WORLD_HEIGHT;

        // Chunk Tick
        this.claimedChunks = new ObjectArrayList<>();
        this.touchedChunks = new ObjectArrayList<>();
        this.touchedSubChunkY = new IntArrayList();

        // Deferred Wakes
        this.deferredWakeChunks = new ObjectArrayList<>();
        this.deferredWakeChunkCoordinates = new LongArrayList();
        this.deferredWakeSubChunkY = new IntArrayList();
        this.deferredWakePackedXYZ = new IntArrayList();

        // Scratch
        this.editCell = new LiquidCellStruct();
        this.wakeCell = new LiquidCellStruct();
    }

    @Override
    protected void get() {

        // Internal
        this.blockManager = get(BlockManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    @Override
    protected void awake() {
        this.airBlockID = (short) blockManager.getBlockIDFromBlockName(EngineSetting.AIR_BLOCK_NAME);
    }

    // Chunk Tick \\

    public void beginChunkTick(ChunkInstance chunkInstance) {

        if (tickChunk != null)
            throwException("A liquid chunk tick was opened while another was still open — "
                    + "every beginChunkTick() must be closed by endChunkTick() first.");

        this.tickChunk = chunkInstance;
        claimedChunks.clear();
        touchedChunks.clear();
        touchedSubChunkY.clear();
    }

    public void flow(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance) {

        if (chunkInstance != tickChunk)
            throwException("Liquid flow ran on a chunk outside the open chunk tick.");

        liquidFlowBranch.flow(chunkInstance, subChunkInstance);
    }

    public boolean tide(ChunkInstance chunkInstance, int surfaceLevels) {

        if (chunkInstance != tickChunk)
            throwException("The tide ran on a chunk outside the open chunk tick.");

        return liquidTideBranch.applyTide(chunkInstance, surfaceLevels);
    }

    public void endChunkTick() {

        for (int i = 0; i < claimedChunks.size(); i++)
            claimedChunks.get(i).getChunkDataSyncContainer().release();

        claimedChunks.clear();
        tickChunk = null;
    }

    // Claims \\

    boolean claimChunk(ChunkInstance chunkInstance) {

        if (tickChunk == null)
            return false;

        if (chunkInstance == tickChunk || claimedChunks.contains(chunkInstance))
            return true;

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        if (!syncContainer.tryAcquire())
            return false;

        if (!syncContainer.getData()[ChunkData.GENERATION_DATA.index]) {
            syncContainer.release();
            return false;
        }

        claimedChunks.add(chunkInstance);
        return true;
    }

    // Touched \\

    void markTouched(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance) {

        int subChunkY = (int) subChunkInstance.getCoordinate();

        for (int i = 0; i < touchedChunks.size(); i++)
            if (touchedChunks.get(i) == chunkInstance && touchedSubChunkY.getInt(i) == subChunkY)
                return;

        touchedChunks.add(chunkInstance);
        touchedSubChunkY.add(subChunkY);
    }

    // Neighbor Resolution \\

    boolean resolveNeighbor(LiquidCellStruct from, Direction3Vector direction, LiquidCellStruct out) {

        ChunkInstance chunkInstance = from.getChunkInstance();
        SubChunkInstance subChunkInstance = from.getSubChunkInstance();
        int packedXYZ = from.getPackedXYZ();
        int neighborPackedXYZ = ChunkCoordinateUtility.getNeighborAndWrap(packedXYZ, direction);

        if (!ChunkCoordinateUtility.isAtEdge(packedXYZ, direction)) {
            out.set(chunkInstance, subChunkInstance, neighborPackedXYZ);
            return true;
        }

        int subChunkY = (int) subChunkInstance.getCoordinate();

        if (direction == Direction3Vector.UP || direction == Direction3Vector.DOWN) {

            int neighborSubChunkY = subChunkY + direction.y;

            if (neighborSubChunkY < 0 || neighborSubChunkY >= worldHeight)
                return false;

            out.set(chunkInstance, chunkInstance.getSubChunk(neighborSubChunkY), neighborPackedXYZ);
            return true;
        }

        ChunkInstance neighborChunk = chunkInstance.getChunkNeighbors().getNeighborChunk(direction.to2D().index);

        if (neighborChunk == null)
            return false;

        out.set(neighborChunk, neighborChunk.getSubChunk(subChunkY), neighborPackedXYZ);
        return true;
    }

    // Cell Queries \\

    boolean isLiquid(short blockID) {
        return blockManager.getGeometryFromBlockID(blockID) == DynamicGeometryType.LIQUID;
    }

    short getBlock(LiquidCellStruct cell) {
        return cell.getSubChunkInstance().getBlock(cell.getPackedXYZ());
    }

    int getLevel(LiquidCellStruct cell) {
        return cell.getSubChunkInstance().getLiquidLevel(cell.getPackedXYZ());
    }

    boolean isPermanent(LiquidCellStruct cell) {
        return cell.getSubChunkInstance().isLiquidPermanent(cell.getPackedXYZ());
    }

    int getFillableLevel(LiquidCellStruct cell, short liquidBlockID) {

        short blockID = getBlock(cell);

        if (blockID == airBlockID)
            return EngineSetting.LIQUID_LEVEL_EMPTY;

        if (blockID == liquidBlockID)
            return getLevel(cell);

        return EngineSetting.LIQUID_LEVEL_BLOCKED;
    }

    // Cell Writes \\

    void writeLiquid(LiquidCellStruct cell, short liquidBlockID, int level) {

        if (level <= EngineSetting.LIQUID_LEVEL_EMPTY) {
            clearLiquid(cell);
            return;
        }

        SubChunkInstance subChunkInstance = cell.getSubChunkInstance();
        int packedXYZ = cell.getPackedXYZ();

        if (subChunkInstance.getBlock(packedXYZ) != liquidBlockID)
            subChunkInstance.setBlock(packedXYZ, liquidBlockID);

        subChunkInstance.setLiquidLevel(packedXYZ, (short) level);
        subChunkInstance.activateLiquid(packedXYZ);

        commitChange(cell);
    }

    void clearLiquid(LiquidCellStruct cell) {
        cell.getSubChunkInstance().setBlock(cell.getPackedXYZ(), airBlockID);
        commitChange(cell);
    }

    void setPermanent(LiquidCellStruct cell, boolean permanent) {
        cell.getSubChunkInstance().setLiquidPermanent(cell.getPackedXYZ(), permanent);
    }

    void deactivate(LiquidCellStruct cell) {
        cell.getSubChunkInstance().deactivateLiquid(cell.getPackedXYZ());
    }

    void writeTidalLiquid(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int packedXYZ,
            short liquidBlockID,
            short level) {

        subChunkInstance.writeTidalLiquid(packedXYZ, liquidBlockID, level);
        markTouched(chunkInstance, subChunkInstance);
    }

    void clearTidalLiquid(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, int packedXYZ) {
        subChunkInstance.setBlock(packedXYZ, airBlockID);
        markTouched(chunkInstance, subChunkInstance);
    }

    private void commitChange(LiquidCellStruct cell) {
        markTouched(cell.getChunkInstance(), cell.getSubChunkInstance());
        wakeNeighbors(cell);
    }

    // Wake \\

    public void wakeLiquid(ChunkInstance chunkInstance, int subChunkY, int packedXYZ) {

        SubChunkInstance subChunkInstance = chunkInstance.getSubChunk(subChunkY);

        editCell.set(chunkInstance, subChunkInstance, packedXYZ);
        subChunkInstance.activateLiquid(packedXYZ);
        wakeNeighbors(editCell);
    }

    private void wakeNeighbors(LiquidCellStruct cell) {

        for (int d = 0; d < WAKE_DIRECTIONS.length; d++) {

            if (!resolveNeighbor(cell, WAKE_DIRECTIONS[d], wakeCell))
                continue;

            if (!isLiquid(getBlock(wakeCell)))
                continue;

            if (wakeCell.getChunkInstance() == cell.getChunkInstance() || claimChunk(wakeCell.getChunkInstance()))
                wakeCell.getSubChunkInstance().activateLiquid(wakeCell.getPackedXYZ());
            else
                deferWake(wakeCell);
        }
    }

    private void deferWake(LiquidCellStruct cell) {

        ChunkInstance chunkInstance = cell.getChunkInstance();
        int subChunkY = (int) cell.getSubChunkInstance().getCoordinate();

        deferredWakeChunks.add(chunkInstance);
        deferredWakeChunkCoordinates.add(chunkInstance.getCoordinate());
        deferredWakeSubChunkY.add(subChunkY);
        deferredWakePackedXYZ.add(cell.getPackedXYZ());
    }

    public void retryDeferredWakes() {

        int kept = 0;

        for (int i = 0; i < deferredWakeChunks.size(); i++) {

            ChunkInstance chunkInstance = deferredWakeChunks.get(i);
            long chunkCoordinate = deferredWakeChunkCoordinates.getLong(i);

            if (worldStreamManager.getChunkInstance(chunkCoordinate) != chunkInstance)
                continue;

            ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

            if (!syncContainer.tryAcquire()) {
                deferredWakeChunks.set(kept, chunkInstance);
                deferredWakeChunkCoordinates.set(kept, chunkCoordinate);
                deferredWakeSubChunkY.set(kept, deferredWakeSubChunkY.getInt(i));
                deferredWakePackedXYZ.set(kept, deferredWakePackedXYZ.getInt(i));
                kept++;
                continue;
            }

            try {
                chunkInstance.getSubChunk(deferredWakeSubChunkY.getInt(i))
                        .activateLiquid(deferredWakePackedXYZ.getInt(i));
            } finally {
                syncContainer.release();
            }
        }

        deferredWakeChunks.size(kept);
        deferredWakeChunkCoordinates.size(kept);
        deferredWakeSubChunkY.size(kept);
        deferredWakePackedXYZ.size(kept);
    }

    // Accessible \\

    public ObjectArrayList<ChunkInstance> getTouchedChunks() {
        return touchedChunks;
    }

    public IntArrayList getTouchedSubChunkY() {
        return touchedSubChunkY;
    }
}
