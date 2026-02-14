package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.core.engine.HandlePackage;

public class GridSlotHandle extends HandlePackage {

    // Internal
    private long gridCoordinate;
    private UBOHandle slotUBO;
    private float distanceFromCenter;
    private DataManagement dataManagement;
    private RenderingMode renderingMode;

    // Internal \\

    void constructor(
            long gridCoordinate,
            UBOHandle slotUBO,
            float distanceFromCenter,
            DataManagement dataManagement,
            RenderingMode renderingMode) {

        // Internal
        this.gridCoordinate = gridCoordinate;
        this.slotUBO = slotUBO;
        this.distanceFromCenter = distanceFromCenter;
        this.dataManagement = dataManagement;
        this.renderingMode = renderingMode;
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

    public DataManagement getDataManagement() {
        return dataManagement;
    }

    public RenderingMode getRenderingMode() {
        return renderingMode;
    }
}