package com.AdventureRPG.bootstrap.worldpipeline.gridmanager;

import com.AdventureRPG.core.engine.InstancePackage;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class GridInstance extends InstancePackage {

    // Internal
    private int totalSlots;
    private long[] loadOrder;
    private LongOpenHashSet gridCoordinates;
    private Long2ObjectOpenHashMap<GridSlotHandle> gridSlots;

    // Internal \\

    protected void constructor(
            int totalSlots,
            long[] loadOrder,
            LongOpenHashSet gridCoordinates,
            Long2ObjectOpenHashMap<GridSlotHandle> gridSlots) {

        // Internal
        this.totalSlots = totalSlots;
        this.loadOrder = loadOrder;
        this.gridCoordinates = gridCoordinates;
        this.gridSlots = gridSlots;
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

    public void assignChunkToSlot(long gridCoordinate, long chunkCoordinate) {
        getGridSlot(gridCoordinate).setChunkCoordinate(chunkCoordinate);
    }
}
