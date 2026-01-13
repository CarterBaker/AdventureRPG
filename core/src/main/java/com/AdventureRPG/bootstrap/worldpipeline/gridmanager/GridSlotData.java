package com.AdventureRPG.bootstrap.worldpipeline.gridmanager;

import com.AdventureRPG.core.engine.DataPackage;

public class GridSlotData extends DataPackage {

    // Internal
    private long gridCoordinate;
    private float distanceFromCenter;

    // Internal \\

    void constructor(
            long gridCoordinate,
            float distanceFromCenter) {

        // Internal
        this.gridCoordinate = gridCoordinate;
        this.distanceFromCenter = distanceFromCenter;
    }

    // Accessible \\

    public long getGridCoordinate() {
        return gridCoordinate;
    }

    public float getDistanceFromCenter() {
        return distanceFromCenter;
    }
}
