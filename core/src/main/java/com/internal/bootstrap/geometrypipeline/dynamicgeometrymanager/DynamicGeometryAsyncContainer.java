package com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager;

import java.util.BitSet;

import com.internal.core.engine.AsyncContainerPackage;
import com.internal.core.util.mathematics.Extras.Direction3Vector;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class DynamicGeometryAsyncContainer extends AsyncContainerPackage {

    // Internal
    private FloatArrayList quads;
    private BitSet[] directionalBatches;
    private BitSet batchReturn;

    // Internal \\

    protected void create() {

        // Internal
        this.quads = new FloatArrayList();
        this.directionalBatches = new BitSet[Direction3Vector.LENGTH];
        for (int i = 0; i < Direction3Vector.LENGTH; i++)
            directionalBatches[i] = new BitSet();
        this.batchReturn = new BitSet();

    }

    // Reset \\

    @Override
    public void reset() {

        // Internal
        quads.clear();
        for (int i = 0; i < Direction3Vector.LENGTH; i++)
            directionalBatches[i].clear();
        batchReturn.clear();
    }

    // Accessible \\

    public FloatArrayList getQuads() {
        return quads;
    }

    public BitSet[] getDirectionalBatches() {
        return directionalBatches;
    }

    public BitSet getBatchReturn() {
        return batchReturn;
    }
}
