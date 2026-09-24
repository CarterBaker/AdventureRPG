package application.bootstrap.geometrypipeline.subvoxel;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SubVoxelModelStruct extends StructPackage {

    /*
     * A model of tiny cubes on the SUB_VOXEL_RESOLUTION grid inside one block.
     * Each cell stores its one-based part index or SUB_VOXEL_EMPTY_CELL, so every
     * generated vertex lands exactly on a sub-voxel boundary.
     */

    // Parts
    private final ObjectArrayList<SubVoxelPartStruct> parts;

    // Cells
    private final byte[] cells;

    // Constructor \\

    public SubVoxelModelStruct() {

        // Parts
        this.parts = new ObjectArrayList<>();

        // Cells
        this.cells = new byte[EngineSetting.SUB_VOXEL_CELL_COUNT];
    }

    public SubVoxelModelStruct(SubVoxelModelStruct source) {

        // Parts
        this.parts = new ObjectArrayList<>(source.parts.size());

        for (int i = 0; i < source.parts.size(); i++) {
            SubVoxelPartStruct part = source.parts.get(i);
            this.parts.add(new SubVoxelPartStruct(part.getPartName(), part.getTextureName()));
        }

        // Cells
        this.cells = source.cells.clone();
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

        int removedCell = partIndex + 1;

        for (int i = 0; i < cells.length; i++) {

            int cell = cells[i] & 0xFF;

            if (cell == removedCell)
                cells[i] = (byte) EngineSetting.SUB_VOXEL_EMPTY_CELL;
            else if (cell > removedCell)
                cells[i] = (byte) (cell - 1);
        }

        parts.remove(partIndex);
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

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        return x >= 0 && y >= 0 && z >= 0 && x < resolution && y < resolution && z < resolution;
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

        int count = 0;

        for (int i = 0; i < cells.length; i++)
            if (cells[i] != EngineSetting.SUB_VOXEL_EMPTY_CELL)
                count++;

        return count;
    }

    public boolean isEmpty() {
        return getFilledCellCount() == 0;
    }

    // Utility \\

    private int toCellIndex(int x, int y, int z) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        return x + resolution * (y + resolution * z);
    }

    private void verifyCell(int x, int y, int z) {
        if (!isInside(x, y, z))
            throwException("Sub-voxel cell (" + x + ", " + y + ", " + z + ") lies outside the model grid.");
    }

    private void verifyPartIndex(int partIndex) {
        if (!hasPart(partIndex))
            throwException("Sub-voxel part index " + partIndex + " does not exist — model has "
                    + parts.size() + " parts.");
    }
}
