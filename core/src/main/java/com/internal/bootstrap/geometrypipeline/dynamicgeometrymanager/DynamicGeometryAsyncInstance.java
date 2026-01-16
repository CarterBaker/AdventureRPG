package com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager;

import java.util.BitSet;

import com.internal.core.engine.AsyncInstancePackage;
import com.internal.core.util.mathematics.Extras.Direction3Vector;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class DynamicGeometryAsyncInstance extends AsyncInstancePackage {

    // Internal
    private FloatArrayList quads;

    private BitSet batchedBlocks;
    private BitSet[] directionalBatches;

    // Internal \\

    protected void create() {

        // Internal
        this.quads = new FloatArrayList();

        this.batchedBlocks = new BitSet();
        this.directionalBatches = new BitSet[Direction3Vector.LENGTH];
        for (int i = 0; i < Direction3Vector.LENGTH; i++)
            directionalBatches[i] = new BitSet();
    }

    // Reset \\

    @Override
    public void reset() {

        // Internal
        quads.clear();

        batchedBlocks.clear();
        for (int i = 0; i < Direction3Vector.LENGTH; i++)
            directionalBatches[i].clear();
    }

    // Accessible \\

    public FloatArrayList getQuads() {
        return quads;
    }

    public BitSet getBatchedBlocks() {
        return batchedBlocks;
    }

    public BitSet[] getDirectionalBatches() {
        return directionalBatches;
    }
}
