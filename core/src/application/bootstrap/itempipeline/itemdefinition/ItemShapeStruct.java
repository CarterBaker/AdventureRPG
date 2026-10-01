package application.bootstrap.itempipeline.itemdefinition;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.matrices.Matrix4;

public class ItemShapeStruct extends StructPackage {

    /*
     * The sub-voxel cells an item claims, trimmed to their bounds: the volume
     * it takes up inside a container and standing in the world. An item's
     * file defines the cells as boxes in its model grid, or leaves them to
     * its model, where a wall claims the cell on its far side, or the last
     * cell where it closes the grid, so a flat item still takes up one layer.
     * The grid mask answers in one read whether a model-grid cell is claimed.
     * A rotation is a number of quarter turns about the vertical axis; rotated
     * cells and the rotated shape transform both keep the shape inside its own
     * rotated bounds, so a placement is always the shape's minimum corner.
     */

    // Offset — the trimmed shape's first cell inside the item's model grid
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;

    // Size
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    // Cells — shape-local coordinates of every filled cell
    private final int[] cellX;
    private final int[] cellY;
    private final int[] cellZ;

    // Grid — one bit per model-grid cell, set where the shape claims it
    private final long[] gridMask;

    // Constructor \\

    public ItemShapeStruct(SubVoxelModelStruct model) {
        this(resolveOccupied(model));
    }

    public ItemShapeStruct(boolean[] occupied) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int cellCount = 0;

        int minX = resolution, minY = resolution, minZ = resolution;
        int maxX = 0, maxY = 0, maxZ = 0;

        for (int z = 0; z < resolution; z++)
            for (int y = 0; y < resolution; y++)
                for (int x = 0; x < resolution; x++) {

                    if (!occupied[toCellIndex(x, y, z)])
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

        // Offset
        this.offsetX = minX;
        this.offsetY = minY;
        this.offsetZ = minZ;

        // Size
        this.sizeX = maxX - minX + 1;
        this.sizeY = maxY - minY + 1;
        this.sizeZ = maxZ - minZ + 1;

        // Cells
        this.cellX = new int[cellCount];
        this.cellY = new int[cellCount];
        this.cellZ = new int[cellCount];

        // Grid
        this.gridMask = new long[EngineSetting.SUB_VOXEL_CELL_COUNT / Long.SIZE];

        int index = 0;

        for (int z = minZ; z <= maxZ; z++)
            for (int y = minY; y <= maxY; y++)
                for (int x = minX; x <= maxX; x++) {

                    if (!occupied[toCellIndex(x, y, z)])
                        continue;

                    cellX[index] = x - minX;
                    cellY[index] = y - minY;
                    cellZ[index] = z - minZ;
                    index++;

                    int cellIndex = toCellIndex(x, y, z);
                    gridMask[cellIndex / Long.SIZE] |= 1L << (cellIndex % Long.SIZE);
                }
    }

    // Every cell a cube fills or a wall claims
    public static boolean[] resolveOccupied(SubVoxelModelStruct model) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        boolean[] occupied = new boolean[EngineSetting.SUB_VOXEL_CELL_COUNT];

        for (int z = 0; z <= resolution; z++)
            for (int y = 0; y <= resolution; y++)
                for (int x = 0; x <= resolution; x++) {

                    if (model.isFilled(x, y, z))
                        occupied[toCellIndex(x, y, z)] = true;

                    for (int axis = 0; axis < EngineSetting.SUB_VOXEL_AXIS_COUNT; axis++)
                        if (model.hasWall(axis, x, y, z))
                            occupied[toCellIndex(
                                    axis == 0 ? Math.min(x, resolution - 1) : x,
                                    axis == 1 ? Math.min(y, resolution - 1) : y,
                                    axis == 2 ? Math.min(z, resolution - 1) : z)] = true;
                }

        return occupied;
    }

    public static int toCellIndex(int x, int y, int z) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        return x + resolution * (y + resolution * z);
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

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        if (x < 0 || y < 0 || z < 0 || x >= resolution || y >= resolution || z >= resolution)
            return false;

        int cellIndex = toCellIndex(x, y, z);

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
