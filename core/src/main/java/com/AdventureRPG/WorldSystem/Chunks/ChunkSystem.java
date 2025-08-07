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
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

public class ChunkSystem {

    // Chunk System
    private final WorldSystem WorldSystem;
    private final ChunkData ChunkData;
    private final Settings settings;
    private final WorldTick WorldTick;

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
    private final int range;
    private final int height;
    private final int size;

    // Rendered Chunks
    private final Vector3Int currentChunkCoordinate;

    // Temp
    private final ArrayList<Vector3Int> gridCoordinates;
    private final ArrayList<Vector3Int> chunkCoordinates;
    private final Map<Vector3Int, Vector3Int> chunkToGridMap;
    private final Vector3 wrappedValue;
    private final HashSet<Vector3Int> loadedChunkCoordinates;
    private final Vector3Int lookupKey;

    // Base \\

    public ChunkSystem(WorldSystem WorldSystem) {

        // Chunk System
        this.WorldSystem = WorldSystem;
        this.ChunkData = WorldSystem.SaveSystem.ChunkData;
        this.settings = WorldSystem.settings;
        this.WorldTick = WorldSystem.WorldTick;

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
        int total = range * range * height;

        gridCoordinates = new ArrayList<>(total);
        chunkCoordinates = new ArrayList<>(total);
        this.chunkToGridMap = new HashMap<>();

        for (int i = 0; i < total; i++) {
            gridCoordinates.add(new Vector3Int());
            chunkCoordinates.add(new Vector3Int());
        }

        this.wrappedValue = new Vector3();

        this.loadedChunkCoordinates = new HashSet<>();
        this.lookupKey = new Vector3Int();
    }

    public void Update() {

        loadedChunksThisFrame = 0;

        if (WorldTick.Tick())
            loadedChunksThisTick = 0;

        if (!HasQueue())
            return;

        for (int i = 0; i < stateCycle.length; i++) {

            if (stateCycle[cycleIndex].process(this))
                return;

            cycleIndex = (cycleIndex + 1) % stateCycle.length;
        }
    }

    public void Render(ModelBatch modelBatch) {

        Vector3 worldPosition = WorldSystem.position();

        for (Map.Entry<Chunk, ModelInstance> entry : chunkModels.entrySet()) {

            Chunk chunk = entry.getKey();
            ModelInstance model = entry.getValue();

            Vector3Int chunkPosition = chunk.position;

            // Calculate world-relative position
            float offsetX = chunkPosition.x - worldPosition.x;
            float offsetY = chunkPosition.y - worldPosition.y;
            float offsetZ = chunkPosition.z - worldPosition.z;

            // Apply the translation to the chunk's mesh
            model.transform.setToTranslation(offsetX, offsetY, offsetZ);

            // Render the mesh
            modelBatch.render(model);

            // TODO: Eventually when I add lighting I need to pass the environment
            // modelBatch.render(model, WorldSystem.GameManager.environment);
        }
    }

    // Main \\

    public void LoadChunks(Vector3Int chunkCoordinate) {

        this.currentChunkCoordinate.set(chunkCoordinate);

        RebuildChunksAroundActiveChunk();

        BuildQueue();
    }

    private void RebuildChunksAroundActiveChunk() {

        int index = 0;

        for (int x = -range / 2; x < range / 2; x++) {

            for (int y = -height / 2; y < height / 2; y++) {

                for (int z = -range / 2; z < range / 2; z++) {

                    int ax = x * size;
                    int ay = y * size;
                    int az = z * size;

                    Vector3Int key = gridCoordinates.get(index);
                    key.set(ax, ay, az);

                    int bx = currentChunkCoordinate.x + x * size;
                    int by = currentChunkCoordinate.y + y * size;
                    int bz = currentChunkCoordinate.z + z * size;

                    wrappedValue.set(bx, by, bz);
                    Vector3Int wrappedChunkCoordinate = WorldSystem.WrapChunksAroundWorld(wrappedValue);

                    Vector3Int value = chunkCoordinates.get(index);
                    value.set(wrappedChunkCoordinate);

                    index++;
                }
            }
        }
    }

    public void BuildQueue() {

        // Prepare new load queue
        moveQueue.clear();
        unloadQueue.clear();
        loadQueue.clear();
        cycleIndex = 0;

        loadedChunkCoordinates.clear();
        chunkToGridMap.clear();

        for (int i = 0; i < chunkCoordinates.size(); i++)
            chunkToGridMap.put(chunkCoordinates.get(i), gridCoordinates.get(i));

        for (Map.Entry<Vector3Int, Chunk> entry : loadedChunks.entrySet()) {

            Vector3Int gridCoordinate = entry.getKey();
            Chunk loadedChunk = entry.getValue();
            Vector3Int chunkCoordinate = loadedChunk.coordinate;
            Vector3Int newGridCoord = chunkToGridMap.get(chunkCoordinate);

            // Move Queue
            if (newGridCoord != null) {

                loadedChunkCoordinates.add(chunkCoordinate);

                if (!newGridCoord.equals(loadedChunk.position))
                    moveQueue.put(newGridCoord, loadedChunk);
            }

            // Unload Queue
            else
                unloadQueue.put(gridCoordinate, loadedChunk);
        }

        // Load Queue
        for (Map.Entry<Vector3Int, Vector3Int> entry : chunkToGridMap.entrySet()) {

            Vector3Int chunkCoord = entry.getKey();
            Vector3Int gridCoord = entry.getValue();

            if (!loadedChunkCoordinates.contains(chunkCoord))
                loadQueue.put(gridCoord, chunkCoord);
        }

        MoveActiveChunks();
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

            RemoveFromNeightborMap(loadedChunk);

            ModelInstance mesh = chunkModels.remove(loadedChunk);
            if (mesh != null) {
                mesh.model.dispose();
            }

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

            Chunk chunk = ChunkData.ReadChunk(chunkCoordinate);

            if (chunk == null)
                chunk = WorldSystem.WorldGenerator.GenerateChunk(chunkCoordinate, gridCoordinate);
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

    private void RemoveFromNeightborMap(Chunk chunk) {

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
}
