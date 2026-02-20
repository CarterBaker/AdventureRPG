package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.core.engine.DataPackage;

public class GridSlotData extends DataPackage {

    // Internal
    private long gridCoordinate;
    private float distanceFromCenter;
    private float angleRadians;

    // Internal \\

    void constructor(
            long gridCoordinate,
            float distanceFromCenter,
            float angleRadians) {

        this.gridCoordinate = gridCoordinate;
        this.distanceFromCenter = distanceFromCenter;
        this.angleRadians = angleRadians;
    }

    // Accessible \\

    public long getGridCoordinate() {
        return gridCoordinate;
    }

    // Corner-based distance — used only for load order sorting
    public float getDistanceFromCenter() {
        return distanceFromCenter;
    }

    public float getAngleRadians() {
        return angleRadians;
    }
}