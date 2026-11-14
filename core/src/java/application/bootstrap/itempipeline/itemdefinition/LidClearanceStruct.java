package application.bootstrap.itempipeline.itemdefinition;

import engine.root.StructPackage;

public class LidClearanceStruct extends StructPackage {

    /*
     * The model-grid cells a container's lid needs clear before it opens, and
     * keeps claimed while it stands open so nothing is set where the lid will
     * close. Cells may lie up to one block outside the model grid, the way a
     * lid swings past the box it closes. The mask over the cells' bounds
     * answers in one read whether a cell belongs to the clearance.
     */

    // Cells — model-grid coordinates of every clearance cell
    private final int[] cellX;
    private final int[] cellY;
    private final int[] cellZ;

    // Bounds — inclusive, model-grid coordinates
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    // Mask — one bit per cell of the bounds, set where the clearance holds it
    private final long[] mask;

    // Constructor \\

    public LidClearanceStruct(int[] cellX, int[] cellY, int[] cellZ) {

        int lowX = Integer.MAX_VALUE, lowY = Integer.MAX_VALUE, lowZ = Integer.MAX_VALUE;
        int highX = Integer.MIN_VALUE, highY = Integer.MIN_VALUE, highZ = Integer.MIN_VALUE;

        for (int i = 0; i < cellX.length; i++) {
            lowX = Math.min(lowX, cellX[i]);
            lowY = Math.min(lowY, cellY[i]);
            lowZ = Math.min(lowZ, cellZ[i]);
            highX = Math.max(highX, cellX[i]);
            highY = Math.max(highY, cellY[i]);
            highZ = Math.max(highZ, cellZ[i]);
        }

        // Cells
        this.cellX = cellX;
        this.cellY = cellY;
        this.cellZ = cellZ;

        // Bounds
        this.minX = cellX.length == 0 ? 0 : lowX;
        this.minY = cellX.length == 0 ? 0 : lowY;
        this.minZ = cellX.length == 0 ? 0 : lowZ;
        this.maxX = cellX.length == 0 ? -1 : highX;
        this.maxY = cellX.length == 0 ? -1 : highY;
        this.maxZ = cellX.length == 0 ? -1 : highZ;

        // Mask
        this.mask = new long[(countBoundsCells() + Long.SIZE - 1) / Long.SIZE];

        for (int i = 0; i < cellX.length; i++) {
            int index = toMaskIndex(cellX[i], cellY[i], cellZ[i]);
            mask[index / Long.SIZE] |= 1L << (index % Long.SIZE);
        }
    }

    private int countBoundsCells() {
        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    private int toMaskIndex(int x, int y, int z) {
        return (x - minX) + (maxX - minX + 1) * ((y - minY) + (maxY - minY + 1) * (z - minZ));
    }

    // Mask \\

    public boolean holdsCell(int x, int y, int z) {

        if (x < minX || y < minY || z < minZ || x > maxX || y > maxY || z > maxZ)
            return false;

        int index = toMaskIndex(x, y, z);

        return (mask[index / Long.SIZE] & (1L << (index % Long.SIZE))) != 0;
    }

    // Accessible \\

    public boolean isEmpty() {
        return cellX.length == 0;
    }

    public int getCellCount() {
        return cellX.length;
    }

    public int getCellX(int cellIndex) {
        return cellX[cellIndex];
    }

    public int getCellY(int cellIndex) {
        return cellY[cellIndex];
    }

    public int getCellZ(int cellIndex) {
        return cellZ[cellIndex];
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }
}
