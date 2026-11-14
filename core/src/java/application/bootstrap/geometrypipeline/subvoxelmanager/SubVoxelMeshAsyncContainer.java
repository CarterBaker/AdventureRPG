package application.bootstrap.geometrypipeline.subvoxelmanager;

import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SubVoxelMeshAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for one sub-voxel mesh pass: the blocks of each
     * layer along the axis being swept, the masks of the plane being merged,
     * one per block it crosses, each block's cells as bit planes, spare
     * masks, planes and lists to reuse, and the walls of each plane. Any thread may mesh at once, and once warm a pass
     * allocates nothing.
     */

    // Layers — the blocks of the grid by their block coordinate along the swept axis
    Int2ObjectOpenHashMap<LongArrayList> layer2Blocks;
    IntArrayList layerOrder;
    ObjectArrayList<LongArrayList> spareBlockLists;

    // Occupancy — per block, one bit per cell of every slice along every axis, in the planes' own a, b order
    Long2ObjectOpenHashMap<long[]> block2Planes;
    ObjectArrayList<long[]> sparePlanes;
    long[] exposedScratch;

    // Plane — the masks of the plane being merged, keyed by block u, v
    Long2ObjectOpenHashMap<int[]> planeMasks;
    LongArrayList planeOrder;
    ObjectArrayList<int[]> spareMasks;
    long lastMaskKey;
    int[] lastMask;

    // Walls — every shown wall of one face and plane, three ints each: u, v and one-based part
    Long2ObjectOpenHashMap<IntArrayList> wallPlane2Cells;
    LongArrayList wallPlaneOrder;
    ObjectArrayList<IntArrayList> spareWallLists;

    // Region — the blocks that make faces, inclusive, indexed by axis
    int[] regionMin;
    int[] regionMax;

    // Scratch — indexed by axis
    int[] blockScratch;
    int[] cellScratch;

    @Override
    protected void create() {

        // Layers
        this.layer2Blocks = new Int2ObjectOpenHashMap<>();
        this.layerOrder = new IntArrayList();
        this.spareBlockLists = new ObjectArrayList<>();

        // Occupancy
        this.block2Planes = new Long2ObjectOpenHashMap<>();
        this.sparePlanes = new ObjectArrayList<>();
        this.exposedScratch = new long[EngineSetting.SUB_VOXEL_RESOLUTION * EngineSetting.SUB_VOXEL_RESOLUTION
                / Long.SIZE];

        // Plane
        this.planeMasks = new Long2ObjectOpenHashMap<>();
        this.planeOrder = new LongArrayList();
        this.spareMasks = new ObjectArrayList<>();

        // Walls
        this.wallPlane2Cells = new Long2ObjectOpenHashMap<>();
        this.wallPlaneOrder = new LongArrayList();
        this.spareWallLists = new ObjectArrayList<>();

        // Region
        this.regionMin = new int[EngineSetting.AXIS_COUNT];
        this.regionMax = new int[EngineSetting.AXIS_COUNT];

        // Scratch
        this.blockScratch = new int[EngineSetting.AXIS_COUNT];
        this.cellScratch = new int[EngineSetting.AXIS_COUNT];
    }
}
