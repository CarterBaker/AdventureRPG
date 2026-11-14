package application.bootstrap.worldpipeline.util;

import engine.root.StructPackage;

public class LatticePointStruct extends StructPackage {

    /*
     * The cell of a channel-interleaved lattice that one point falls in,
     * resolved once by TerrainCarveUtility.resolveLatticePoint(): the index of
     * the cell's first corner, the strides to its other seven corners and the
     * point's weights across the cell, so every channel read at the point
     * interpolates from the same corners with no index arithmetic of its own.
     */

    // Corners
    int base;
    int strideX;
    int strideY;
    int strideZ;

    // Weights
    float weightX;
    float weightY;
    float weightZ;
}
