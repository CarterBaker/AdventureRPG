package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.core.engine.HandlePackage;

public class GridSlotHandle extends HandlePackage {

    // Internal
    private long gridCoordinate;
    private UBOHandle slotUBO;

    // Internal \\

    void constructor(
            long gridCoordinate,
            UBOHandle slotUBO) {

        // Internal
        this.gridCoordinate = gridCoordinate;
        this.slotUBO = slotUBO;
    }

    // Accessible \\

    public long getGridCoordinate() {
        return gridCoordinate;
    }

    public UBOHandle getSlotUBO() {
        return slotUBO;
    }
}
