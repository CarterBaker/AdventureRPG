package com.AdventureRPG.bootstrap.geometrypipeline.buildManager;

import com.AdventureRPG.bootstrap.geometrypipeline.buildManager.subchunkbuilderbranch.SubChunkBuilderBranch;
import com.AdventureRPG.core.engine.ManagerPackage;

public class BuildManager extends ManagerPackage {

    // Internal
    private SubChunkBuilderBranch subChunkBuilderBranch;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.subChunkBuilderBranch = create(SubChunkBuilderBranch.class);
    }

    public void build() {
        subChunkBuilderBranch.build();
    }
}
