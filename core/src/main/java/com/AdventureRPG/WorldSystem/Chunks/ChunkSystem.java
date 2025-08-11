package com.AdventureRPG.WorldSystem.Chunks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.AdventureRPG.SaveSystem.ChunkData;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Direction;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.WorldTick;
import com.AdventureRPG.WorldSystem.Blocks.BlockData;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

public class ChunkSystem {

    // Debug
    private final boolean debug = false; // TODO: Remove debug line

    // Chunk System
    private final WorldSystem worldSystem;
    private final ChunkData chunkData;
    private final Settings settings;
    private final WorldTick worldTick;

    // Data
    private final int MAX_CHUNK_LOADS_PER_FRAME;
    private final int MAX_CHUNK_LOADS_PER_TICK;

    // Chunk System
    private int loadedChunksThisFrame;
    private int loadedChunksThisTick;

    private final Map<Vector3Int, Chunk> loadedChunks;
    private final Map<Chunk, Set<NeighborChunks>> neighborMap;
    private final Map<Chunk, ModelInstance> chunkModels;

    // Batch
    private final int indexPerBatch = 32;
    private final State[] stateCycle;
    private int cycleIndex;

    // Queue
    private final Map<Vector3Int, Chunk> moveQueue;
    private final Map<Vector3Int, Chunk> unloadQueue;
    private final Map<Vector3Int, Vector3Int> loadQueue;

    // Settings
    private int range;
    private int height;
    private final int size;

    // Rendered Chunks
    private final Vector3Int currentChunkCoordinate;

    // Temp
    private int total;
    private final ArrayList<Vector3Int> gridCoordinates;
    private final ArrayList<Vector3Int> chunkCoordinates;
    private final Map<Vector3Int, Vector3Int> chunkToGridMap;
    private final Vector3Int wrappedValue;
    private final HashSet<Vector3Int> loadedChunkCoordinates;
    private final Vector3Int lookupKey;
    private final Vector3 offset;

    // Base \\

    public ChunkSystem(WorldSystem WorldSystem) {

        // Chunk System
        this.worldSystem = WorldSystem;
        this.chunkData = WorldSystem.saveSystem.chunkData;
        this.settings = WorldSystem.settings;
        this.worldTick = WorldSystem.worldTick;

        // Data
        this.MAX_CHUNK_LOADS_PER_FRAME = settings.MAX_CHUNK_LOADS_PER_FRAME;
        this.MAX_CHUNK_LOADS_PER_TICK = settings.MAX_CHUNK_LOADS_PER_TICK;

        // Chunk System
        this.loadedChunks = new HashMap<>();
        this.neighborMap = new HashMap<>();
        this.chunkModels = new HashMap<>();

        // Batch
        this.stateCycle = new State[] { State.Unloading, State.Loading };

        // Queue
        this.moveQueue = new HashMap<>();
        this.unloadQueue = new HashMap<>();
        this.loadQueue = new HashMap<>();

        // Settings
        this.range = settings.MAX_RENDER_DISTANCE;
        this.height = settings.MAX_RENDER_HEIGHT;
        this.size = settings.CHUNK_SIZE;

        // Rendered Chunks
        this.currentChunkCoordinate = new Vector3Int(-1, -1, -1); // Initialize ChunkSystem with an invalid chunk

        // Temp
        this.total = range * range * height;
        gridCoordinates = new ArrayList<>(total);
        chunkCoordinates = new ArrayList<>(total);
        this.chunkToGridMap = new HashMap<>();

        for (int i = 0; i < total; i++)
            chunkCoordinates.add(new Vector3Int());

        this.wrappedValue = new Vector3Int();

        this.loadedChunkCoordinates = new HashSet<>();
        this.lookupKey = new Vector3Int();
        this.offset = new Vector3();
    }

    public void Awake() {
        RebuildGrid();
    }

    public void Start() {

    }

    public void Update() {
        UpdateQueue();
    }

    public void Render(ModelBatch modelBatch) {
        RenderChunks(modelBatch);
    }

    // Initialize \\

