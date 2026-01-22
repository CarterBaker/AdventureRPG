package com.internal.bootstrap.worldpipeline.worldrendersystem;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicPacketInstance;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotHandle;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.InstancePackage;

public class WorldRenderInstance extends InstancePackage {

    // Internal
    protected WorldRenderSystem worldrendersystem;
    protected WorldHandle worldHandle;
    protected long coordinate;

    // grid
    protected GridSlotHandle gridSlotHandle;

    // Dynamic Mesh
    protected DynamicPacketInstance dynamicPacketInstance;

    @Override
    protected void create() {

        // Dynamic Mesh
        this.dynamicPacketInstance = create(DynamicPacketInstance.class);
    }

    public void constructor(
            WorldRenderSystem worldRenderSystem,
            WorldHandle worldHandle,
            long coordinate,
            VAOHandle vaoHandle) {

        // Internal
        this.worldrendersystem = worldRenderSystem;
        this.worldHandle = worldHandle;
        this.coordinate = coordinate;

        // Dynamic Mesh
        this.dynamicPacketInstance.constructor(vaoHandle);
    }

    public void dispose() {
        worldrendersystem.removeWorldInstance(coordinate);
    }

    // Accessible \\

    // Internal
    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public long getCoordinate() {
        return coordinate;
    }

    // Grid
    public GridSlotHandle getGridSlotHandle() {
        return gridSlotHandle;
    }

    public void setGridSlotHandle(GridSlotHandle gridSlotHandle) {
        this.gridSlotHandle = gridSlotHandle;
    }

    // Dynamic Mesh
    public DynamicPacketInstance getDynamicPacketInstance() {
        return dynamicPacketInstance;
    }
}
