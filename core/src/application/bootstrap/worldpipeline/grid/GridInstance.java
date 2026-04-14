package application.bootstrap.worldpipeline.grid;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.gridslot.GridSlotHandle;
import application.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldrendermanager.RenderType;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class GridInstance extends InstancePackage {

    /*
     * The active spatial grid for a single focal entity. Owns the load order,
     * slot handles, active chunks, active mega chunks, pending load and unload
     * requests, and the render queues for this grid. Render queues map chunk
     * world coordinates directly to their GridSlotHandle — populated at rebuild
     * time so no reverse lookup is needed at render time. rebuildRenderQueue()
     * is called internally when the focal entity crosses a chunk boundary. The
     * window this grid renders into is stored here so FrustumCullingSystem reads
     * the correct camera per grid independently.
     */

    // Focal
    private EntityInstance focalEntity;

    // Window
    private WindowInstance windowInstance;

    // Grid
    private int totalSlots;
    private long[] loadOrder;
    private LongOpenHashSet gridCoordinates;
    private Long2ObjectOpenHashMap<GridSlotHandle> gridSlots;
    private float radiusSquared;

    // Active State
    private long activeChunkCoordinate;

    // Chunk State
    private Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;
    private Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks;
    private LongLinkedOpenHashSet loadRequests;
    private LongLinkedOpenHashSet unloadRequests;

    // Render Queues — chunk/mega world coordinate → slot handle
    private Long2ObjectLinkedOpenHashMap<GridSlotHandle> chunkRenderQueue;
    private Long2ObjectLinkedOpenHashMap<GridSlotHandle> megaRenderQueue;

    // Settings
    private int batchedChunks;

    // Scan cursor
    private int scanCursor;

    // Constructor \\

    public void constructor(
            EntityInstance focalEntity,
            WindowInstance windowInstance,
            int totalSlots,
            long[] loadOrder,
            LongOpenHashSet gridCoordinates,
            Long2ObjectOpenHashMap<GridSlotHandle> gridSlots,
            float radiusSquared,
            int maxChunks) {

        // Focal
        this.focalEntity = focalEntity;

        // Window
        this.windowInstance = windowInstance;

        // Grid
        this.totalSlots = totalSlots;
        this.loadOrder = loadOrder;
        this.gridCoordinates = gridCoordinates;
        this.gridSlots = gridSlots;
        this.radiusSquared = radiusSquared;

        // Active State
        this.activeChunkCoordinate = Coordinate2Long.pack(-1, -1);

        // Chunk State
        this.activeChunks = new Long2ObjectLinkedOpenHashMap<>(maxChunks);
        this.activeMegaChunks = new Long2ObjectLinkedOpenHashMap<>();
        this.loadRequests = new LongLinkedOpenHashSet();
        this.unloadRequests = new LongLinkedOpenHashSet();

        // Render Queues
        this.chunkRenderQueue = new Long2ObjectLinkedOpenHashMap<>();
        this.megaRenderQueue = new Long2ObjectLinkedOpenHashMap<>();

        // Settings
        this.batchedChunks = EngineSetting.MEGA_CHUNK_SIZE * EngineSetting.MEGA_CHUNK_SIZE;

        this.scanCursor = 0;
    }

    // Render Queue \\

    private void rebuildRenderQueue() {

        chunkRenderQueue.clear();
        megaRenderQueue.clear();

        for (int i = 0; i < totalSlots; i++) {

            long gridCoordinate = loadOrder[i];
            GridSlotHandle slot = gridSlots.get(gridCoordinate);
            long chunkCoordinate = getChunkCoordinateForSlot(gridCoordinate);

            queueChunk(slot, chunkCoordinate);

            if (slot.getDetailLevel().renderMode == RenderType.BATCHED)
                queueMega(slot, chunkCoordinate);
        }
    }

    private void queueChunk(GridSlotHandle slot, long chunkCoordinate) {

        long megaCoordinate = Coordinate2Long.toMegaChunkCoordinate(chunkCoordinate);

        if (megaRenderQueue.containsKey(megaCoordinate))
            return;

        chunkRenderQueue.put(chunkCoordinate, slot);
    }

    private void queueMega(GridSlotHandle slot, long chunkCoordinate) {

        long megaCoordinate = Coordinate2Long.toMegaChunkCoordinate(chunkCoordinate);

        if (chunkCoordinate != megaCoordinate)
            return;

        ObjectArrayList<GridSlotHandle> coveredSlots = slot.getCoveredSlots();

        if (coveredSlots.size() != batchedChunks)
            return;

        megaRenderQueue.put(megaCoordinate, slot);

        for (int i = 0; i < coveredSlots.size(); i++) {
            long coveredChunk = getChunkCoordinateForSlot(coveredSlots.get(i).getGridCoordinate());
            chunkRenderQueue.remove(coveredChunk);
        }
    }

    // Active State \\

    public boolean updateActiveChunkCoordinate() {

        long entityChunkCoordinate = focalEntity
                .getWorldPositionStruct()
                .getChunkCoordinate();

        if (activeChunkCoordinate == entityChunkCoordinate)
            return false;

        activeChunkCoordinate = entityChunkCoordinate;
        rebuildRenderQueue();
        return true;
    }

    public long getActiveChunkCoordinate() {
        return activeChunkCoordinate;
    }

    // Scan Iteration \\

    public GridSlotHandle getNextScanSlot() {

        if (scanCursor >= totalSlots)
            scanCursor = 0;

        long gridCoordinate = loadOrder[scanCursor];
        scanCursor++;

        return gridSlots.get(gridCoordinate);
    }

    // Computed Slot Lookups \\

    public long getChunkCoordinateForSlot(long gridCoordinate) {
        long raw = Coordinate2Long.add(activeChunkCoordinate, gridCoordinate);
        return WorldWrapUtility.wrapAroundWorld(getWorldHandle(), raw);
    }

    public long getMegaCoordinateForSlot(long gridCoordinate) {
        return Coordinate2Long.toMegaChunkCoordinate(getChunkCoordinateForSlot(gridCoordinate));
    }

    public GridSlotHandle getGridSlotForChunk(long chunkCoordinate) {
        long gridCoordinate = Coordinate2Long.subtract(chunkCoordinate, activeChunkCoordinate);
        return gridSlots.get(gridCoordinate);
    }

    // Accessible \\

    public EntityInstance getFocalEntity() {
        return focalEntity;
    }

    public WindowInstance getWindowInstance() {
        return windowInstance;
    }

    public WorldHandle getWorldHandle() {
        return focalEntity.getWorldHandle();
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public long[] getLoadOrder() {
        return loadOrder;
    }

    public long getGridCoordinate(int i) {
        return loadOrder[i];
    }

    public LongOpenHashSet getGridCoordinates() {
        return gridCoordinates;
    }

    public GridSlotHandle getGridSlot(long gridCoordinate) {
        return gridSlots.get(gridCoordinate);
    }

    public float getRadiusSquared() {
        return radiusSquared;
    }

    public Long2ObjectLinkedOpenHashMap<ChunkInstance> getActiveChunks() {
        return activeChunks;
    }

    public Long2ObjectLinkedOpenHashMap<MegaChunkInstance> getActiveMegaChunks() {
        return activeMegaChunks;
    }

    public LongLinkedOpenHashSet getLoadRequests() {
        return loadRequests;
    }

    public LongLinkedOpenHashSet getUnloadRequests() {
        return unloadRequests;
    }

    public Long2ObjectLinkedOpenHashMap<GridSlotHandle> getChunkRenderQueue() {
        return chunkRenderQueue;
    }

    public Long2ObjectLinkedOpenHashMap<GridSlotHandle> getMegaRenderQueue() {
        return megaRenderQueue;
    }
}