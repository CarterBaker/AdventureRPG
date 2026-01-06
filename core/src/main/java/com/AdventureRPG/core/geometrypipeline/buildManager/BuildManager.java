package com.AdventureRPG.core.geometrypipeline.buildManager;

import com.AdventureRPG.WorldPipeline.subchunks.SubChunk;
import com.AdventureRPG.core.engine.ManagerPackage;
import com.AdventureRPG.core.geometrypipeline.buildManager.subchunkbuilderbranch.SubChunkBuilderBranch;

public class BuildManager extends ManagerPackage {

    // Internal
    private SubChunkBuilderBranch subChunkBuilderBranch;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.subChunkBuilderBranch = create(SubChunkBuilderBranch.class);
    }

    public void build(SubChunk subChunk) {
        subChunkBuilderBranch.build(subChunk);
    }
}
