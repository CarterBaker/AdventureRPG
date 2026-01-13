package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.core.engine.HandlePackage;

public class GridSlotHandle extends HandlePackage {

    // Internal
    private long gridCoordinate;
    private long chunkCoordinate;

    // Internal \\

    void constructor(long gridCoordinate) {

        // Internal
        this.gridCoordinate = gridCoordinate;
        this.chunkCoordinate = gridCoordinate;
    }

    // Accessible \\

    public long getGridCoordinate() {
        return gridCoordinate;
    }

    public long getChunkCoordinate() {
        return chunkCoordinate;
    }

    public void setChunkCoordinate(long chunkCoordinate) {
        this.chunkCoordinate = chunkCoordinate;
    }
}
