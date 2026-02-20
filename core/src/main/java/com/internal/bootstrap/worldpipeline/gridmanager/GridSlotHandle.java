package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.core.engine.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class GridSlotHandle extends HandlePackage {

    // Internal
    private long gridCoordinate;
    private UBOHandle slotUBO;
    private GridSlotDetailLevel detailLevel;

    // Culling values from the center of the individual 1x1 chunk footprint
    private float chunkDistanceFromCenter;
    private float chunkAngleFromCenter;

    // Culling values from the center of the full NxN mega footprint
    private float megaDistanceFromCenter;
    private float megaAngleFromCenter;

    // Mega coverage
    private long chunkCoordinate;
    private long megaCoordinate;
    private ObjectArrayList<GridSlotHandle> coveredSlots;

    // Internal \\

    void constructor(
            long gridCoordinate,
            UBOHandle slotUBO,
            float chunkDistanceFromCenter,
            float chunkAngleFromCenter,
            float megaDistanceFromCenter,
            float megaAngleFromCenter,
            GridSlotDetailLevel detailLevel) {

        this.gridCoordinate = gridCoordinate;
        this.slotUBO = slotUBO;
        this.chunkDistanceFromCenter = chunkDistanceFromCenter;
        this.chunkAngleFromCenter = chunkAngleFromCenter;
        this.megaDistanceFromCenter = megaDistanceFromCenter;
        this.megaAngleFromCenter = megaAngleFromCenter;
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

    public float getChunkDistanceFromCenter() {
        return chunkDistanceFromCenter;
    }

    public float getChunkAngleFromCenter() {
        return chunkAngleFromCenter;
    }

    public float getMegaDistanceFromCenter() {
        return megaDistanceFromCenter;
    }

    public float getMegaAngleFromCenter() {
        return megaAngleFromCenter;
    }

    public GridSlotDetailLevel getDetailLevel() {
        return detailLevel;
    }

    public ObjectArrayList<GridSlotHandle> getCoveredSlots() {
        return coveredSlots;
    }
}