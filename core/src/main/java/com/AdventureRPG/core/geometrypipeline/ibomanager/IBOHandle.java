package com.AdventureRPG.core.geometrypipeline.ibomanager;

import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class IBOHandle {

    // Internal
    public final ShortArrayList indices;
    public final int indexCount;

    IBOHandle(
            ShortArrayList indices,
            int indexCount) {

        // Internal
        this.indices = indices;
        this.indexCount = indexCount;
    }
}
