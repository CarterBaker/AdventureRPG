package com.AdventureRPG.core.geometrypipeline.ibomanager;

import com.AdventureRPG.core.engine.HandlePackage;

public class IBOHandle extends HandlePackage {

    // Internal
    private int indexHandle;
    private int indexCount;

    // Internal \\

    public void constructor(
            int indexHandle,
            int indexCount) {

        // Internal
        this.indexHandle = indexHandle;
        this.indexCount = indexCount;
    }

    // Accessible \\

    public int getIndexHandle() {
        return indexHandle;
    }

    public int getIndexCount() {
        return indexCount;
    }
}