    public void RebuildGrid() {

        // Reset render distances
        this.range = settings.MAX_RENDER_DISTANCE;
        this.height = settings.MAX_RENDER_HEIGHT;

        this.total = range * height * range;

        // Clear the existing lists just in case
        PrepareNewQueue();
        gridCoordinates.clear();

        // Rebuild the gridVectors in chunk space
        for (int x = -range / 2; x < range / 2; x++)
            for (int y = -height / 2; y < height / 2; y++)
                for (int z = -range / 2; z < range / 2; z++)
                    gridCoordinates.add(new Vector3Int(x, y, z));

        // Reload the world
        LoadChunks(currentChunkCoordinate);
    }

    // Update \\

    private void UpdateQueue() {

        // Reset every frame
        loadedChunksThisFrame = 0;

        // Reset every tick
        if (worldTick.Tick())
            loadedChunksThisTick = 0;

        // If there is no queue we don't need to do anything
        if (!HasQueue())
            return;

        // Alternating queue update
        for (int i = 0; i < stateCycle.length; i++) {

            if (stateCycle[cycleIndex].process(this))
                return;

            cycleIndex = (cycleIndex + 1) % stateCycle.length;
        }

        if (debug) // TODO: Remove debug line
            Debug();
    }

    // Render \\

    private void RenderChunks(ModelBatch modelBatch) {

        // Smooth float pos inside the current chunk
        Vector3 playerPos = worldSystem.Position();

        // Calculated current chunk coordinate in block space
        int baseX = currentChunkCoordinate.x * size;
        int baseY = currentChunkCoordinate.y * size;
        int baseZ = currentChunkCoordinate.z * size;

        // Calculated offset for each chunk for smooth movement
        float worldOffsetX = baseX + playerPos.x;
        float worldOffsetY = baseY + playerPos.y;
        float worldOffsetZ = baseZ + playerPos.z;

        // For each chunk with an active model calculate the render position
        for (Map.Entry<Chunk, ModelInstance> entry : chunkModels.entrySet()) {

            Chunk chunk = entry.getKey();
            ModelInstance model = entry.getValue();

            offset.x = (chunk.position.x * size) - worldOffsetX;
            offset.y = (chunk.position.y * size) - worldOffsetY;
            offset.z = (chunk.position.z * size) - worldOffsetZ;

            // TODO: This will be needed later but for now debugging is easier without
            // worldSystem.WrapAroundGrid(offset);

            model.transform.setToTranslation(offset);
            modelBatch.render(model);

            // TODO: Eventually when I add lighting I need to pass the environment
            // modelBatch.render(model, WorldSystem.GameManager.environment);
        }
    }

    // Main \\

    public void LoadChunks(Vector3Int chunkCoordinate) {

        // Triple check in the back end it is safe to trigger a load
        if (this.currentChunkCoordinate.equals(chunkCoordinate))
            return;

        // Set the current chunk first
        currentChunkCoordinate.set(chunkCoordinate);

        // Prepare new load queue
        PrepareNewQueue();

        // Determine which chunks need to be loaded where
        RebuildChunksAroundActiveChunk();

        // Aseemble all queues to handle chunk loading
        BuildQueue();
    }

    private void RebuildChunksAroundActiveChunk() {

        for (int i = 0; i < total; i++) {

            // We only need to read the key never set
            Vector3Int key = gridCoordinates.get(i);

            // Calculate the chunk to load using the key and current chunk
            int x = currentChunkCoordinate.x + key.x;
            int y = currentChunkCoordinate.y + key.y;
            int z = currentChunkCoordinate.z + key.z;

            wrappedValue.set(x, y, z);
            worldSystem.WrapAroundWorld(wrappedValue);

            // Set the value to the correct chunks coordinate
            chunkCoordinates.get(i).set(wrappedValue);

            // Assemble the map with grid coordinates and chunk coordinates
            chunkToGridMap.put(chunkCoordinates.get(i), gridCoordinates.get(i));
        }
    }

