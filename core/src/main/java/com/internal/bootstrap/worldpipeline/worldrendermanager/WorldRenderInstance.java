package com.internal.bootstrap.worldpipeline.worldrendermanager;

import com.internal.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.InstancePackage;

public class WorldRenderInstance extends InstancePackage {

    // Internal
    protected WorldRenderManager worldRenderSystem;
    protected WorldHandle worldHandle;
    protected RenderType renderType;
    protected long coordinate;

    // Dynamic Mesh
    protected DynamicPacketInstance dynamicPacketInstance;

    @Override
    protected void create() {
        this.dynamicPacketInstance = create(DynamicPacketInstance.class);
    }

    public void constructor(
            WorldRenderManager worldRenderSystem,
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

    public DynamicPacketInstance getDynamicPacketInstance() {
        return dynamicPacketInstance;
    }
}