package application.bootstrap.entitypipeline.pathfindingmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.pathfinding.PathLayer;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worlditemmanager.WorldItemSpaceSystem;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.extras.Coordinate3Long;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

class PathTerrainBranch extends BranchPackage {

    /*
     * Reads the world for PathSearchBranch as block columns of sub-block
     * layers, X and Z measured from the corner of the chunk a search begins
     * in and free to run into its neighbors across the world's wrap, layers
     * counted up from the bottom of the world. A layer is SOLID when anything
     * solid fills one of its four sub-cells, LIQUID when water does, and OPEN
     * otherwise. begin() fixes the frame and the body's height in layers, and
     * gathers every solid item box and tree wood box within the search's reach
     * once, marking each layer they fill as solid, exactly as collision stops
     * a body on them. resolveFloor() is the one place that decides where a
     * body stepping into a column comes to stand, each column resolved once a
     * search from each layer it is stepped into from, and hasHeadroom()
     * whether a climb has room above the column it leaves. A chunk not yet generated
     * reads as solid, so no way is found through ground that is not there yet.
     * The last chunk and the last block cell read are kept, as a search reads
     * the same few over and over.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private WorldItemSpaceSystem worldItemSpaceSystem;
    private TreeManager treeManager;

    // Settings
    private int divisions;
    private float subBlockSize;
    private int worldTopLayer;
    private int[] layerMasks;

    // Frame
    private WorldHandle worldHandle;
    private long frameChunkCoordinate;
    private int clearance;

    // Chunk Cache
    private boolean chunkCached;
    private int cachedChunkX;
    private int cachedChunkZ;
    private ChunkInstance cachedChunk;

    // Cell Cache
    private boolean cellCached;
    private int cachedCellX;
    private int cachedCellY;
    private int cachedCellZ;
    private int cachedSolidMask;
    private int cachedLiquidMask;

    // Obstacles
    private FloatArrayList obstacleBoxes;
    private LongOpenHashSet obstacleLayers;
    private int boxStride;

    // Floors — each column's floor as reached from a layer, resolved once a search
    private Long2IntOpenHashMap step2Floor;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.divisions = SubBlockUtility.DIVISIONS;
        this.subBlockSize = SubBlockUtility.SIZE;
        this.worldTopLayer = EngineSetting.WORLD_HEIGHT * EngineSetting.CHUNK_SIZE * SubBlockUtility.DIVISIONS;
        this.layerMasks = new int[SubBlockUtility.DIVISIONS];

        for (int octantY = 0; octantY < SubBlockUtility.DIVISIONS; octantY++)
            layerMasks[octantY] = SubBlockUtility.layerMask(octantY);

        // Obstacles
        this.obstacleBoxes = new FloatArrayList();
        this.obstacleLayers = new LongOpenHashSet(EngineSetting.PATH_OBSTACLE_CAPACITY);
        this.boxStride = EngineSetting.AXIS_COUNT * 2;

        // Floors
        this.step2Floor = new Long2IntOpenHashMap(EngineSetting.PATH_NODE_CAPACITY);
        this.step2Floor.defaultReturnValue(EngineSetting.PATH_FLOOR_UNRESOLVED);
    }

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
        this.worldItemSpaceSystem = get(WorldItemSpaceSystem.class);
        this.treeManager = get(TreeManager.class);
    }

    // Frame \\

    // Sets the search's frame to the entity's chunk and its body's height, and marks every obstacle within reach
    void begin(EntityInstance entity, int startX, int startLayer, int startZ) {

        this.worldHandle = entity.getWorldHandle();
        this.frameChunkCoordinate = entity.getWorldPositionStruct().getChunkCoordinate();
        this.clearance = Math.max(1, (int) Math.ceil(
                entity.getSize().y / subBlockSize - EngineSetting.PATH_CLEARANCE_EPSILON));
        this.chunkCached = false;
        this.cellCached = false;

        step2Floor.clear();
        gatherObstacles(startX, startLayer, startZ);
    }

    // Every solid item box and tree wood box the search could reach, each layer they fill marked solid
    private void gatherObstacles(int startX, int startLayer, int startZ) {

        int radius = EngineSetting.PATH_SEARCH_RADIUS_BLOCKS;
        int span = EngineSetting.PATH_SEARCH_HEIGHT_LAYERS;
        float minX = startX - radius;
        float minZ = startZ - radius;
        float maxX = startX + radius + 1;
        float maxZ = startZ + radius + 1;
        float minY = Math.max(0, startLayer - span) * subBlockSize;
        float maxY = (startLayer + span + 1) * subBlockSize;

        worldItemSpaceSystem.collectSolidBoxes(
                worldHandle, frameChunkCoordinate, minX, minY, minZ, maxX, maxY, maxZ, obstacleBoxes);
        treeManager.collectWoodBoxes(
                worldHandle, frameChunkCoordinate, minX, minY, minZ, maxX, maxY, maxZ, obstacleBoxes);

        obstacleLayers.clear();

        float[] boxes = obstacleBoxes.elements();
        float skin = EngineSetting.COLLISION_SKIN_BLOCKS;

        for (int box = 0; box < obstacleBoxes.size(); box += boxStride) {

            int max = box + EngineSetting.AXIS_COUNT;
            int firstX = (int) Math.floor(Math.max(minX, boxes[box + EngineSetting.AXIS_X] + skin));
            int lastX = (int) Math.floor(Math.min(maxX - skin, boxes[max + EngineSetting.AXIS_X] - skin));
            int firstLayer = (int) Math.floor(Math.max(minY, boxes[box + EngineSetting.AXIS_Y] + skin) / subBlockSize);
            int lastLayer = (int) Math.floor(Math.min(maxY - skin, boxes[max + EngineSetting.AXIS_Y] - skin)
                    / subBlockSize);
            int firstZ = (int) Math.floor(Math.max(minZ, boxes[box + EngineSetting.AXIS_Z] + skin));
            int lastZ = (int) Math.floor(Math.min(maxZ - skin, boxes[max + EngineSetting.AXIS_Z] - skin));

            for (int x = firstX; x <= lastX; x++)
                for (int z = firstZ; z <= lastZ; z++)
                    for (int layer = firstLayer; layer <= lastLayer; layer++)
                        obstacleLayers.add(Coordinate3Long.pack(x, layer, z));
        }

        obstacleBoxes.clear();
    }

    // Floor \\

    // The layer a body stepping into a column from a layer comes to stand on — the highest it can climb to or drop
    // onto with room for its whole height all the way across, PATH_LAYER_NONE when there is none or water lies below
    int resolveFloor(int x, int z, int fromLayer) {

        long step = Coordinate3Long.pack(x, fromLayer, z);
        int floor = step2Floor.get(step);

        if (floor == EngineSetting.PATH_FLOOR_UNRESOLVED) {
            floor = scanFloor(x, z, fromLayer);
            step2Floor.put(step, floor);
        }

        return floor;
    }

    private int scanFloor(int x, int z, int fromLayer) {

        int climbTop = fromLayer + EngineSetting.PATH_CLIMB_LAYERS;
        int bottom = fromLayer - EngineSetting.PATH_DROP_LAYERS - 1;
        int run = 0;

        for (int layer = climbTop + clearance - 1; layer >= bottom; layer--) {

            PathLayer sample = sample(x, layer, z);

            if (sample == PathLayer.OPEN) {
                run++;
                continue;
            }

            if (sample == PathLayer.LIQUID)
                return EngineSetting.PATH_LAYER_NONE;

            int floor = layer + 1;

            if (floor <= climbTop && run >= Math.max(clearance, fromLayer + clearance - floor))
                return floor;

            run = 0;
        }

        return EngineSetting.PATH_LAYER_NONE;
    }

    // True when the column a body climbs out of is open above its head all the way up to its height on the new floor
    boolean hasHeadroom(int x, int z, int fromLayer, int toLayer) {

        for (int layer = fromLayer + clearance; layer < toLayer + clearance; layer++)
            if (sample(x, layer, z) != PathLayer.OPEN)
                return false;

        return true;
    }

    // Sample \\

    private PathLayer sample(int x, int layer, int z) {

        if (layer < 0)
            return PathLayer.SOLID;

        if (layer >= worldTopLayer)
            return PathLayer.OPEN;

        if (!obstacleLayers.isEmpty() && obstacleLayers.contains(Coordinate3Long.pack(x, layer, z)))
            return PathLayer.SOLID;

        readCell(x, layer / divisions, z);

        int layerMask = layerMasks[layer % divisions];

        if ((cachedSolidMask & layerMask) != SubBlockUtility.MASK_EMPTY)
            return PathLayer.SOLID;

        if ((cachedLiquidMask & layerMask) != SubBlockUtility.MASK_EMPTY)
            return PathLayer.LIQUID;

        return PathLayer.OPEN;
    }

    // The octants of one block cell that are solid and that are liquid, kept until another cell is read
    private void readCell(int cellX, int cellY, int cellZ) {

        if (cellCached && cellX == cachedCellX && cellY == cachedCellY && cellZ == cachedCellZ)
            return;

        cellCached = true;
        cachedCellX = cellX;
        cachedCellY = cellY;
        cachedCellZ = cellZ;
        cachedSolidMask = SubBlockUtility.MASK_EMPTY;
        cachedLiquidMask = SubBlockUtility.MASK_EMPTY;

        int chunkSize = EngineSetting.CHUNK_SIZE;
        ChunkInstance chunk = resolveChunk(Math.floorDiv(cellX, chunkSize), Math.floorDiv(cellZ, chunkSize));

        if (chunk == null) {
            cachedSolidMask = SubBlockUtility.MASK_FULL;
            return;
        }

        SubChunkInstance subChunk = chunk.getSubChunk(cellY / chunkSize);
        int cellXYZ = Coordinate3Int.pack(
                Math.floorMod(cellX, chunkSize),
                cellY % chunkSize,
                Math.floorMod(cellZ, chunkSize));
        int mask = subChunk.getSubBlockMask(cellXYZ);

        if (mask == SubBlockUtility.MASK_EMPTY)
            return;

        BlockHandle blockHandle = blockManager.getBlockHandleFromBlockID(subChunk.getBlock(cellXYZ));

        switch (blockHandle.getGeometry()) {
            case NONE -> {
            }
            case LIQUID -> cachedLiquidMask = mask;
            default -> cachedSolidMask = mask;
        }
    }

    // The chunk a number of chunks off the frame's own, across the world's wrap — null until it has generated
    private ChunkInstance resolveChunk(int offsetX, int offsetZ) {

        if (chunkCached && offsetX == cachedChunkX && offsetZ == cachedChunkZ)
            return cachedChunk;

        long chunkCoordinate = WorldWrapUtility.wrapAroundWorld(
                worldHandle,
                Coordinate2Long.add(frameChunkCoordinate, offsetX, offsetZ));
        ChunkInstance chunk = worldStreamManager.getChunkInstance(chunkCoordinate);

        chunkCached = true;
        cachedChunkX = offsetX;
        cachedChunkZ = offsetZ;
        cachedChunk = chunk != null && chunk.getChunkDataSyncContainer().hasData(ChunkData.GENERATION_DATA)
                ? chunk
                : null;

        return cachedChunk;
    }
}
