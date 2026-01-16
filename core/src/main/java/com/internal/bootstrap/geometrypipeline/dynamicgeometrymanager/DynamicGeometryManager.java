package com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;

public class DynamicGeometryManager extends ManagerPackage {

    // Internal
    private DynamicGeometryAsyncInstance dynamicGeometryAsyncInstance;
    private InternalBuildManager internalBuildManager;
    private int worldHeight;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.dynamicGeometryAsyncInstance = create(DynamicGeometryAsyncInstance.class);
        this.internalBuildManager = create(InternalBuildManager.class);
        this.worldHeight = EngineSetting.WORLD_HEIGHT;
    }

    public boolean build(
            DynamicGeometryAsyncInstance dynamicGeometryAsyncInstance,
            ChunkInstance chunkInstance) {

        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();

        for (int i = 0; i < worldHeight; i++)
            if (!internalBuildManager.build(
                    dynamicGeometryAsyncInstance,
                    chunkInstance,
                    subChunks[i]))
                return false;

        return true;
    }

    // Accessible \\

    public DynamicGeometryAsyncInstance getDynamicGeometryAsyncInstance() {
        return dynamicGeometryAsyncInstance;
    }
}
