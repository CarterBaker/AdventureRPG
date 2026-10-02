package application.bootstrap.vehiclepipeline.vehicle;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.extras.Coordinate3Long;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;

public class VehicleGridStruct extends StructPackage {

    /*
     * A vehicle's sub-voxels, stored sparsely one block at a time. Each touched
     * block holds a SUB_VOXEL_RESOLUTION cube of cells, and each cell its
     * one-based part index or SUB_VOXEL_EMPTY_CELL, exactly as a sub-voxel
     * model stores its cubes, so a vehicle of any size sits on the same grid
     * as everything else in the world. Coordinates are the vehicle's model
     * sub-voxels, never negative. The last block read is remembered, since
     * collision and casting walk neighbouring cells.
     */

    // Cells
    private final Long2ObjectOpenHashMap<byte[]> block2Cells;

    // Lookup
    private long lastBlock;
    private byte[] lastCells;

    // Constructor \\

    public VehicleGridStruct() {

        // Cells
        this.block2Cells = new Long2ObjectOpenHashMap<>();

        // Lookup
        this.lastBlock = Long.MIN_VALUE;
        this.lastCells = null;
    }

    // Cells \\

    public boolean isFilled(int x, int y, int z) {
        return getPart(x, y, z) != EngineSetting.INDEX_NOT_FOUND;
    }

    // The zero-based part index filling a cell, INDEX_NOT_FOUND when it is empty
    public int getPart(int x, int y, int z) {

        if (x < 0 || y < 0 || z < 0)
            return EngineSetting.INDEX_NOT_FOUND;

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        byte[] cells = findCells(Coordinate3Long.pack(x / resolution, y / resolution, z / resolution));

        if (cells == null)
            return EngineSetting.INDEX_NOT_FOUND;

        int entry = cells[toCellIndex(x % resolution, y % resolution, z % resolution)] & 0xFF;

        return entry == EngineSetting.SUB_VOXEL_EMPTY_CELL ? EngineSetting.INDEX_NOT_FOUND : entry - 1;
    }

    public void setPart(int x, int y, int z, int partIndex) {

        if (x < 0 || y < 0 || z < 0)
            throwException("Vehicle sub-voxel (" + x + ", " + y + ", " + z + ") lies below the model grid.");

        if (partIndex < 0 || partIndex >= EngineSetting.SUB_VOXEL_MAX_PARTS)
            throwException("Vehicle part index " + partIndex + " is outside the "
                    + EngineSetting.SUB_VOXEL_MAX_PARTS + " parts a vehicle can hold.");

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        long block = Coordinate3Long.pack(x / resolution, y / resolution, z / resolution);
        byte[] cells = findCells(block);

        if (cells == null) {
            cells = new byte[EngineSetting.SUB_VOXEL_CELL_COUNT];
            block2Cells.put(block, cells);
            lastBlock = block;
            lastCells = cells;
        }

        cells[toCellIndex(x % resolution, y % resolution, z % resolution)] = (byte) (partIndex + 1);
    }

    // Every cell of an inclusive-minimum, exclusive-maximum box set to one part
    public void fillBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int partIndex) {

        for (int z = minZ; z < maxZ; z++)
            for (int y = minY; y < maxY; y++)
                for (int x = minX; x < maxX; x++)
                    setPart(x, y, z, partIndex);
    }

    // Blocks \\

    public LongSet getBlocks() {
        return block2Cells.keySet();
    }

    // The cells of one block, one byte each in toCellIndex() order, null when nothing fills it
    public byte[] getBlockCells(int blockX, int blockY, int blockZ) {

        if (blockX < 0 || blockY < 0 || blockZ < 0)
            return null;

        return findCells(Coordinate3Long.pack(blockX, blockY, blockZ));
    }

    public int getBlockCount() {
        return block2Cells.size();
    }

    public boolean isEmpty() {
        return block2Cells.isEmpty();
    }

    // Utility \\

    private byte[] findCells(long block) {

        if (block == lastBlock)
            return lastCells;

        byte[] cells = block2Cells.get(block);

        lastBlock = block;
        lastCells = cells;

        return cells;
    }

    // A cell's index inside its block's cells, from its coordinates inside that block
    public static int toCellIndex(int x, int y, int z) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        return x + resolution * (y + resolution * z);
    }
}
