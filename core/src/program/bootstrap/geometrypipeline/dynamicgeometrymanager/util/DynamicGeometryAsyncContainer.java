package program.bootstrap.geometrypipeline.dynamicgeometrymanager.util;

import java.util.BitSet;
import program.core.engine.AsyncContainerPackage;
import program.core.util.mathematics.extras.Color;
import program.core.util.mathematics.extras.Direction3Vector;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class DynamicGeometryAsyncContainer extends AsyncContainerPackage {

    /*
     * Reusable scratch container for one geometry build pass. Holds per-material
     * vertex buffers, directional greedy-mesh bitsets, and per-vertex color
     * scratch space. Reset between passes via reset() — no allocations at runtime.
     */

    // Internal
    private Int2ObjectOpenHashMap<FloatArrayList> verts;
    private BitSet[] directionalBatches;
    private BitSet batchReturn;
    private Color[] vertColors;

    // Internal \\

    @Override
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