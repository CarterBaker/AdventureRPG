package application.bootstrap.geometrypipeline.subvoxel;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SubVoxelModelStruct extends StructPackage {

    /*
     * A model of tiny cubes and flat walls on the SUB_VOXEL_RESOLUTION grid,
     * one block by default and up to SUB_VOXEL_MAX_MODEL_BLOCKS on each axis,
     * so furniture keeps its real size. Each cell stores its one-based part
     * index or SUB_VOXEL_EMPTY_CELL. A wall is one double-sided sub-voxel
     * square lying on a grid plane: it is addressed by the axis it faces along
     * and a position whose coordinate on that axis is the plane, from 0 to the
     * model's size, and whose other two are the cell it covers. Every
     * generated vertex lands exactly on a sub-voxel boundary.
     */

    // Size — in blocks, and in sub-voxel cells
    private final int blocksX;
    private final int blocksY;
    private final int blocksZ;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    // Parts
    private final ObjectArrayList<SubVoxelPartStruct> parts;

    // Cells
    private final byte[] cells;

    // Walls
    private final byte[] walls;

    // Constructor \\

    public SubVoxelModelStruct() {
        this(1, 1, 1);
    }

    public SubVoxelModelStruct(int blocksX, int blocksY, int blocksZ) {

        int maxBlocks = EngineSetting.SUB_VOXEL_MAX_MODEL_BLOCKS;

        if (blocksX < 1 || blocksY < 1 || blocksZ < 1
                || blocksX > maxBlocks || blocksY > maxBlocks || blocksZ > maxBlocks)
            throwException("Sub-voxel model of " + blocksX + " x " + blocksY + " x " + blocksZ
                    + " blocks is outside 1 to " + maxBlocks + " blocks on an axis.");

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        // Size
        this.blocksX = blocksX;
        this.blocksY = blocksY;
        this.blocksZ = blocksZ;
        this.sizeX = blocksX * resolution;
        this.sizeY = blocksY * resolution;
        this.sizeZ = blocksZ * resolution;

        // Parts
        this.parts = new ObjectArrayList<>();

        // Cells
        this.cells = new byte[sizeX * sizeY * sizeZ];

        // Walls
        this.walls = new byte[(sizeX + 1) * sizeY * sizeZ + sizeX * (sizeY + 1) * sizeZ
                + sizeX * sizeY * (sizeZ + 1)];
    }

    public SubVoxelModelStruct(SubVoxelModelStruct source) {

        // Size
        this.blocksX = source.blocksX;
        this.blocksY = source.blocksY;
        this.blocksZ = source.blocksZ;
        this.sizeX = source.sizeX;
        this.sizeY = source.sizeY;
        this.sizeZ = source.sizeZ;

        // Parts
        this.parts = new ObjectArrayList<>(source.parts.size());

        for (int i = 0; i < source.parts.size(); i++) {
            SubVoxelPartStruct part = source.parts.get(i);
            this.parts.add(new SubVoxelPartStruct(part.getPartName(), part.getTextureName()));
        }

        // Cells
        this.cells = source.cells.clone();

        // Walls
        this.walls = source.walls.clone();
    }

    // Parts \\

    public int addPart(SubVoxelPartStruct part) {

        if (parts.size() >= EngineSetting.SUB_VOXEL_MAX_PARTS)
            throwException("Sub-voxel model cannot hold more than " + EngineSetting.SUB_VOXEL_MAX_PARTS + " parts.");

        parts.add(part);
        return parts.size() - 1;
    }

    public void removePart(int partIndex) {

        verifyPartIndex(partIndex);

        removePartEntries(cells, partIndex + 1);
        removePartEntries(walls, partIndex + 1);

        parts.remove(partIndex);
    }

    private void removePartEntries(byte[] entries, int removedEntry) {

        for (int i = 0; i < entries.length; i++) {

            int entry = entries[i] & 0xFF;

            if (entry == removedEntry)
                entries[i] = (byte) EngineSetting.SUB_VOXEL_EMPTY_CELL;
            else if (entry > removedEntry)
                entries[i] = (byte) (entry - 1);
        }
    }

    public SubVoxelPartStruct getPart(int partIndex) {

        verifyPartIndex(partIndex);
        return parts.get(partIndex);
    }

    public int getPartCount() {
        return parts.size();
    }

    public boolean hasPart(int partIndex) {
        return partIndex >= 0 && partIndex < parts.size();
    }

    // Cells \\

    public boolean isInside(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sizeX && y < sizeY && z < sizeZ;
    }

    public boolean isFilled(int x, int y, int z) {
        return isInside(x, y, z) && cells[toCellIndex(x, y, z)] != EngineSetting.SUB_VOXEL_EMPTY_CELL;
    }

    public int getCellPart(int x, int y, int z) {

        if (!isFilled(x, y, z))
            return EngineSetting.INDEX_NOT_FOUND;

        return (cells[toCellIndex(x, y, z)] & 0xFF) - 1;
    }

    public void setCell(int x, int y, int z, int partIndex) {

        verifyCell(x, y, z);
        verifyPartIndex(partIndex);

        cells[toCellIndex(x, y, z)] = (byte) (partIndex + 1);
    }

    public void clearCell(int x, int y, int z) {

        verifyCell(x, y, z);
        cells[toCellIndex(x, y, z)] = (byte) EngineSetting.SUB_VOXEL_EMPTY_CELL;
    }

    public int getFilledCellCount() {
        return countEntries(cells);
    }

    // Walls \\

    public boolean isWallInside(int axis, int x, int y, int z) {

        int[] position = { x, y, z };

        if (axis < 0 || axis >= EngineSetting.SUB_VOXEL_AXIS_COUNT)
            return false;

        for (int i = 0; i < EngineSetting.SUB_VOXEL_AXIS_COUNT; i++) {

            int limit = i == axis ? getSize(i) : getSize(i) - 1;

            if (position[i] < 0 || position[i] > limit)
                return false;
        }

        return true;
    }

    public boolean hasWall(int axis, int x, int y, int z) {
        return isWallInside(axis, x, y, z)
                && walls[toWallIndex(axis, x, y, z)] != EngineSetting.SUB_VOXEL_EMPTY_CELL;
    }

    public int getWallPart(int axis, int x, int y, int z) {

        if (!hasWall(axis, x, y, z))
            return EngineSetting.INDEX_NOT_FOUND;

        return (walls[toWallIndex(axis, x, y, z)] & 0xFF) - 1;
    }

    public void setWall(int axis, int x, int y, int z, int partIndex) {

        verifyWall(axis, x, y, z);
        verifyPartIndex(partIndex);

        walls[toWallIndex(axis, x, y, z)] = (byte) (partIndex + 1);
    }

    public void clearWall(int axis, int x, int y, int z) {

        verifyWall(axis, x, y, z);
        walls[toWallIndex(axis, x, y, z)] = (byte) EngineSetting.SUB_VOXEL_EMPTY_CELL;
    }

    public int getWallCount() {
        return countEntries(walls);
    }

    // Contents \\

    public boolean isEmpty() {
        return getFilledCellCount() == 0 && getWallCount() == 0;
    }

    private int countEntries(byte[] entries) {

        int count = 0;

        for (int i = 0; i < entries.length; i++)
            if (entries[i] != EngineSetting.SUB_VOXEL_EMPTY_CELL)
                count++;

        return count;
    }

    // Utility \\

    private int toCellIndex(int x, int y, int z) {
        return x + sizeX * (y + sizeY * z);
    }

    // Each axis's walls follow the last axis's; the plane coordinate runs fastest, then the two covered axes in the
    // mesher's u, v order
    private int toWallIndex(int axis, int x, int y, int z) {

        int[] position = { x, y, z };
        int uAxis = (axis + 1) % EngineSetting.SUB_VOXEL_AXIS_COUNT;
        int vAxis = (axis + 2) % EngineSetting.SUB_VOXEL_AXIS_COUNT;
        int base = 0;

        for (int previous = 0; previous < axis; previous++)
            base += countWalls(previous);

        return base + position[axis]
                + (getSize(axis) + 1) * (position[uAxis] + getSize(uAxis) * position[vAxis]);
    }

    private int countWalls(int axis) {
        return (getSize(axis) + 1) * getSize((axis + 1) % EngineSetting.SUB_VOXEL_AXIS_COUNT)
                * getSize((axis + 2) % EngineSetting.SUB_VOXEL_AXIS_COUNT);
    }

    private void verifyCell(int x, int y, int z) {
        if (!isInside(x, y, z))
            throwException("Sub-voxel cell (" + x + ", " + y + ", " + z + ") lies outside the model grid.");
    }

    private void verifyWall(int axis, int x, int y, int z) {
        if (!isWallInside(axis, x, y, z))
            throwException("Sub-voxel wall (" + x + ", " + y + ", " + z + ") on axis " + axis
                    + " lies outside the model grid.");
    }

    private void verifyPartIndex(int partIndex) {
        if (!hasPart(partIndex))
            throwException("Sub-voxel part index " + partIndex + " does not exist — model has "
                    + parts.size() + " parts.");
    }

    // Accessible \\

    public int getBlocksX() {
        return blocksX;
    }

    public int getBlocksY() {
        return blocksY;
    }

    public int getBlocksZ() {
        return blocksZ;
    }

    // The model's size on an axis in blocks
    public int getBlocks(int axis) {
        return axis == EngineSetting.AXIS_X ? blocksX : axis == EngineSetting.AXIS_Y ? blocksY : blocksZ;
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

    // The model's size on an axis in sub-voxel cells
    public int getSize(int axis) {
        return axis == EngineSetting.AXIS_X ? sizeX : axis == EngineSetting.AXIS_Y ? sizeY : sizeZ;
    }

    public boolean isSingleBlock() {
        return blocksX == 1 && blocksY == 1 && blocksZ == 1;
    }
}
