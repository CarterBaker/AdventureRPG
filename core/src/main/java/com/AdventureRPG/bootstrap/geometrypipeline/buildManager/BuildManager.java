package com.AdventureRPG.bootstrap.geometrypipeline.buildManager;

import com.AdventureRPG.bootstrap.geometrypipeline.buildManager.chunkbuildbranch.ChunkBuildBranch;
import com.AdventureRPG.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.AdventureRPG.core.engine.ManagerPackage;

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
