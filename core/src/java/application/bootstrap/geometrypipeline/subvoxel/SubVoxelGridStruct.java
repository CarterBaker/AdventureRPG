package application.bootstrap.geometrypipeline.subvoxel;

import java.util.Arrays;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.extras.Coordinate3Long;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

public class SubVoxelGridStruct extends StructPackage {

    /*
     * Sub-voxels of any size stored sparsely one block at a time, on the same
     * grid as everything else in the world. Each touched block holds a
     * SUB_VOXEL_RESOLUTION cube of cells, each its one-based part index or
     * SUB_VOXEL_EMPTY_CELL, exactly as a sub-voxel model stores its cubes. A
     * block wholly one part shares that part's uniform cells and allocates
     * nothing, so a trunk many blocks across costs one entry per block, and is
     * copied out the moment one of its cells changes. Double-sided walls lie
     * on grid planes, keyed by the axis they face along and a position whose
     * coordinate on that axis is the plane. Coordinates are never negative.
     * clear() keeps every cell array for reuse, so a grid kept as scratch
     * allocates nothing once warm. The last block read is remembered, since
     * meshing, collision and casting walk neighbouring cells.
     */

    // Settings
    private static final int RESOLUTION = EngineSetting.SUB_VOXEL_RESOLUTION;
    private static final int WALL_AXIS_SHIFT = 60;
    private static final int WALL_X_SHIFT = 40;
    private static final int WALL_Y_SHIFT = 20;
    private static final long WALL_COORDINATE_MASK = (1L << WALL_Y_SHIFT) - 1;

    // Cells
    private final Long2ObjectOpenHashMap<byte[]> block2Cells;
    private final byte[][] uniformCells;

    // Walls
    private final Long2ByteOpenHashMap wall2Part;

    // Pool
    private final ObjectArrayList<byte[]> cellPool;

    // Lookup
    private long lastBlock;
    private byte[] lastCells;

    // Constructor \\

    public SubVoxelGridStruct() {

        // Cells
        this.block2Cells = new Long2ObjectOpenHashMap<>();
        this.uniformCells = new byte[EngineSetting.SUB_VOXEL_MAX_PARTS][];

        // Walls
        this.wall2Part = new Long2ByteOpenHashMap();

        // Pool
        this.cellPool = new ObjectArrayList<>();

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

        byte[] cells = findCells(Coordinate3Long.pack(x / RESOLUTION, y / RESOLUTION, z / RESOLUTION));

        if (cells == null)
            return EngineSetting.INDEX_NOT_FOUND;

        int entry = cells[toCellIndex(x % RESOLUTION, y % RESOLUTION, z % RESOLUTION)] & 0xFF;

        return entry == EngineSetting.SUB_VOXEL_EMPTY_CELL ? EngineSetting.INDEX_NOT_FOUND : entry - 1;
    }

    public void setPart(int x, int y, int z, int partIndex) {

        verifyCell(x, y, z);
        verifyPart(partIndex);

        writeCell(x, y, z, (byte) (partIndex + 1));
    }

    public void clearCell(int x, int y, int z) {

        verifyCell(x, y, z);

        if (findCells(Coordinate3Long.pack(x / RESOLUTION, y / RESOLUTION, z / RESOLUTION)) != null)
            writeCell(x, y, z, (byte) EngineSetting.SUB_VOXEL_EMPTY_CELL);
    }

    // Every cell of an inclusive-minimum, exclusive-maximum box set to one part — or, when onlyEmpty, only the cells
    // still empty. A block the box wholly covers takes the part's uniform cells instead of being written cell by cell
    public void fillBox(
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ,
            int partIndex,
            boolean onlyEmpty) {

        if (minX >= maxX || minY >= maxY || minZ >= maxZ)
            return;

        verifyCell(minX, minY, minZ);
        verifyPart(partIndex);

        for (int blockZ = minZ / RESOLUTION; blockZ <= (maxZ - 1) / RESOLUTION; blockZ++)
            for (int blockY = minY / RESOLUTION; blockY <= (maxY - 1) / RESOLUTION; blockY++)
                for (int blockX = minX / RESOLUTION; blockX <= (maxX - 1) / RESOLUTION; blockX++) {

                    int fromX = Math.max(minX - blockX * RESOLUTION, 0);
                    int fromY = Math.max(minY - blockY * RESOLUTION, 0);
                    int fromZ = Math.max(minZ - blockZ * RESOLUTION, 0);
                    int toX = Math.min(maxX - blockX * RESOLUTION, RESOLUTION);
                    int toY = Math.min(maxY - blockY * RESOLUTION, RESOLUTION);
                    int toZ = Math.min(maxZ - blockZ * RESOLUTION, RESOLUTION);

                    if (fromX == 0 && fromY == 0 && fromZ == 0
                            && toX == RESOLUTION && toY == RESOLUTION && toZ == RESOLUTION)
                        fillBlock(blockX, blockY, blockZ, partIndex, onlyEmpty);
                    else
                        fillInBlock(blockX, blockY, blockZ, fromX, fromY, fromZ, toX, toY, toZ, partIndex, onlyEmpty);
                }
    }

