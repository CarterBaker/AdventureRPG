package com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;

public class DynamicGeometryManager extends ManagerPackage {

    // Internal
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;
    private InternalBuildManager internalBuildManager;
    private int worldHeight;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.dynamicGeometryAsyncContainer = create(DynamicGeometryAsyncContainer.class);
        this.internalBuildManager = create(InternalBuildManager.class);
        this.worldHeight = EngineSetting.WORLD_HEIGHT;
    }

    public boolean build(
            DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer,
            ChunkInstance chunkInstance) {

        boolean success = true;
        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();

        for (int i = 0; i < worldHeight; i++)
            if (!internalBuildManager.build(
                    dynamicGeometryAsyncContainer,
                    chunkInstance,
                    subChunks[i]))
                success = false;

        return success;
    }

    public boolean buildSubChunk(
            DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer,
            ChunkInstance chunkInstance,
            int subChunkIndex) {

        return internalBuildManager.build(
                dynamicGeometryAsyncContainer,
                chunkInstance,
                chunkInstance.getSubChunks()[subChunkIndex]);
    }

    // Accessible \\

    public DynamicGeometryAsyncContainer getDynamicGeometryAsyncInstance() {
        return dynamicGeometryAsyncContainer;
    }
}
