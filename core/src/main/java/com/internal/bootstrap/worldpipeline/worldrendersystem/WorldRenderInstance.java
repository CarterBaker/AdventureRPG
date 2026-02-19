package com.internal.bootstrap.worldpipeline.worldrendersystem;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicPacketInstance;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotHandle;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.InstancePackage;

public class WorldRenderInstance extends InstancePackage {

    // Internal
    protected WorldRenderSystem worldRenderSystem;
    protected WorldHandle worldHandle;
    protected RenderType renderType;
    protected long coordinate;

    // Grid
    protected GridSlotHandle gridSlotHandle;

    // Dynamic Mesh
    protected DynamicPacketInstance dynamicPacketInstance;

    @Override
    protected void create() {
        this.dynamicPacketInstance = create(DynamicPacketInstance.class);
    }

    public void constructor(
            WorldRenderSystem worldRenderSystem,
            WorldHandle worldHandle,
            RenderType renderType,
            long coordinate,
            VAOHandle vaoHandle) {

        this.worldRenderSystem = worldRenderSystem;
        this.worldHandle = worldHandle;
        this.renderType = renderType;
        this.coordinate = coordinate;

        this.dynamicPacketInstance.constructor(vaoHandle);
    }

    public void dispose() {

        if (renderType == RenderType.INDIVIDUAL)
            worldRenderSystem.removeChunkInstance(coordinate);
        else
            worldRenderSystem.removeMegaInstance(coordinate);
    }

    // Accessible \\

    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public long getCoordinate() {
        return coordinate;
    }

    public GridSlotHandle getGridSlotHandle() {
        return gridSlotHandle;
    }

    public void setGridSlotHandle(GridSlotHandle gridSlotHandle) {
        this.gridSlotHandle = gridSlotHandle;
    }

    public DynamicPacketInstance getDynamicPacketInstance() {
        return dynamicPacketInstance;
    }
}