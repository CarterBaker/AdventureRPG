package com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.util;

import java.util.BitSet;

import com.internal.core.engine.AsyncContainerPackage;
import com.internal.core.util.mathematics.Extras.Color;
import com.internal.core.util.mathematics.Extras.Direction3Vector;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class DynamicGeometryAsyncContainer extends AsyncContainerPackage {

    // Internal
    private Int2ObjectOpenHashMap<FloatArrayList> verts;
    private BitSet[] directionalBatches;
    private BitSet batchReturn;

    private Color[] vertColors;

    // Internal \\

    protected void create() {

        // Internal
        this.verts = new Int2ObjectOpenHashMap<>();
        this.directionalBatches = new BitSet[Direction3Vector.LENGTH];
        for (int i = 0; i < Direction3Vector.LENGTH; i++)
            directionalBatches[i] = new BitSet();
        this.batchReturn = new BitSet();

        this.vertColors = new Color[VertBlockNeighbor3Vector.LENGTH];
    }

    // Reset \\

    @Override
    public void reset() {

        // Internal
        for (FloatArrayList buffer : verts.values())
            buffer.clear();

        for (int i = 0; i < Direction3Vector.LENGTH; i++)
            directionalBatches[i].clear();

        batchReturn.clear();
    }

    // Accessible \\

    public Int2ObjectOpenHashMap<FloatArrayList> getVerts() {
        return verts;
    }

    public BitSet[] getDirectionalBatches() {
        return directionalBatches;
    }

    public BitSet getBatchReturn() {
        return batchReturn;
    }

    public Color[] getVertColors() {
        return vertColors;
    }
}
