package com.AdventureRPG.core.geometry.ibomanager;

import com.AdventureRPG.core.engine.HandleFrame;

public class IBOHandle extends HandleFrame {

    // Internal
    public final int indexHandle;
    public final int indexCount;

    public IBOHandle(
            int indexHandle,
            int indexCount) {

        // Internal
        this.indexHandle = indexHandle;
        this.indexCount = indexCount;
    }
}