    // A box inside one block, in cells from the block's corner, written a row at a time
    private void fillInBlock(
            int blockX,
            int blockY,
            int blockZ,
            int fromX,
            int fromY,
            int fromZ,
            int toX,
            int toY,
            int toZ,
            int partIndex,
            boolean onlyEmpty) {

        long block = Coordinate3Long.pack(blockX, blockY, blockZ);
        byte[] cells = findCells(block);
        int uniformPart = getUniformPart(cells);
        byte entry = (byte) (partIndex + 1);

        if (uniformPart != EngineSetting.INDEX_NOT_FOUND && (onlyEmpty || uniformPart == partIndex))
            return;

        if (cells == null || uniformPart != EngineSetting.INDEX_NOT_FOUND)
            cells = detachCells(block, cells);

        for (int z = fromZ; z < toZ; z++)
            for (int y = fromY; y < toY; y++) {

                int row = toCellIndex(fromX, y, z);

                if (!onlyEmpty) {
                    Arrays.fill(cells, row, row + toX - fromX, entry);
                    continue;
                }

                for (int x = row; x < row + toX - fromX; x++)
                    if (cells[x] == EngineSetting.SUB_VOXEL_EMPTY_CELL)
                        cells[x] = entry;
            }
    }

    private void writeCell(int x, int y, int z, byte entry) {

        long block = Coordinate3Long.pack(x / RESOLUTION, y / RESOLUTION, z / RESOLUTION);
        byte[] cells = findCells(block);
        int index = toCellIndex(x % RESOLUTION, y % RESOLUTION, z % RESOLUTION);

        if (cells != null && cells[index] == entry)
            return;

        if (cells == null || getUniformPart(cells) != EngineSetting.INDEX_NOT_FOUND)
            cells = detachCells(block, cells);

        cells[index] = entry;
    }

    // A private copy of a block's cells, from its uniform cells or empty, stored in its place
    private byte[] detachCells(long block, byte[] shared) {

        byte[] cells = cellPool.isEmpty() ? new byte[EngineSetting.SUB_VOXEL_CELL_COUNT] : cellPool.pop();

        if (shared != null)
            System.arraycopy(shared, 0, cells, 0, cells.length);

        block2Cells.put(block, cells);
        lastBlock = block;
        lastCells = cells;

        return cells;
    }

    // Blocks \\

    // A whole block given to one part, sharing that part's uniform cells — or, when onlyEmpty, only a block nothing
    // fills yet
    public void fillBlock(int blockX, int blockY, int blockZ, int partIndex, boolean onlyEmpty) {

        verifyCell(blockX, blockY, blockZ);
        verifyPart(partIndex);

        long block = Coordinate3Long.pack(blockX, blockY, blockZ);
        byte[] existing = findCells(block);

        if (existing != null && onlyEmpty) {

            if (getUniformPart(existing) == EngineSetting.INDEX_NOT_FOUND)
                fillInBlock(blockX, blockY, blockZ, 0, 0, 0, RESOLUTION, RESOLUTION, RESOLUTION, partIndex, true);

            return;
        }

        releaseCells(block2Cells.put(block, resolveUniformCells(partIndex)));

        lastBlock = block;
        lastCells = block2Cells.get(block);
    }

    // Every block whose cells are all one part shares that part's uniform cells instead
    public void collapseUniformBlocks() {

        ObjectIterator<Long2ObjectMap.Entry<byte[]>> iterator = block2Cells.long2ObjectEntrySet().fastIterator();

        while (iterator.hasNext()) {

            Long2ObjectMap.Entry<byte[]> entry = iterator.next();
            byte[] cells = entry.getValue();

            if (getUniformPart(cells) != EngineSetting.INDEX_NOT_FOUND || !isSinglePart(cells))
                continue;

            entry.setValue(resolveUniformCells((cells[0] & 0xFF) - 1));
            releaseCells(cells);
        }

        lastBlock = Long.MIN_VALUE;
        lastCells = null;
    }

    private boolean isSinglePart(byte[] cells) {

        byte first = cells[0];

        if (first == EngineSetting.SUB_VOXEL_EMPTY_CELL)
            return false;

        for (int i = 1; i < cells.length; i++)
            if (cells[i] != first)
                return false;

        return true;
    }

