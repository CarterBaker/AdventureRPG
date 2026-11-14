package application.bootstrap.worldpipeline.worldgenerationmanager;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import application.bootstrap.worldpipeline.util.TerrainCarveUtility;
import application.kernel.kernelpipeline.kernel.KernelArgumentInstance;
import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;
import engine.util.memory.BufferUtility;

class CaveDensityAsyncContainer extends AsyncContainerPackage {

    /*
     * Per-thread memory of one cave density field, the only memory the
     * CaveDensity kernel reads and writes. bind() copies a frame's lattice and
     * lakes and a chunk's column fields in and empties the field; addPoint()
     * queues a point with a tag of the caller's own, and once
     * CaveCarveBranch.computeField() has run, every queued point holds its
     * density and whether it lies in lake water. Every buffer is sized once to
     * the largest frame, lake count and point count any caller can bind, so a
     * field allocates nothing.
     */

    // Capacity
    static final int POINT_CAPACITY = CaveVolumeAsyncContainer.CELL_COUNT;
    static final int LATTICE_CAPACITY = Math.max(
            TerrainColumnAsyncContainer.LATTICE_POINTS,
            TerrainCarveUtility.COLUMN_LATTICE_SIDE * TerrainCarveUtility.COLUMN_LATTICE_SIDE
                    * Math.max(CaveProbeAsyncContainer.COLUMN_LATTICE_ROWS, CaveVolumeAsyncContainer.SCAN_LATTICE_ROWS))
            * TerrainCarveUtility.CAVE_CHANNELS;
    static final int LAKE_CAPACITY = EngineSetting.CAVE_LAKE_SITE_MAX * EngineSetting.CAVE_DENSITY_LAKE_FLOATS;
    static final int COLUMN_COUNT = EngineSetting.CHUNK_SIZE * EngineSetting.CHUNK_SIZE;

    // Frame
    FloatBuffer lattice;
    int latticeSide;
    int latticeRows;
    int latticeOriginY;
    float latticeOffsetX;
    float latticeOffsetZ;
    FloatBuffer lakes;
    int lakeCount;

    // Columns
    IntBuffer columnFloorY;
    IntBuffer columnCeilingY;
    IntBuffer columnGroundY;
    FloatBuffer columnTunnels;
    FloatBuffer columnNoodles;
    FloatBuffer columnCaverns;
    FloatBuffer columnBarrier;

    // Points
    FloatBuffer pointX;
    FloatBuffer pointY;
    FloatBuffer pointZ;
    IntBuffer pointColumn;
    int[] pointTags;
    int pointCount;

    // Results
    FloatBuffer densities;
    ByteBuffer lakeWater;

    // Scratch
    FloatBuffer radiusScratch;
    FloatBuffer lakeScratch;

    // Kernel
    KernelArgumentInstance arguments;

    @Override
    protected void create() {

        // Frame
        this.lattice = BufferUtility.newFloatBuffer(LATTICE_CAPACITY);
        this.lakes = BufferUtility.newFloatBuffer(LAKE_CAPACITY);

        // Columns
        this.columnFloorY = BufferUtility.newIntBuffer(COLUMN_COUNT);
        this.columnCeilingY = BufferUtility.newIntBuffer(COLUMN_COUNT);
        this.columnGroundY = BufferUtility.newIntBuffer(COLUMN_COUNT);
        this.columnTunnels = BufferUtility.newFloatBuffer(COLUMN_COUNT);
        this.columnNoodles = BufferUtility.newFloatBuffer(COLUMN_COUNT);
        this.columnCaverns = BufferUtility.newFloatBuffer(COLUMN_COUNT);
        this.columnBarrier = BufferUtility.newFloatBuffer(COLUMN_COUNT);

        // Points
        this.pointX = BufferUtility.newFloatBuffer(POINT_CAPACITY);
        this.pointY = BufferUtility.newFloatBuffer(POINT_CAPACITY);
        this.pointZ = BufferUtility.newFloatBuffer(POINT_CAPACITY);
        this.pointColumn = BufferUtility.newIntBuffer(POINT_CAPACITY);
        this.pointTags = new int[POINT_CAPACITY];

        // Results
        this.densities = BufferUtility.newFloatBuffer(POINT_CAPACITY);
        this.lakeWater = BufferUtility.newByteBuffer(POINT_CAPACITY);

        // Scratch
        this.radiusScratch = BufferUtility.newFloatBuffer(POINT_CAPACITY);
        this.lakeScratch = BufferUtility.newFloatBuffer(POINT_CAPACITY);

        // Kernel
        this.arguments = create(KernelArgumentInstance.class);
    }

