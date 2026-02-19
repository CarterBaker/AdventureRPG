package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.core.engine.HandlePackage;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class GridSlotHandle extends HandlePackage {

    // Internal
    private long gridCoordinate;
    private UBOHandle slotUBO;
    private float distanceFromCenter;
    private float angleFromCenter;
    private GridSlotDetailLevel detailLevel;

    // Mega coverage
    private long chunkCoordinate, megaCoordinate;
    private ObjectArrayList<GridSlotHandle> coveredSlots;

    // Internal \\

    void constructor(
            long gridCoordinate,
            UBOHandle slotUBO,
            float distanceFromCenter,
            float angleFromCenter,
            GridSlotDetailLevel detailLevel) {

        this.gridCoordinate = gridCoordinate;
        this.slotUBO = slotUBO;
        this.distanceFromCenter = distanceFromCenter;
        this.angleFromCenter = angleFromCenter;
        this.detailLevel = detailLevel;
        this.coveredSlots = new ObjectArrayList<>();
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

    public long getMegaCoordinate() {
        return megaCoordinate;
    }

    public void setMegaCoordinate(long megaCoordinate) {
        this.megaCoordinate = megaCoordinate;
    }

    public UBOHandle getSlotUBO() {
        return slotUBO;
    }

    public float getDistanceFromCenter() {
        return distanceFromCenter;
    }

    public float getAngleFromCenter() {
        return angleFromCenter;
    }

    public GridSlotDetailLevel getDetailLevel() {
        return detailLevel;
    }

    public ObjectArrayList<GridSlotHandle> getCoveredSlots() {
        return coveredSlots;
    }
}