    private void BuildQueue() {

        for (Map.Entry<Vector3Int, Chunk> entry : loadedChunks.entrySet()) {

            Vector3Int gridCoordinate = entry.getKey();
            Chunk loadedChunk = entry.getValue();

            Vector3Int loadedChunkCoordinate = loadedChunk.coordinate;
            Vector3Int newGridCoordinate = chunkToGridMap.get(loadedChunkCoordinate);

            if (newGridCoordinate != null) {

                loadedChunkCoordinates.add(loadedChunkCoordinate);

                if (!newGridCoordinate.equals(loadedChunk.position))
                    moveQueue.put(newGridCoordinate, loadedChunk);
            }

            else
                unloadQueue.put(gridCoordinate, loadedChunk);
        }

        for (Map.Entry<Vector3Int, Vector3Int> entry : chunkToGridMap.entrySet()) {

            Vector3Int chunkCoord = entry.getKey();
            Vector3Int gridCoord = entry.getValue();

            if (!loadedChunkCoordinates.contains(chunkCoord))
                loadQueue.put(gridCoord, chunkCoord);
        }

        System.out.println("Current Chunk: " + currentChunkCoordinate.toString()
                + "Move Queue: " + moveQueue.size() // TODO: Remove debug line
                + ", Unload Queue: " + unloadQueue.size()
                + ", Load Queue: " + loadQueue.size());

        MoveActiveChunks();
    }

    private void PrepareNewQueue() {

        cycleIndex = 0;

        moveQueue.clear();
        unloadQueue.clear();
        loadQueue.clear();

        loadedChunkCoordinates.clear();
        chunkToGridMap.clear();
    }

    // Move \\