    // Bind \\

    // The frame and the chunk's columns every point after this reads, and no points yet
    void bind(CaveFrameStruct frame, TerrainColumnAsyncContainer column) {

        int latticeFloats = frame.side * frame.side * frame.rows * TerrainCarveUtility.CAVE_CHANNELS;

        if (latticeFloats > LATTICE_CAPACITY)
            throwException("A cave frame of " + latticeFloats + " lattice values exceeds the density field's "
                    + LATTICE_CAPACITY);

        lattice.clear();
        lattice.put(frame.lattice, 0, latticeFloats);
        this.latticeSide = frame.side;
        this.latticeRows = frame.rows;
        this.latticeOriginY = frame.originY;
        this.latticeOffsetX = frame.latticeOffsetX;
        this.latticeOffsetZ = frame.latticeOffsetZ;

        bindLakes(frame);
        bindColumns(column);
        clearPoints();
    }

    // Each lake as seven floats in CaveLakeStruct order
    private void bindLakes(CaveFrameStruct frame) {

        lakes.clear();

        for (int i = 0; i < frame.lakeSiteCount; i++) {

            CaveLakeSiteStruct site = frame.lakeSites[i];

            lakes.put(site.centerX);
            lakes.put(site.centerZ);
            lakes.put(site.waterY);
            lakes.put(site.radiusX);
            lakes.put(site.radiusZ);
            lakes.put(site.depth);
            lakes.put(site.headroom);
        }

        this.lakeCount = frame.lakeSiteCount;
    }

    private void bindColumns(TerrainColumnAsyncContainer column) {

        columnFloorY.clear();
        columnFloorY.put(column.columnCaveFloorY, 0, COLUMN_COUNT);
        columnCeilingY.clear();
        columnCeilingY.put(column.columnCaveCeilingY, 0, COLUMN_COUNT);
        columnGroundY.clear();
        columnGroundY.put(column.groundHeightBlocks, 0, COLUMN_COUNT);
        columnTunnels.clear();
        columnTunnels.put(column.columnCaveTunnels, 0, COLUMN_COUNT);
        columnNoodles.clear();
        columnNoodles.put(column.columnCaveNoodles, 0, COLUMN_COUNT);
        columnCaverns.clear();
        columnCaverns.put(column.columnCaveCaverns, 0, COLUMN_COUNT);
        columnBarrier.clear();
        columnBarrier.put(column.columnCaveBarrier, 0, COLUMN_COUNT);
    }

    // Points \\

    void clearPoints() {
        pointCount = 0;
    }

    boolean hasRoom(int points) {
        return pointCount + points <= POINT_CAPACITY;
    }

    // Queues a point read in the frame's coordinates, tagged with whatever the caller needs back
    void addPoint(float frameX, float frameY, float frameZ, int columnIndex, int tag) {

        if (pointCount >= POINT_CAPACITY)
            throwException("A cave density field holds at most " + POINT_CAPACITY + " points");

        pointX.put(pointCount, frameX);
        pointY.put(pointCount, frameY);
        pointZ.put(pointCount, frameZ);
        pointColumn.put(pointCount, columnIndex);
        pointTags[pointCount] = tag;
        pointCount++;
    }

    // Results \\

    float getDensity(int point) {
        return densities.get(point);
    }

    boolean isLakeWater(int point) {
        return lakeWater.get(point) != 0;
    }

    int getTag(int point) {
        return pointTags[point];
    }

    int getPointCount() {
        return pointCount;
    }
}
