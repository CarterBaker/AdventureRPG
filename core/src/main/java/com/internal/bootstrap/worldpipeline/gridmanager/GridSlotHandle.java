package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.core.engine.HandlePackage;

public class GridSlotHandle extends HandlePackage {

    // Internal
    private long gridCoordinate;
    private long chunkCoordinate;
    private long megaChunkCoordinate;
    private UBOHandle slotUBO;

    // Internal \\

    void constructor(
            long gridCoordinate,
            UBOHandle slotUBO) {

        // Internal
        this.gridCoordinate = gridCoordinate;
        this.chunkCoordinate = gridCoordinate;
        this.megaChunkCoordinate = gridCoordinate;
        this.slotUBO = slotUBO;
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

    public long getMegaChunkCoordinate() {
        return megaChunkCoordinate;
    }

    public void setMegaChunkCoordinate(long megaChunkCoordinate) {
        this.megaChunkCoordinate = megaChunkCoordinate;
    }

    public UBOHandle getSlotUBO() {
        return slotUBO;
    }
}
