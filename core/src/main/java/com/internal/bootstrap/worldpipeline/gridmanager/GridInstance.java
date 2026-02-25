package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.bootstrap.worldpipeline.util.WorldWrapUtility;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.InstancePackage;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class GridInstance extends InstancePackage {

    // Internal
    private int totalSlots;
    private long[] loadOrder;
    private LongOpenHashSet gridCoordinates;
    private Long2ObjectOpenHashMap<GridSlotHandle> gridSlots;
    private float radiusSquared;

    // Active State
    private long activeChunkCoordinate;
    private WorldHandle worldHandle;

    // Internal scan cursor — cycles through loadOrder continuously
    private int scanCursor;

    // Internal \\

    protected void constructor(
            int totalSlots,
            long[] loadOrder,
            LongOpenHashSet gridCoordinates,
            Long2ObjectOpenHashMap<GridSlotHandle> gridSlots,
            float radiusSquared) {

        this.totalSlots = totalSlots;
        this.loadOrder = loadOrder;
        this.gridCoordinates = gridCoordinates;
        this.gridSlots = gridSlots;
        this.radiusSquared = radiusSquared;
        this.activeChunkCoordinate = Coordinate2Long.pack(0, 0);
        this.scanCursor = 0;
    }

    // Active State \\

    public void setWorldHandle(WorldHandle worldHandle) {
        this.worldHandle = worldHandle;
    }

    public void setActiveChunkCoordinate(long activeChunkCoordinate) {
        this.activeChunkCoordinate = activeChunkCoordinate;
    }

    public long getActiveChunkCoordinate() {
        return activeChunkCoordinate;
    }

    // Scan Iteration \\

    // Returns the next GridSlotHandle in load-order, cycling back to the front
    public GridSlotHandle getNextScanSlot() {

        if (scanCursor >= totalSlots)
            scanCursor = 0;

        long gridCoordinate = loadOrder[scanCursor];
        scanCursor++;

        return gridSlots.get(gridCoordinate);
    }

    // Computed Slot Lookups \\

    // Returns the world chunk coordinate this grid slot currently maps to
    public long getChunkCoordinateForSlot(long gridCoordinate) {
        long raw = Coordinate2Long.add(activeChunkCoordinate, gridCoordinate);
        return WorldWrapUtility.wrapAroundWorld(worldHandle, raw);
    }

    // Returns the mega coordinate for the chunk this grid slot currently maps to
    public long getMegaCoordinateForSlot(long gridCoordinate) {
        return Coordinate2Long.toMegaChunkCoordinate(getChunkCoordinateForSlot(gridCoordinate));
    }

    // Given a world chunk coordinate, returns the GridSlotHandle it maps to,
    // or null if the coordinate is not currently within the grid
    public GridSlotHandle getGridSlotForChunk(long chunkCoordinate) {
        long gridCoordinate = Coordinate2Long.subtract(chunkCoordinate, activeChunkCoordinate);
        return gridSlots.get(gridCoordinate);
    }

    // Accessible \\

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
}