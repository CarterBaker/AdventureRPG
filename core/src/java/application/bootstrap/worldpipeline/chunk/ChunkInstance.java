package application.bootstrap.worldpipeline.chunk;

import java.util.concurrent.atomic.AtomicLong;

import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import application.bootstrap.geometrypipeline.quadpacket.QuadPacketInstance;
import application.bootstrap.geometrypipeline.quadpacket.QuadRecordUtility;
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
     * and reset before reuse. Its subchunks' records are the only CPU copy of
     * its terrain: merge() marks them final by bumping a globally unique
     * mergeVersion so megas never confuse occupants, and assemble() appends
     * them into a caller's staging packet for upload, each bucket grown at
     * most once; neighbors
     * are reconfigured in place, and tideSurfaceLevels records the tide the
     * ocean was last written against. It holds every tree that reaches it
     * from the moment it generates until it is reset, when TreeManager lets
     * them go, and draws them into a tree packet of its own, apart from its
     * terrain, so a tree that changes never rebuilds the ground.
     */

    // Internal
    private ChunkDataSyncContainer chunkDataSyncContainer;
    private SubChunkInstance[] subChunks;
    private ChunkNeighborHandle chunkNeighbors;
    private WorldItemInstancePaletteHandle worldItemInstancePaletteHandle;
    private GenerationCacheStruct terrainCache;
    private TreePaletteHandle treePaletteHandle;
    private DynamicPacketInstance treePacketInstance;
    private TreeManager treeManager;

    // Settings
    private int subChunkSpan;

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
        this.treePacketInstance = create(DynamicPacketInstance.class);

        this.subChunks = new SubChunkInstance[EngineSetting.WORLD_HEIGHT];
        for (short i = 0; i < EngineSetting.WORLD_HEIGHT; i++)
            subChunks[i] = create(SubChunkInstance.class);

        // Settings
        this.subChunkSpan = EngineSetting.CHUNK_SIZE * EngineSetting.SUB_BLOCK_DIVISIONS;

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
                coordinate);

        this.treePacketInstance.constructor(vaoHandle);

        for (byte subChunkCoordinate = 0; subChunkCoordinate < EngineSetting.WORLD_HEIGHT; subChunkCoordinate++)
            subChunks[subChunkCoordinate].constructor(
                    worldRenderManager,
                    worldHandle,
                    subChunkCoordinate,
                    airBlockId,
                    blockManager);

        this.chunkNeighbors.reconfigure(coordinate, this, activeChunks);
    }

    // Reset \\

    public void reset() {
        chunkDataSyncContainer.resetData();
        worldItemInstancePaletteHandle.clear();
        treeManager.releaseTrees(treePaletteHandle);
        treePacketInstance.clear();
        terrainCache.invalidate();
        tideSurfaceLevels = EngineSetting.OCEAN_TIDE_UNAPPLIED;

        for (SubChunkInstance subChunk : subChunks)
            subChunk.reset();
    }

    // Geometry \\

    public void merge() {
        mergeVersion = MERGE_VERSION_SEQUENCE.incrementAndGet();
    }

    // Every subchunk's records shifted from its own origin to the chunk's — callers hold the chunk's lock
    public void assemble(QuadPacketInstance staging) {

        staging.clear();

        for (int i = 0; i < subChunks.length; i++)
            staging.reserve(subChunks[i].getQuadPacketInstance());

        staging.commitReserve();

        for (int i = 0; i < subChunks.length; i++)
            staging.merge(
                    subChunks[i].getQuadPacketInstance(),
                    QuadRecordUtility.packPosition(0, i * subChunkSpan, 0));

        staging.finish();
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

    public DynamicPacketInstance getTreePacketInstance() {
        return treePacketInstance;
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