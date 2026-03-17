package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.bootstrap.entitypipeline.entity.EntityInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.util.WorldWrapUtility;
import com.internal.bootstrap.worldpipeline.world.WorldHandle;
import com.internal.core.engine.InstancePackage;
import com.internal.core.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class GridInstance extends InstancePackage {

    /*
     * The active spatial grid for a single focal entity. Owns the load order,
     * slot handles, active chunks, active mega chunks, and pending load and
     * unload requests. The focal entity drives the streaming origin — world
     * handle and chunk coordinate are read directly from it each frame.
     */

    // Focal
    private EntityInstance focalEntity;

    // Grid
    private int totalSlots;
    private long[] loadOrder;
    private LongOpenHashSet gridCoordinates;
    private Long2ObjectOpenHashMap<GridSlotHandle> gridSlots;
    private float radiusSquared;

    // Active State
    private long activeChunkCoordinate;

    // Chunk State
    private Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;
    private Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks;
    private LongLinkedOpenHashSet loadRequests;
    private LongLinkedOpenHashSet unloadRequests;

    // Scan cursor — cycles through loadOrder continuously
    private int scanCursor;

    // Constructor \\

    protected void constructor(
            EntityInstance focalEntity,
            int totalSlots,
            long[] loadOrder,
            LongOpenHashSet gridCoordinates,
            Long2ObjectOpenHashMap<GridSlotHandle> gridSlots,
            float radiusSquared,
            int maxChunks) {

        // Focal
        this.focalEntity = focalEntity;

        // Grid
        this.totalSlots = totalSlots;
        this.loadOrder = loadOrder;
        this.gridCoordinates = gridCoordinates;
        this.gridSlots = gridSlots;
        this.radiusSquared = radiusSquared;

        // Active State
        this.activeChunkCoordinate = Coordinate2Long.pack(-1, -1);

        // Chunk State
        this.activeChunks = new Long2ObjectLinkedOpenHashMap<>(maxChunks);
        this.activeMegaChunks = new Long2ObjectLinkedOpenHashMap<>();
        this.loadRequests = new LongLinkedOpenHashSet();
        this.unloadRequests = new LongLinkedOpenHashSet();

        this.scanCursor = 0;
    }

    // Active State \\

    public void setActiveChunkCoordinate(long activeChunkCoordinate) {
        this.activeChunkCoordinate = activeChunkCoordinate;
    }

    public long getActiveChunkCoordinate() {
        return activeChunkCoordinate;
    }

    // Scan Iteration \\

    public GridSlotHandle getNextScanSlot() {

        if (scanCursor >= totalSlots)
            scanCursor = 0;

        long gridCoordinate = loadOrder[scanCursor];
        scanCursor++;

        return gridSlots.get(gridCoordinate);
    }

    // Computed Slot Lookups \\

    public long getChunkCoordinateForSlot(long gridCoordinate) {
        long raw = Coordinate2Long.add(activeChunkCoordinate, gridCoordinate);
        return WorldWrapUtility.wrapAroundWorld(getWorldHandle(), raw);
    }

    public long getMegaCoordinateForSlot(long gridCoordinate) {
        return Coordinate2Long.toMegaChunkCoordinate(getChunkCoordinateForSlot(gridCoordinate));
    }

    public GridSlotHandle getGridSlotForChunk(long chunkCoordinate) {
        long gridCoordinate = Coordinate2Long.subtract(chunkCoordinate, activeChunkCoordinate);
        return gridSlots.get(gridCoordinate);
    }

    // Accessible \\

    public EntityInstance getFocalEntity() {
        return focalEntity;
    }

    public WorldHandle getWorldHandle() {
        return focalEntity.getWorldHandle();
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public long[] getLoadOrder() {
        return loadOrder;
    }

    public long getGridCoordinate(int i) {
        return loadOrder[i];
    }

    public LongOpenHashSet getGridCoordinates() {
        return gridCoordinates;
    }

    public GridSlotHandle getGridSlot(long gridCoordinate) {
        return gridSlots.get(gridCoordinate);
    }

    public float getRadiusSquared() {
        return radiusSquared;
    }

    public Long2ObjectLinkedOpenHashMap<ChunkInstance> getActiveChunks() {
        return activeChunks;
    }

    public Long2ObjectLinkedOpenHashMap<MegaChunkInstance> getActiveMegaChunks() {
        return activeMegaChunks;
    }

    public LongLinkedOpenHashSet getLoadRequests() {
        return loadRequests;
    }

    public LongLinkedOpenHashSet getUnloadRequests() {
        return unloadRequests;
    }
}