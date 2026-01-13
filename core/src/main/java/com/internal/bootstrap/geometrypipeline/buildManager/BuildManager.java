package com.internal.bootstrap.geometrypipeline.buildManager;

import com.internal.bootstrap.geometrypipeline.buildManager.chunkbuildbranch.ChunkBuildBranch;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.core.engine.ManagerPackage;

public class BuildManager extends ManagerPackage {

    // Internal
    private ChunkBuildBranch subChunkBuilderBranch;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.subChunkBuilderBranch = create(ChunkBuildBranch.class);
    }

    public boolean build(SubChunkInstance subChunkInstance) {
        subChunkBuilderBranch.build();
        return true;
    }
}
