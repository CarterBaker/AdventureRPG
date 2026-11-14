package application.bootstrap.geometrypipeline.dynamicgeometrymanager.util;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.block.BlockHandle;
import engine.graphics.color.Color;
import engine.root.StructPackage;

public class SubCellGridStruct extends StructPackage {

    /*
     * One subchunk and the one-cell shell around it, flattened for a geometry
     * build: per cell its block, the block's geometry, its sub-block mask and
     * its biome's color in struct-of-arrays order, Y-Z-X like the palettes,
     * from cell -1 to the chunk size on every axis. A cell in an unloaded
     * neighbor holds no block, no geometry, a whole mask and no biome; a cell
     * outside the world holds air, a whole mask and no biome. SubCellSampleBranch
     * fills it once per subchunk, so every sample a build takes is one array
     * read and never a walk through a handle.
     */

    // Layout
    private final int strideZ;
    private final int strideY;
    private final int origin;

    // Cells
    private final BlockHandle[] cellBlockHandles;
    private final DynamicGeometryType[] cellGeometries;
    private final int[] cellMasks;

    // Biome Colors
    private final boolean[] cellHasBiome;
    private final float[] cellBiomeRed;
    private final float[] cellBiomeGreen;
    private final float[] cellBiomeBlue;

    // Constructor \\

    public SubCellGridStruct(int chunkSize) {

        int side = chunkSize + 2;
        int cellCount = side * side * side;

        // Layout
        this.strideZ = side;
        this.strideY = side * side;
        this.origin = strideY + strideZ + 1;

        // Cells
        this.cellBlockHandles = new BlockHandle[cellCount];
        this.cellGeometries = new DynamicGeometryType[cellCount];
        this.cellMasks = new int[cellCount];

        // Biome Colors
        this.cellHasBiome = new boolean[cellCount];
        this.cellBiomeRed = new float[cellCount];
        this.cellBiomeGreen = new float[cellCount];
        this.cellBiomeBlue = new float[cellCount];
    }

    // Management \\

    public void setCell(int index, BlockHandle blockHandle, int mask) {
        cellBlockHandles[index] = blockHandle;
        cellGeometries[index] = blockHandle != null ? blockHandle.getGeometry() : null;
        cellMasks[index] = mask;
    }

    public void setBiomeColor(int index, Color biomeColor) {
        cellHasBiome[index] = true;
        cellBiomeRed[index] = biomeColor.r;
        cellBiomeGreen[index] = biomeColor.g;
        cellBiomeBlue[index] = biomeColor.b;
    }

    public void clearBiomeColor(int index) {
        cellHasBiome[index] = false;
    }

    // Accessible \\

    public int toIndex(int cellX, int cellY, int cellZ) {
        return origin + cellY * strideY + cellZ * strideZ + cellX;
    }

    // How far one index lies from another along a step of whole cells
    public int toOffset(int stepX, int stepY, int stepZ) {
        return stepY * strideY + stepZ * strideZ + stepX;
    }

    public BlockHandle getBlockHandle(int index) {
        return cellBlockHandles[index];
    }

    public DynamicGeometryType getGeometry(int index) {
        return cellGeometries[index];
    }

    public int getMask(int index) {
        return cellMasks[index];
    }

    public boolean hasBiome(int index) {
        return cellHasBiome[index];
    }

    public float getBiomeRed(int index) {
        return cellBiomeRed[index];
    }

    public float getBiomeGreen(int index) {
        return cellBiomeGreen[index];
    }

    public float getBiomeBlue(int index) {
        return cellBiomeBlue[index];
    }
}
