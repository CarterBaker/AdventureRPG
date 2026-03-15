package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.bootstrap.shaderpipeline.ubo.UBOInstance;
import com.internal.core.engine.HandlePackage;
import com.internal.core.util.mathematics.extras.Coordinate2Long;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class GridSlotHandle extends HandlePackage {

    // Internal
    private long gridCoordinate;
    private UBOInstance slotUBO;
    private GridSlotDetailLevel detailLevel;
    private GridInstance gridInstance;

    // Culling values from the center of the individual 1x1 chunk footprint
    private float chunkDistanceFromCenter;
    private float chunkAngleFromCenter;

    // Culling values from the center of the full NxN mega footprint
    private float megaDistanceFromCenter;
    private float megaAngleFromCenter;

    // Mega coverage
    private ObjectArrayList<GridSlotHandle> coveredSlots;

    // Internal \\

    void constructor(
            long gridCoordinate,
            UBOInstance slotUBO,
            float chunkDistanceFromCenter,
            float chunkAngleFromCenter,
            float megaDistanceFromCenter,
            float megaAngleFromCenter,
            GridSlotDetailLevel detailLevel,
            GridInstance gridInstance) {
        this.gridCoordinate = gridCoordinate;
        this.slotUBO = slotUBO;
        this.chunkDistanceFromCenter = chunkDistanceFromCenter;
        this.chunkAngleFromCenter = chunkAngleFromCenter;
        this.megaDistanceFromCenter = megaDistanceFromCenter;
        this.megaAngleFromCenter = megaAngleFromCenter;
        this.detailLevel = detailLevel;
        this.gridInstance = gridInstance;
        this.coveredSlots = new ObjectArrayList<>();
    }

    // Computed Coordinate Lookups \\

    public long getChunkCoordinate() {
        return Coordinate2Long.add(gridInstance.getActiveChunkCoordinate(), gridCoordinate);
    }

    public long getMegaCoordinate() {
        return Coordinate2Long.toMegaChunkCoordinate(getChunkCoordinate());
    }

    // Accessible \\

    public long getGridCoordinate() {
        return gridCoordinate;
    }

    public UBOInstance getSlotUBO() {
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