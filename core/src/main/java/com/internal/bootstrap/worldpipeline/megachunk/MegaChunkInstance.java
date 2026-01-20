package com.internal.bootstrap.worldpipeline.megachunk;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicPacketInstance;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.InstancePackage;

public class MegaChunkInstance extends InstancePackage {

    // Internal
    private WorldHandle worldHandle;
    private long megaChunkCoordinate;

    // Dynamic Mesh
    private DynamicPacketInstance dynamicPacketInstance;

    public void constructor(
            WorldHandle worldHandle,
            long megaChunkCoordinate,
            VAOHandle vaoHandle) {

        // Internal
        this.worldHandle = worldHandle;
        this.megaChunkCoordinate = megaChunkCoordinate;

        // Dynamic Mesh
        this.dynamicPacketInstance.constructor(vaoHandle);
    }

    // Accessible \\

    // Internal
    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public long getMegaChunkCoordinate() {
        return megaChunkCoordinate;
    }

    // Dynamic Mesh
    public DynamicPacketInstance getDynamicPacketInstance() {
        return dynamicPacketInstance;
    }
}