    private void MoveActiveChunks() {

        Iterator<Map.Entry<Vector3Int, Chunk>> iterator = moveQueue.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<Vector3Int, Chunk> entry = iterator.next();
            Vector3Int gridCoordinate = entry.getKey();
            Chunk loadedChunk = entry.getValue();

            loadedChunk.MoveTo(gridCoordinate);
            loadedChunks.put(gridCoordinate, loadedChunk);

            iterator.remove();
        }

    }

    // Unload \\

    private boolean UpdateUnloading() {

        Iterator<Map.Entry<Vector3Int, Chunk>> iterator = unloadQueue.entrySet().iterator();
        int index = 0;

        while (iterator.hasNext() && index < indexPerBatch &&
                loadedChunksThisFrame < MAX_CHUNK_LOADS_PER_FRAME &&
                loadedChunksThisTick < MAX_CHUNK_LOADS_PER_TICK) {

            Map.Entry<Vector3Int, Chunk> entry = iterator.next();
            Vector3Int gridCoordinate = entry.getKey();
            Chunk loadedChunk = entry.getValue();

            RemoveFromNeighborMap(loadedChunk);

            ModelInstance mesh = chunkModels.remove(loadedChunk);

            if (mesh != null)
                mesh.model.dispose();

            loadedChunks.remove(gridCoordinate);

            iterator.remove();
            IncreaseQueueCount();
            index++;
        }

        return loadedChunksThisFrame >= MAX_CHUNK_LOADS_PER_FRAME ||
                loadedChunksThisTick >= MAX_CHUNK_LOADS_PER_TICK;
    }

    // Load \\

    private boolean UpdateLoading() {

        Iterator<Map.Entry<Vector3Int, Vector3Int>> iterator = loadQueue.entrySet().iterator();
        int index = 0;

        while (iterator.hasNext() && index < indexPerBatch &&
                loadedChunksThisFrame < MAX_CHUNK_LOADS_PER_FRAME &&
                loadedChunksThisTick < MAX_CHUNK_LOADS_PER_TICK) {

            Map.Entry<Vector3Int, Vector3Int> entry = iterator.next();
            Vector3Int gridCoordinate = entry.getKey();
            Vector3Int chunkCoordinate = entry.getValue();

            Chunk chunk = chunkData.ReadChunk(chunkCoordinate);

            if (chunk == null)
                chunk = worldSystem.worldGenerator.GenerateChunk(chunkCoordinate, gridCoordinate);
            else // The world generator handles it's own positioning logic
                chunk.MoveTo(gridCoordinate);

            loadedChunks.put(gridCoordinate, chunk);

            GetNearbyChunks(chunk);

            iterator.remove();
            IncreaseQueueCount();
            index++;
        }

        return loadedChunksThisFrame >= MAX_CHUNK_LOADS_PER_FRAME ||
                loadedChunksThisTick >= MAX_CHUNK_LOADS_PER_TICK;
    }

    // Build \\

    private void GetNearbyChunks(Chunk chunk) {

        AssessNeighbors(chunk);

        for (Chunk nearbyChunk : chunk.getNeighbors().chunks())
            AssessNeighbors(nearbyChunk);
    }

    private void AssessNeighbors(Chunk chunk) {

        if (chunk.getNeighbors().isValid()) {

            if (chunk.getMesh() == null)
                TryBuild(chunk);

            return;
        }

        Vector3Int chunkPosition = chunk.position;
        NeighborChunks neighborChunks = chunk.getNeighbors();

        SetNearbyChunks(chunkPosition, neighborChunks);

        TryBuild(chunk);
    }

    private void SetNearbyChunks(Vector3Int chunkPosition, NeighborChunks neighborChunks) {

        for (Direction dir : Direction.values()) {

            int offsetX = chunkPosition.x + dir.x * size;
            int offsetY = chunkPosition.y + dir.y * size;
            int offsetZ = chunkPosition.z + dir.z * size;

            lookupKey.set(offsetX, offsetY, offsetZ);
            Chunk neighbor = loadedChunks.get(lookupKey);

            if (neighbor != null) {
                neighborChunks.set(dir, neighbor);
                PutToNeightborMap(neighbor, neighborChunks);
            }
        }
    }

    private void PutToNeightborMap(Chunk chunk, NeighborChunks neighborChunks) {

        if (chunk == null || neighborChunks == null)
            return;

        neighborMap.computeIfAbsent(chunk, k -> new HashSet<>()).add(neighborChunks);
    }

    private void RemoveFromNeighborMap(Chunk chunk) {

        if (chunk == null)
            return;

        Set<NeighborChunks> neighbors = neighborMap.get(chunk);

        if (neighbors == null)
            return;

        for (NeighborChunks neighbor : neighbors)
            if (neighbor != null)
                neighbor.remove(chunk);

        neighborMap.remove(chunk);
    }

    private void TryBuild(Chunk chunk) {

        boolean successfullBuild = chunk.TryBuild();

        if (!successfullBuild)
            return;

        ModelInstance mesh = chunk.getMesh();

        if (mesh != null)
            chunkModels.put(chunk, mesh);
    }

    // Utility \\

    public boolean HasQueue() {
        return (unloadQueue.size() > 0 ||
                loadQueue.size() > 0);
    }

    public int QueueSize() {
        return unloadQueue.size() + loadQueue.size();
    }

    private void IncreaseQueueCount() {

        loadedChunksThisFrame += 1;
        loadedChunksThisTick += 1;
    }

    enum State {
        Unloading {
            boolean process(ChunkSystem system) {
                return system.UpdateUnloading();
            }
        },
        Loading {
            boolean process(ChunkSystem system) {
                return system.UpdateLoading();
            }
        };

        abstract boolean process(ChunkSystem system);
    }

    // Debug \\

    private void Debug() { // TODO: Remove debug line

        Vector3 currentPosition = worldSystem.Position();

        int blockPositionX = (int) Math.floor(currentPosition.x);
        int blockPositionY = (int) Math.floor(currentPosition.y);
        int blockPositionZ = (int) Math.floor(currentPosition.z);

        Chunk chunk = loadedChunks.get(currentChunkCoordinate.multiply(size));

        if (chunk == null)
            return;

        BlockData data = chunk.getBlockData(blockPositionX, blockPositionY, blockPositionZ);

        if (data == null)
            return;

        System.out.print("\rChunk Coordinate: " + currentChunkCoordinate.toString() +
                ", Block Coordinate: " + new Vector3Int(blockPositionX, blockPositionY, blockPositionZ).toString() +
                ", Biome: " + worldSystem.biomeSystem.GetBiomeByID(data.biomeID).name +
                ", Block: " + worldSystem.GetBlockByID(data.blockID).name);
        System.out.flush();
    }
}
