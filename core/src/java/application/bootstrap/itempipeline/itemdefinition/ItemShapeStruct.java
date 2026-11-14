package application.bootstrap.itempipeline.itemdefinition;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.matrices.Matrix4;

public class ItemShapeStruct extends StructPackage {

    /*
     * The sub-voxel cells an item claims, trimmed to their bounds: the volume
     * it takes up inside a container and standing in the world. An item's
     * file defines the cells as boxes in its model grid, which spans one block
     * or several, or leaves them to its model, where a wall claims the cell on
     * its far side, or the last cell where it closes the grid, so a flat item
     * still takes up one layer. The grid mask answers in one read whether a
     * model-grid cell is claimed. A rotation is a number of quarter turns about
     * the vertical axis; rotated cells and the rotated shape transform both
     * keep the shape inside its own rotated bounds, so a placement is always
     * the shape's minimum corner. An item standing in the world turns about
     * the centre of its model grid's first block, so a shape larger than a
     * block reaches blocks either side of the one its corner lies in, and its
     * reach says how many, turned any way at all.
     */

    // Grid Size — the item's model grid in sub-voxel cells
    private final int gridSizeX;
    private final int gridSizeY;
    private final int gridSizeZ;

    // Offset — the trimmed shape's first cell inside the item's model grid
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;

    // Size
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    // Reach — blocks beyond its corner's block the shape can reach, turned any way
    private final int reachBefore;
    private final int reachAfter;

    // Cells — shape-local coordinates of every filled cell
    private final int[] cellX;
    private final int[] cellY;
    private final int[] cellZ;

    // Grid — one bit per model-grid cell, set where the shape claims it
    private final long[] gridMask;

    // Constructor \\

    public ItemShapeStruct(SubVoxelModelStruct model) {
        this(resolveOccupied(model), model.getSizeX(), model.getSizeY(), model.getSizeZ());
    }

    public ItemShapeStruct(boolean[] occupied, int gridSizeX, int gridSizeY, int gridSizeZ) {

        int cellCount = 0;

        int minX = gridSizeX, minY = gridSizeY, minZ = gridSizeZ;
        int maxX = 0, maxY = 0, maxZ = 0;

        for (int z = 0; z < gridSizeZ; z++)
            for (int y = 0; y < gridSizeY; y++)
                for (int x = 0; x < gridSizeX; x++) {

                    if (!occupied[toCellIndex(x, y, z, gridSizeX, gridSizeY)])
                        continue;

                    cellCount++;
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    maxZ = Math.max(maxZ, z);
                }

        if (cellCount == 0)
            throwException("An item shape needs at least one filled sub-voxel cell or wall.");

        // Grid Size
        this.gridSizeX = gridSizeX;
        this.gridSizeY = gridSizeY;
        this.gridSizeZ = gridSizeZ;

        // Offset
        this.offsetX = minX;
        this.offsetY = minY;
        this.offsetZ = minZ;

        // Size
        this.sizeX = maxX - minX + 1;
        this.sizeY = maxY - minY + 1;
        this.sizeZ = maxZ - minZ + 1;

        // Reach
        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int lowest = Math.min(minX, Math.min(minY, minZ));
        int highest = Math.max(maxX, Math.max(maxY, maxZ));
        this.reachBefore = -Math.floorDiv(Math.min(lowest, resolution - 1 - highest), resolution);
        this.reachAfter = Math.floorDiv(resolution - 1 + Math.max(highest, resolution - 1 - lowest), resolution);

        // Cells
        this.cellX = new int[cellCount];
        this.cellY = new int[cellCount];
        this.cellZ = new int[cellCount];

        // Grid
        this.gridMask = new long[(gridSizeX * gridSizeY * gridSizeZ + Long.SIZE - 1) / Long.SIZE];

        int index = 0;

        for (int z = minZ; z <= maxZ; z++)
            for (int y = minY; y <= maxY; y++)
                for (int x = minX; x <= maxX; x++) {

                    int cellIndex = toCellIndex(x, y, z, gridSizeX, gridSizeY);

                    if (!occupied[cellIndex])
                        continue;

                    cellX[index] = x - minX;
                    cellY[index] = y - minY;
                    cellZ[index] = z - minZ;
                    index++;

                    gridMask[cellIndex / Long.SIZE] |= 1L << (cellIndex % Long.SIZE);
                }
    }

