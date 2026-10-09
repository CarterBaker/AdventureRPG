package application.bootstrap.worldpipeline.chunk;

import java.util.concurrent.atomic.AtomicLong;

import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.tree.TreePaletteHandle;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.GenerationCacheStruct;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstancePaletteHandle;
import application.bootstrap.worldpipeline.worldrendermanager.RenderType;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderInstance;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public class ChunkInstance extends WorldRenderInstance {

    /*
     * One loaded chunk column owning its subchunks. Pooled by ChunkQueueManager
     * and reset before reuse. merge() combines subchunk packets and bumps a
     * globally unique mergeVersion so megas never confuse occupants; neighbors
     * are reconfigured in place, and tideSurfaceLevels records the tide the
     * ocean was last written against. It holds every tree that reaches it
     * from the moment it generates until it is reset, when TreeManager lets
     * them go.
     */

    // Internal
    private ChunkDataSyncContainer chunkDataSyncContainer;
    private SubChunkInstance[] subChunks;
    private ChunkNeighborHandle chunkNeighbors;
    private WorldItemInstancePaletteHandle worldItemInstancePaletteHandle;
    private GenerationCacheStruct terrainCache;
    private TreePaletteHandle treePaletteHandle;
    private TreeManager treeManager;

    // Scratch — pre-allocated, reused per merge call
    private int[] vertPositionArray;
    private float[] mergeOffsetValues;

    // Settings
    private int chunkSize;

    // Merge Version
    private static final AtomicLong MERGE_VERSION_SEQUENCE = new AtomicLong();
    private volatile long mergeVersion;

    // Tide
    private int tideSurfaceLevels;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.chunkDataSyncContainer = create(ChunkDataSyncContainer.class);
        this.worldItemInstancePaletteHandle = create(WorldItemInstancePaletteHandle.class);
        this.worldItemInstancePaletteHandle.constructor();
        this.terrainCache = new GenerationCacheStruct();
        this.chunkNeighbors = create(ChunkNeighborHandle.class);
        this.treePaletteHandle = create(TreePaletteHandle.class);
        this.treePaletteHandle.constructor();

        this.subChunks = new SubChunkInstance[EngineSetting.WORLD_HEIGHT];
        for (short i = 0; i < EngineSetting.WORLD_HEIGHT; i++)
            subChunks[i] = create(SubChunkInstance.class);

        // Scratch
        this.vertPositionArray = new int[] { 1 };
        this.mergeOffsetValues = new float[1];

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;

        // Tide
        this.tideSurfaceLevels = EngineSetting.OCEAN_TIDE_UNAPPLIED;

        super.create();
    }

    // Constructor \\

    public void constructor(
            WorldRenderManager worldRenderManager,
            WorldHandle worldHandle,
            long coordinate,
            VAOHandle vaoHandle,
            short airBlockId,
            BlockManager blockManager,
            TreeManager treeManager,
            Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks) {

        this.treeManager = treeManager;

        super.constructor(
                worldRenderManager,
                worldHandle,
                RenderType.INDIVIDUAL,
                coordinate,
                vaoHandle);

        for (byte subChunkCoordinate = 0; subChunkCoordinate < EngineSetting.WORLD_HEIGHT; subChunkCoordinate++)
            subChunks[subChunkCoordinate].constructor(
                    worldRenderManager,
                    worldHandle,
                    subChunkCoordinate,
                    vaoHandle,
                    airBlockId,
                    blockManager);

        this.chunkNeighbors.reconfigure(coordinate, this, activeChunks);
    }

    // Reset \\

    public void reset() {
        chunkDataSyncContainer.resetData();
        getDynamicPacket().clear();
        worldItemInstancePaletteHandle.clear();
        treeManager.releaseTrees(treePaletteHandle);
        terrainCache.invalidate();
        tideSurfaceLevels = EngineSetting.OCEAN_TIDE_UNAPPLIED;

        for (SubChunkInstance subChunk : subChunks)
            subChunk.reset();
    }

    // Geometry \\

    public boolean merge() {

        boolean success = true;
        getDynamicPacket().clear();

        for (SubChunkInstance subChunk : subChunks) {
            mergeOffsetValues[0] = subChunk.getCoordinate() * chunkSize;
            if (!getDynamicPacket().merge(
                    subChunk.getDynamicPacketInstance(),
                    vertPositionArray,
                    mergeOffsetValues))
                success = false;
        }

        if (getDynamicPacket().hasModels())
            getDynamicPacket().setReady();
        else
            getDynamicPacket().unlock();

        mergeVersion = MERGE_VERSION_SEQUENCE.incrementAndGet();

        return success;
    }

    // Accessible \\

    public ChunkDataSyncContainer getChunkDataSyncContainer() {
        return chunkDataSyncContainer;
    }

    public SubChunkInstance[] getSubChunks() {
        return subChunks;
    }

    public SubChunkInstance getSubChunk(int subChunkCoordinate) {
        return subChunks[subChunkCoordinate];
    }

    public ChunkNeighborHandle getChunkNeighbors() {
        return chunkNeighbors;
    }

    public WorldItemInstancePaletteHandle getWorldItemInstancePaletteHandle() {
        return worldItemInstancePaletteHandle;
    }

    public TreePaletteHandle getTreePaletteHandle() {
        return treePaletteHandle;
    }

    public GenerationCacheStruct getTerrainCache() {
        return terrainCache;
    }

    public long getMergeVersion() {
        return mergeVersion;
    }

    public int getTideSurfaceLevels() {
        return tideSurfaceLevels;
    }

    public void setTideSurfaceLevels(int tideSurfaceLevels) {
        this.tideSurfaceLevels = tideSurfaceLevels;
    }
}