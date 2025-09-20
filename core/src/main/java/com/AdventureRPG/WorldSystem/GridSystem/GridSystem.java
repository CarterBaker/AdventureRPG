package com.AdventureRPG.WorldSystem.GridSystem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Coordinate2Int;
import com.AdventureRPG.Util.Vector2Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.WorldTick;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;
import com.AdventureRPG.WorldSystem.Chunks.ChunkSystem;
import com.AdventureRPG.WorldSystem.Chunks.NeighborStatus;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class GridSystem {

    // Debug
    private final boolean debug = false; // TODO: Debug line

    // Game Manager
    private final Settings settings;
    private final WorldSystem worldSystem;
    private final ChunkSystem chunkSystem;
    private final WorldTick worldTick;
    private Camera camera;

    // Settings
    private int maxRenderDistance;
    private final int MAX_CHUNK_LOADS_PER_FRAME;
    private final int MAX_CHUNK_LOADS_PER_TICK;

    // Position Tracking
    private long currentChunk;
    private int currentChunkX, currentChunkY;

    // Chunk Tracking
    private long[] loadOrder;
    private final float loadFactor;
    private int totalChunks;
    private LongOpenHashSet gridCoordinates;
    private LongOpenHashSet chunkCoordinates;
    private Long2LongOpenHashMap gridToChunkMap;
    private Long2LongOpenHashMap chunkToGridMap;
    private Long2ObjectOpenHashMap<Chunk> loadedChunks;
    private Long2ObjectOpenHashMap<BoundingBox> chunkBounds;
    private Long2ObjectOpenHashMap<ModelInstance> modelInstances;

    // Queue System
    private Long2ObjectOpenHashMap<Chunk> unloadQueue;
    private LongArrayFIFOQueue loadQueue;
    private LongArrayFIFOQueue generateQueue;
    private LongOpenHashSet generateQueueCheck;
    private LongArrayFIFOQueue assessmentQueue;
    private LongOpenHashSet assessmentQueueCheck;
    private LongArrayFIFOQueue buildQueue;
    private LongOpenHashSet buildQueueCheck;

    private int loadedChunksThisFrame;
    private int loadedChunksThisTick;

    // Base \\

    public GridSystem(WorldSystem WorldSystem) {

        // Chunk System
        this.settings = WorldSystem.settings;
        this.worldSystem = WorldSystem;
        this.chunkSystem = worldSystem.chunkSystem;
        this.worldTick = WorldSystem.worldTick;

        // Settings
        this.maxRenderDistance = settings.maxRenderDistance;
        this.MAX_CHUNK_LOADS_PER_FRAME = settings.MAX_CHUNK_LOADS_PER_FRAME;
        this.MAX_CHUNK_LOADS_PER_TICK = settings.MAX_CHUNK_LOADS_PER_TICK;

        // Position Tracking
        this.currentChunk = Coordinate2Int.pack(-1, -1);
        this.currentChunkX = -1;
        this.currentChunkY = -1;

        // Chunk Tracking
        this.loadFactor = 0.75f;
        this.totalChunks = maxRenderDistance * maxRenderDistance;
        int initialCapacity = (int) Math.ceil(totalChunks / loadFactor);
        this.loadOrder = new long[totalChunks];
        this.gridCoordinates = new LongOpenHashSet(totalChunks);
        this.chunkCoordinates = new LongOpenHashSet(totalChunks);
        this.gridToChunkMap = new Long2LongOpenHashMap(initialCapacity, loadFactor);
        this.chunkToGridMap = new Long2LongOpenHashMap(initialCapacity, loadFactor);
        this.loadedChunks = new Long2ObjectOpenHashMap<>(totalChunks);
        this.chunkBounds = new Long2ObjectOpenHashMap<>(totalChunks);
        this.modelInstances = new Long2ObjectOpenHashMap<>(totalChunks);

        // Queue System
        this.unloadQueue = new Long2ObjectOpenHashMap<>(totalChunks);
        this.loadQueue = new LongArrayFIFOQueue(totalChunks);
        this.generateQueue = new LongArrayFIFOQueue(totalChunks);
        this.generateQueueCheck = new LongOpenHashSet(totalChunks);
        this.assessmentQueue = new LongArrayFIFOQueue(totalChunks);
        this.assessmentQueueCheck = new LongOpenHashSet(totalChunks);
        this.buildQueue = new LongArrayFIFOQueue(totalChunks);
        this.buildQueueCheck = new LongOpenHashSet(totalChunks);

        this.loadedChunksThisFrame = 0;
        this.loadedChunksThisTick = 0;
    }

    public void awake() {

        this.camera = worldSystem.gameManager.playerSystem.camera.get();
        rebuildGrid();
    }

    public void start() {

    }

    public void update() {
        updateQueue();
    }

    public void render(ModelBatch modelBatch) {
        renderChunks(modelBatch);
    }

    public void dispose() {

        for (int i = 0; i < totalChunks; i++) {

            long gridCoordinate = loadOrder[i];

            long chunkCoordinate = gridToChunkMap.get(gridCoordinate);
            Chunk loadedChunk = loadedChunks.get(chunkCoordinate);

            if (loadedChunk != null)
                loadedChunk.dispose();
        }
    }

    // Awake \\

    // Main rebuild method
    public void rebuildGrid() {

        maxRenderDistance = settings.maxRenderDistance;
        float radius = calculateRadius();

        List<ChunkDistance> tempList = collectChunkDistances(radius);

        totalChunks = tempList.size();
        initializeDataStructures(totalChunks);
        fillGridCoordinates(tempList);
        assignLoadOrder(tempList);
        createBoundingBoxes();
    }

    // Calculate radius
    private float calculateRadius() {
        return settings.maxRenderDistance / 2f;
    }

    // Collect all coordinates inside a circle
    private List<ChunkDistance> collectChunkDistances(float radius) {

        float radiusSquared = radius * radius;
        List<ChunkDistance> list = new ArrayList<>(); // TODO: Check GC creation

        for (int x = -(maxRenderDistance / 2); x < maxRenderDistance / 2; x++) {

            for (int y = -(maxRenderDistance / 2); y < maxRenderDistance / 2; y++) {

                float distSquared = (x * x) + (y * y);

                if (distSquared <= radiusSquared)
                    list.add(new ChunkDistance(Coordinate2Int.pack(x, y), distSquared));
            }
        }

        return list;
    }

    // Inner class
    private static class ChunkDistance {

        long coord;
        float distSquared;

        ChunkDistance(long c, float d2) {

            coord = c;
            distSquared = d2;
        }
    }

    // Initialize all data structures
    private void initializeDataStructures(int totalChunks) {

        int initialCapacity = (int) Math.ceil(totalChunks / loadFactor);

        // Chunk Tracking
        this.loadOrder = new long[totalChunks];
        this.gridCoordinates = new LongOpenHashSet(totalChunks);
        this.chunkCoordinates = new LongOpenHashSet(totalChunks);
        this.gridToChunkMap = new Long2LongOpenHashMap(initialCapacity, loadFactor);
        this.chunkToGridMap = new Long2LongOpenHashMap(initialCapacity, loadFactor);
        this.loadedChunks = new Long2ObjectOpenHashMap<>(totalChunks);
        this.chunkBounds = new Long2ObjectOpenHashMap<>(totalChunks);
        this.modelInstances = new Long2ObjectOpenHashMap<>(totalChunks);

        // Queue System
        this.unloadQueue = new Long2ObjectOpenHashMap<>(totalChunks);
        this.loadQueue = new LongArrayFIFOQueue(totalChunks);
        this.generateQueue = new LongArrayFIFOQueue(totalChunks);
        this.generateQueueCheck = new LongOpenHashSet(totalChunks);
        this.assessmentQueue = new LongArrayFIFOQueue(totalChunks);
        this.assessmentQueueCheck = new LongOpenHashSet(totalChunks);
        this.buildQueue = new LongArrayFIFOQueue(totalChunks);
        this.buildQueueCheck = new LongOpenHashSet(totalChunks);
    }

    // Fill gridCoordinates set
    private void fillGridCoordinates(List<ChunkDistance> tempList) {

        for (ChunkDistance cd : tempList)
            gridCoordinates.add(cd.coord);
    }

    // Sort by distance and assign load order
    private void assignLoadOrder(List<ChunkDistance> tempList) {

        tempList.sort(Comparator.comparingDouble(cd -> cd.distSquared));

        for (int i = 0; i < tempList.size(); i++)
            loadOrder[i] = tempList.get(i).coord;
    }

    private void createBoundingBoxes() {

        int CHUNK_SIZE = settings.CHUNK_SIZE;
        int WORLD_HEIGHT = settings.WORLD_HEIGHT;

        for (Long gridCoordinate : gridCoordinates) {

            int x = Coordinate2Int.unpackX(gridCoordinate);
            int z = Coordinate2Int.unpackY(gridCoordinate);

            int gridX = Coordinate2Int.unpackX(gridCoordinate);
            int gridZ = Coordinate2Int.unpackY(gridCoordinate);

            int worldX = gridX * CHUNK_SIZE;
            int worldZ = gridZ * CHUNK_SIZE;

            // TODO: Check GC creation
            Vector3 min = new Vector3(worldX, 0, worldZ);
            Vector3 max = new Vector3(worldX + CHUNK_SIZE,
                    WORLD_HEIGHT * CHUNK_SIZE,
                    worldZ + CHUNK_SIZE);

            BoundingBox boundingBox = new BoundingBox(min, max);
            chunkBounds.putIfAbsent(gridCoordinate, boundingBox);
        }
    }

    // Update \\

    private void updateQueue() {

        // Reset every frame
        loadedChunksThisFrame = 0;

        // Reset every tick
        if (worldTick.tick())
            loadedChunksThisTick = 0;

        ReceiveData();

        while (!totalProcessThisFrame() && hasQueue()) {

            QueueProcess next = pickNextQueue();

            if (next == null)
                break;

            while (!totalProcessThisFrame() && getQueueSize(next) > 0)
                if (!next.process(this))
                    break;
        }
    }

    private QueueProcess pickNextQueue() {

        QueueProcess priorityQueue = null;
        int priorityScore = 0;

        for (QueueProcess queue : QueueProcess.values()) {

            int queueScore = getQueueScore(queue);

            if (queueScore > priorityScore) {
                priorityQueue = queue;
                priorityScore = queueScore;
            }
        }

        return priorityQueue;
    }

    private int getQueueScore(QueueProcess process) {

        int queueSize = getQueueSize(process);

        if (queueSize == 0)
            return 0;

        return queueSize + process.getPriority() * 100;
    }

    // Render \\

    private void renderChunks(ModelBatch modelBatch) {

        int skipped = 0;

        for (int i = 0; i < totalChunks; i++) {

            long gridCoordinate = loadOrder[i];
            long chunkCoordinate = gridToChunkMap.getOrDefault(gridCoordinate, -1L);

            if (chunkCoordinate != -1L) {

                ModelInstance instance = modelInstances.get(chunkCoordinate);

                if (instance != null) {

                    BoundingBox bounds = chunkBounds.get(gridCoordinate);

                    if (bounds != null && camera.frustum.boundsInFrustum(bounds))
                        modelBatch.render(instance);

                    else
                        skipped++;
                }
            }
        }

        if (debug)
            System.out.println("Culled " + skipped + " chunks this frame");
    }

    // Main \\

    public void updateChunksInGrid(Vector2Int chunkCoordinate) {

        // Pack the coordinate
        int chunkCoordinateX = chunkCoordinate.x;
        int chunkCoordinateY = chunkCoordinate.y;
        long packedCoordinate = Coordinate2Int.pack(chunkCoordinateX, chunkCoordinateY);

        // Triple check if the chunk is already active
        if (packedCoordinate == currentChunk)
            return;

        // Set the current chunk first
        currentChunk = packedCoordinate;
        currentChunkX = chunkCoordinateX;
        currentChunkY = chunkCoordinateY;

        // Clear all collections
        clearQueue();

        // Determine which chunks need to be loaded where
        createQueue();
    }

    private void clearQueue() {

        // Clear mappings
        gridToChunkMap.clear();
        chunkToGridMap.clear();

        // Clear unload map
        unloadQueue.clear();

        // Clear all primitive queues
        loadQueue.clear();
        generateQueue.clear();
        generateQueueCheck.clear();
        assessmentQueue.clear();
        assessmentQueueCheck.clear();
        buildQueue.clear();
        buildQueueCheck.clear();

        // Reversely remove all computed Coordinate2Ints from unloadQueue for efficiency
        unloadQueue.putAll(loadedChunks);
    }

    private void createQueue() {

        // The grid is prebuilt and never changes
        for (int i = 0; i < totalChunks; i++) {

            long gridCoordinate = loadOrder[i];

            // Use the position in grid as an offset
            int offsetX = Coordinate2Int.unpackX(gridCoordinate);
            int offsetY = Coordinate2Int.unpackY(gridCoordinate);

            // Use the current chunks coordinates to compute necessary chunks
            int chunkX = currentChunkX + offsetX;
            int chunkY = currentChunkY + offsetY;

            // Pack the coordinates into a usable long value and wrap it
            long chunkCoordinate = Coordinate2Int.pack(chunkX, chunkY);
            chunkCoordinate = worldSystem.wrapAroundWorld(chunkCoordinate);

            // The very first step is to remap all the chunks to the correct position
            gridToChunkMap.put(gridCoordinate, chunkCoordinate);
            chunkToGridMap.put(chunkCoordinate, gridCoordinate);

            // Log all active chunk coordinates
            chunkCoordinates.add(chunkCoordinate);

            // Remove the computed coordinate from the unload queue
            unloadQueue.remove(chunkCoordinate);

            // Attempt to use the chunk coordinate to retrieve a loaded chunk
            Chunk loadedChunk = loadedChunks.get(chunkCoordinate);

            // If the chunk was loaded we move it to the new grid position
            if (loadedChunk != null) {

                loadedChunk.moveTo(gridCoordinate);
                loadedChunk.enqueue();
            }

            else // If the chunkCoordinate could not be found add it to the queue
                loadQueue.enqueue(gridCoordinate);
        }
    }

    // Unload \\

    private boolean unloadQueue() {

        int index = 0;
        var iterator = unloadQueue.long2ObjectEntrySet().fastIterator();

        while (iterator.hasNext() && processIsSafe(index)) {

            var entry = iterator.next();
            long chunkCoordinate = entry.getLongKey();
            Chunk loadedChunk = entry.getValue();

            if (loadedChunk == null)
                continue;

            // Remove from unloadQueue
            iterator.remove();
            loadedChunks.remove(chunkCoordinate);
            loadedChunk.dispose();

            // Increment counters
            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    // Load \\

    private boolean loadQueue() {

        // Return early if there is no room for loading new chunks
        if (loadedChunks.size() == totalChunks)
            return totalProcessThisFrame();

        int index = 0;

        // Capture queue size once to avoid changing bound while dequeuing
        int qSize = loadQueue.size();

        while (index < qSize && processIsSafe(index)) {

            long gridCoordinate = loadQueue.dequeueLong();
            long chunkCoordinate = gridToChunkMap.get(gridCoordinate);

            chunkSystem.requestLoad(chunkCoordinate);

            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    // Generate \\

    private boolean generateQueue() {

        int index = 0;

        int qSize = generateQueue.size();

        while (index < qSize && processIsSafe(index)) {

            long chunkCoordinate = dequeueGenerateQueue();
            Chunk loadedChunk = loadedChunks.get(chunkCoordinate);

            if (loadedChunk == null) {

                loadedChunks.remove(chunkCoordinate);
                index = incrementQueueTotal(index);

                continue;
            }

            chunkSystem.requestGenerate(loadedChunk);

            // Increment counters
            index = incrementQueueTotal(index);

        }

        return totalProcessThisFrame();
    }

    // Assessment \\

    private boolean assessmentQueue() {

        int index = 0;

        int qSize = assessmentQueue.size();

        while (index < qSize && processIsSafe(index)) {

            long chunkCoordinate = dequeueAssessmentQueue();
            Chunk loadedChunk = loadedChunks.get(chunkCoordinate);

            if (loadedChunk == null) {

                loadedChunks.remove(chunkCoordinate);
                index = incrementQueueTotal(index);

                continue;
            }

            loadedChunk.assessNeighbors();

            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    // Build \\

    private boolean buildQueue() {

        int index = 0;

        int qSize = buildQueue.size();

        while (index < qSize && processIsSafe(index)) {

            long chunkCoordinate = dequeueBuildQueue();
            Chunk loadedChunk = loadedChunks.get(chunkCoordinate);

            if (loadedChunk == null) {

                loadedChunks.remove(chunkCoordinate);
                index = incrementQueueTotal(index);

                continue;
            }

            if (loadedChunk.getNeighborStatus() != NeighborStatus.INCOMPLETE)
                chunkSystem.requestBuild(loadedChunk);

            index = incrementQueueTotal(index);
        }

        return totalProcessThisFrame();
    }

    // Thread Queue \\

    private void ReceiveData() {

        receiveLoadedChunks();
        receiveGeneratedChunks();
        receiveBuiltChunks();
    }

    private void receiveLoadedChunks() {

        while (chunkSystem.hasLoadedData()) {

            Chunk loadedChunk = chunkSystem.pollLoadedChunk();

            long chunkCoordinate = loadedChunk.coordinate;
            long gridCoordinate = chunkToGridMap.getOrDefault(chunkCoordinate, -1);

            if (gridCoordinate != -1) {

                loadedChunk.moveTo(gridCoordinate);
                loadedChunks.put(chunkCoordinate, loadedChunk);
                loadedChunk.enqueue();
            }

            else
                loadedChunk.dispose();
        }
    }

    private void receiveGeneratedChunks() {

        while (chunkSystem.hasGeneratedData()) {

            Chunk loadedChunk = chunkSystem.pollGeneratedChunk();

            System.out.println(Coordinate2Int.toString(loadedChunk.coordinate));

            long chunkCoordinate = loadedChunk.coordinate;
            long gridCoordinate = chunkToGridMap.getOrDefault(chunkCoordinate, -1);

            if (gridCoordinate != -1)
                loadedChunk.enqueue();

            else
                loadedChunk.dispose();
        }
    }

    private void receiveBuiltChunks() {

        while (chunkSystem.hasBuiltData()) {

            Chunk loadedChunk = chunkSystem.pollBuiltChunk();

            long chunkCoordinate = loadedChunk.coordinate;
            long gridCoordinate = chunkToGridMap.getOrDefault(chunkCoordinate, -1);

            if (gridCoordinate != -1)
                loadedChunk.buildChunkMesh();

            else
                loadedChunk.dispose();
        }
    }

    // Queue Utility \\

    private boolean processIsSafe(int index) {
        return loadedChunksThisFrame < MAX_CHUNK_LOADS_PER_FRAME &&
                loadedChunksThisTick < MAX_CHUNK_LOADS_PER_TICK;
    }

    private boolean totalProcessThisFrame() {
        return loadedChunksThisFrame >= MAX_CHUNK_LOADS_PER_FRAME ||
                loadedChunksThisTick >= MAX_CHUNK_LOADS_PER_TICK;
    }

    private int incrementQueueTotal(int index) {

        loadedChunksThisFrame++;
        loadedChunksThisTick++;

        return ++index;
    }

    public boolean hasQueue() {
        return unloadQueue.size() > 0 ||
                loadQueue.size() > 0 ||
                assessmentQueue.size() > 0 ||
                generateQueue.size() > 0 ||
                buildQueue.size() > 0;
    }

    public int totalQueueSize() {
        return unloadQueue.size() +
                loadQueue.size() +
                assessmentQueue.size() +
                generateQueue.size() +
                buildQueue.size();
    }

    public int getQueueSize(QueueProcess process) {
        return switch (process) {
            case Unload -> unloadQueue.size();
            case Load -> loadQueue.size();
            case Generate -> generateQueue.size();
            case Assessment -> assessmentQueue.size();
            case Build -> buildQueue.size();
        };
    }

    enum QueueProcess {

        Unload(5) {
            boolean process(GridSystem system) {
                return system.unloadQueue();
            }
        },

        Load(4) {
            boolean process(GridSystem system) {
                return system.loadQueue();
            }
        },

        Generate(3) {
            boolean process(GridSystem system) {
                return system.generateQueue();
            }
        },

        Assessment(2) {
            boolean process(GridSystem system) {
                return system.assessmentQueue();
            }
        },

        Build(1) {
            boolean process(GridSystem system) {
                return system.buildQueue();
            }
        };

        private final int priority;

        abstract boolean process(GridSystem system);

        QueueProcess(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    private long dequeueGenerateQueue() {

        long chunkCoordinate = generateQueue.dequeueLong();
        generateQueueCheck.remove(chunkCoordinate);

        return chunkCoordinate;
    }

    private long dequeueAssessmentQueue() {

        long chunkCoordinate = assessmentQueue.dequeueLong();
        assessmentQueueCheck.remove(chunkCoordinate);

        return chunkCoordinate;
    }

    private long dequeueBuildQueue() {

        long chunkCoordinate = buildQueue.dequeueLong();
        buildQueueCheck.remove(chunkCoordinate);

        return chunkCoordinate;
    }

    // External Queueing \\

    public void addToGenerateQueue(long chunkCoordinate) {

        if (generateQueueCheck.add(chunkCoordinate))
            generateQueue.enqueue(chunkCoordinate);
    }

    public void addToAssessmentQueue(long chunkCoordinate) {

        if (assessmentQueueCheck.add(chunkCoordinate))
            assessmentQueue.enqueue(chunkCoordinate);
    }

    public void addToBuildQueue(long chunkCoordinate) {

        if (buildQueueCheck.add(chunkCoordinate))
            buildQueue.enqueue(chunkCoordinate);
    }

    // Accessible \\

    public void addToModelInstances(long chunkCoordinate, ModelInstance modelInstance) {
        modelInstances.put(chunkCoordinate, modelInstance);
    }

    public void removeFromModelInstances(long chunkCoordinate) {
        modelInstances.remove(chunkCoordinate);
    }

    public Chunk getChunkFromCoordinate(long chunkCoordinate) {
        return loadedChunks.get(chunkCoordinate);
    }

    // Debug \\

    private void debug() { // TODO: Debug line

    }
}
