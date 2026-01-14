package com.internal.bootstrap.worldpipeline.dynamicgeometrymanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;

public class DynamicGeometryManager extends ManagerPackage {

    // Internal
    private InternalBuildManager internalBuildManager;
    private int worldHeight;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.internalBuildManager = create(InternalBuildManager.class);
        this.worldHeight = EngineSetting.WORLD_HEIGHT;
    }

    public boolean build(ChunkInstance chunkInstance) {

        for (int i = 0; i < worldHeight; i++)
            if (!internalBuildManager.build(
                    chunkInstance,
                    chunkInstance.getSubChunk(i)))
                return false;

        return true;
    }
}
