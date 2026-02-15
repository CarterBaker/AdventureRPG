package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.core.engine.HandlePackage;

public class GridSlotHandle extends HandlePackage {

    // Internal
    private long gridCoordinate;
    private UBOHandle slotUBO;
    private float distanceFromCenter;
    private float normalizedDistance;
    private GridSlotDetailLevel detailLevel;

    // Internal \\

    void constructor(
            long gridCoordinate,
            UBOHandle slotUBO,
            float distanceFromCenter,
            float normalizedDistance,
            GridSlotDetailLevel detailLevel) {

        // Internal
        this.gridCoordinate = gridCoordinate;
        this.slotUBO = slotUBO;
        this.distanceFromCenter = distanceFromCenter;
        this.normalizedDistance = normalizedDistance;
        this.detailLevel = detailLevel;
    }

    // Accessible \\

    public long getGridCoordinate() {
        return gridCoordinate;
    }

    public UBOHandle getSlotUBO() {
        return slotUBO;
    }

    public float getDistanceFromCenter() {
        return distanceFromCenter;
    }

    public float getNormalizedDistance() {
        return normalizedDistance;
    }

    public GridSlotDetailLevel getDetailLevel() {
        return detailLevel;
    }
}