    // The zero-based part a block's cells wholly belong to when they are that part's uniform cells, else
    // INDEX_NOT_FOUND
    public int getUniformPart(byte[] cells) {

        if (cells == null)
            return EngineSetting.INDEX_NOT_FOUND;

        int entry = cells[0] & 0xFF;

        if (entry == EngineSetting.SUB_VOXEL_EMPTY_CELL || cells != uniformCells[entry - 1])
            return EngineSetting.INDEX_NOT_FOUND;

        return entry - 1;
    }

    private byte[] resolveUniformCells(int partIndex) {

        byte[] cells = uniformCells[partIndex];

        if (cells == null) {
            cells = new byte[EngineSetting.SUB_VOXEL_CELL_COUNT];
            Arrays.fill(cells, (byte) (partIndex + 1));
            uniformCells[partIndex] = cells;
        }

        return cells;
    }

    public LongSet getBlocks() {
        return block2Cells.keySet();
    }

    // The cells of one block, one byte each in toCellIndex() order, null when nothing fills it — read only
    public byte[] getBlockCells(int blockX, int blockY, int blockZ) {

        if (blockX < 0 || blockY < 0 || blockZ < 0)
            return null;

        return findCells(Coordinate3Long.pack(blockX, blockY, blockZ));
    }

    public int getBlockCount() {
        return block2Cells.size();
    }

    public boolean isEmpty() {
        return block2Cells.isEmpty() && wall2Part.isEmpty();
    }

    // Walls \\

    public boolean hasWall(int axis, int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && wall2Part.containsKey(packWall(axis, x, y, z));
    }

    // The zero-based part of a wall, INDEX_NOT_FOUND when there is none
    public int getWallPart(int axis, int x, int y, int z) {

        if (x < 0 || y < 0 || z < 0)
            return EngineSetting.INDEX_NOT_FOUND;

        int entry = wall2Part.get(packWall(axis, x, y, z)) & 0xFF;

        return entry == EngineSetting.SUB_VOXEL_EMPTY_CELL ? EngineSetting.INDEX_NOT_FOUND : entry - 1;
    }

    public void setWall(int axis, int x, int y, int z, int partIndex) {

        verifyCell(x, y, z);
        verifyPart(partIndex);

        wall2Part.put(packWall(axis, x, y, z), (byte) (partIndex + 1));
    }

    public boolean hasWalls() {
        return !wall2Part.isEmpty();
    }

    public ObjectIterator<Long2ByteMap.Entry> getWalls() {
        return wall2Part.long2ByteEntrySet().fastIterator();
    }

    public static long packWall(int axis, int x, int y, int z) {
        return ((long) axis << WALL_AXIS_SHIFT) | ((long) x << WALL_X_SHIFT) | ((long) y << WALL_Y_SHIFT) | z;
    }

    public static int unpackWallAxis(long wall) {
        return (int) (wall >>> WALL_AXIS_SHIFT);
    }

    public static int unpackWallX(long wall) {
        return (int) ((wall >>> WALL_X_SHIFT) & WALL_COORDINATE_MASK);
    }

    public static int unpackWallY(long wall) {
        return (int) ((wall >>> WALL_Y_SHIFT) & WALL_COORDINATE_MASK);
    }

    public static int unpackWallZ(long wall) {
        return (int) (wall & WALL_COORDINATE_MASK);
    }

    // Reset \\

    // Empties the grid, keeping every private cell array for the next fill
    public void clear() {

        for (byte[] cells : block2Cells.values())
            releaseCells(cells);

        block2Cells.clear();
        wall2Part.clear();

        lastBlock = Long.MIN_VALUE;
        lastCells = null;
    }

    private void releaseCells(byte[] cells) {

        if (cells == null || getUniformPart(cells) != EngineSetting.INDEX_NOT_FOUND)
            return;

        Arrays.fill(cells, (byte) EngineSetting.SUB_VOXEL_EMPTY_CELL);
        cellPool.push(cells);
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

    private void verifyCell(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0)
            throwException("Sub-voxel (" + x + ", " + y + ", " + z + ") lies below the grid.");
    }

    private void verifyPart(int partIndex) {
        if (partIndex < 0 || partIndex >= EngineSetting.SUB_VOXEL_MAX_PARTS)
            throwException("Sub-voxel part index " + partIndex + " is outside the "
                    + EngineSetting.SUB_VOXEL_MAX_PARTS + " parts a grid can hold.");
    }

    // A cell's index inside its block's cells, from its coordinates inside that block
    public static int toCellIndex(int x, int y, int z) {
        return x + RESOLUTION * (y + RESOLUTION * z);
    }
}