    // Every cell of the model grid a cube fills or a wall claims
    public static boolean[] resolveOccupied(SubVoxelModelStruct model) {

        int sizeX = model.getSizeX();
        int sizeY = model.getSizeY();
        int sizeZ = model.getSizeZ();
        boolean[] occupied = new boolean[sizeX * sizeY * sizeZ];

        for (int z = 0; z <= sizeZ; z++)
            for (int y = 0; y <= sizeY; y++)
                for (int x = 0; x <= sizeX; x++) {

                    if (model.isFilled(x, y, z))
                        occupied[toCellIndex(x, y, z, sizeX, sizeY)] = true;

                    for (int axis = 0; axis < EngineSetting.SUB_VOXEL_AXIS_COUNT; axis++)
                        if (model.hasWall(axis, x, y, z))
                            occupied[toCellIndex(
                                    axis == 0 ? Math.min(x, sizeX - 1) : x,
                                    axis == 1 ? Math.min(y, sizeY - 1) : y,
                                    axis == 2 ? Math.min(z, sizeZ - 1) : z,
                                    sizeX,
                                    sizeY)] = true;
                }

        return occupied;
    }

    // A cell's index in a model grid of the size given on x and y
    public static int toCellIndex(int x, int y, int z, int gridSizeX, int gridSizeY) {
        return x + gridSizeX * (y + gridSizeY * z);
    }

    // Rotation \\

    public int getRotatedSizeX(int rotation) {
        return isQuarterTurn(rotation) ? sizeZ : sizeX;
    }

    public int getRotatedSizeZ(int rotation) {
        return isQuarterTurn(rotation) ? sizeX : sizeZ;
    }

    public int getRotatedCellX(int cellIndex, int rotation) {

        return switch (normalizeRotation(rotation)) {
            case 1 -> sizeZ - 1 - cellZ[cellIndex];
            case 2 -> sizeX - 1 - cellX[cellIndex];
            case 3 -> cellZ[cellIndex];
            default -> cellX[cellIndex];
        };
    }

    public int getRotatedCellZ(int cellIndex, int rotation) {

        return switch (normalizeRotation(rotation)) {
            case 1 -> cellX[cellIndex];
            case 2 -> sizeZ - 1 - cellZ[cellIndex];
            case 3 -> sizeX - 1 - cellX[cellIndex];
            default -> cellZ[cellIndex];
        };
    }

    // Maps shape-local space into its rotated bounds — the continuous form of the rotated cells above
    public Matrix4 composeRotation(Matrix4 out, int rotation) {

        return switch (normalizeRotation(rotation)) {
            case 1 -> out.set(
                    0, 0, -1, sizeZ,
                    0, 1, 0, 0,
                    1, 0, 0, 0,
                    0, 0, 0, 1);
            case 2 -> out.set(
                    -1, 0, 0, sizeX,
                    0, 1, 0, 0,
                    0, 0, -1, sizeZ,
                    0, 0, 0, 1);
            case 3 -> out.set(
                    0, 0, 1, 0,
                    0, 1, 0, 0,
                    -1, 0, 0, sizeX,
                    0, 0, 0, 1);
            default -> out.set(1f);
        };
    }

    public static int normalizeRotation(int rotation) {
        return Math.floorMod(rotation, EngineSetting.ITEM_ROTATION_COUNT);
    }

    private boolean isQuarterTurn(int rotation) {
        return normalizeRotation(rotation) % 2 == 1;
    }

    // Grid \\

    // True where the shape claims this cell of the item's model grid, false outside the grid
    public boolean claimsGridCell(int x, int y, int z) {

        if (x < 0 || y < 0 || z < 0 || x >= gridSizeX || y >= gridSizeY || z >= gridSizeZ)
            return false;

        int cellIndex = toCellIndex(x, y, z, gridSizeX, gridSizeY);

        return (gridMask[cellIndex / Long.SIZE] & (1L << (cellIndex % Long.SIZE))) != 0;
    }

    // Accessible \\

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public int getOffsetZ() {
        return offsetZ;
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int getSizeZ() {
        return sizeZ;
    }

    // How many blocks before its corner's block the shape can reach on any axis, turned any way
    public int getReachBefore() {
        return reachBefore;
    }

    // How many blocks after its corner's block the shape can reach on any axis, turned any way
    public int getReachAfter() {
        return reachAfter;
    }

    public int getGridSizeX() {
        return gridSizeX;
    }

    public int getGridSizeY() {
        return gridSizeY;
    }

    public int getGridSizeZ() {
        return gridSizeZ;
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
}
