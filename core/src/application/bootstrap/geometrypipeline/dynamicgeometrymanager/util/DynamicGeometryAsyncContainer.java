package application.bootstrap.geometrypipeline.dynamicgeometrymanager.util;

import java.util.BitSet;

import application.bootstrap.worldpipeline.util.ChunkCoordinateUtility;
import engine.graphics.color.Color;
import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Direction3Vector;

public class DynamicGeometryAsyncContainer extends AsyncContainerPackage {

    /*
     * Reusable scratch container for one geometry build pass. Holds the
     * directional greedy-mesh bitsets at block resolution and at sub-block
     * resolution, and a per-vertex color accumulator; records go straight
     * into the subchunk's packet. Reset between passes via reset() — no
     * allocations at runtime.
     */

    // Internal
    private BitSet[] directionalBatches;
    private BitSet batchReturn;
    private BitSet[] subDirectionalBatches;
    private BitSet subBatchReturn;
    private Color vertColorAccumulator;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.directionalBatches = new BitSet[Direction3Vector.LENGTH];
        for (int i = 0; i < Direction3Vector.LENGTH; i++)
            directionalBatches[i] = new BitSet();
        this.batchReturn = new BitSet();

        int subCellCount = ChunkCoordinateUtility.BLOCK_COORDINATE_COUNT * EngineSetting.SUB_BLOCK_OCTANT_COUNT;

        this.subDirectionalBatches = new BitSet[Direction3Vector.LENGTH];
        for (int i = 0; i < Direction3Vector.LENGTH; i++)
            subDirectionalBatches[i] = new BitSet(subCellCount);
        this.subBatchReturn = new BitSet(subCellCount);
        this.vertColorAccumulator = new Color();
    }

    // Reset \\

    @Override
    public void reset() {

        for (int i = 0; i < Direction3Vector.LENGTH; i++) {
            directionalBatches[i].clear();
            subDirectionalBatches[i].clear();
        }

        batchReturn.clear();
        subBatchReturn.clear();
    }

    // Accessible \\

    public BitSet[] getDirectionalBatches() {
        return directionalBatches;
    }

    public BitSet getBatchReturn() {
        return batchReturn;
    }

    public BitSet[] getSubDirectionalBatches() {
        return subDirectionalBatches;
    }

    public BitSet getSubBatchReturn() {
        return subBatchReturn;
    }

    public Color getVertColorAccumulator() {
        return vertColorAccumulator;
    }
}