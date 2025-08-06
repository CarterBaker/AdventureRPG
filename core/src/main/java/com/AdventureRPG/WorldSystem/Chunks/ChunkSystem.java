package com.AdventureRPG.WorldSystem.Chunks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
    private final List<ModelInstance> chunkModels;

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
    private final Vector3 wrappedValue;
    private final HashSet<Vector3Int> loadedChunkCoordinates;
    private final Map<Vector3Int, Vector3Int> chunkToGridMap;
    private final Vector3Int nearbyChunk;

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
        this.chunkModels = Collections.synchronizedList(new ArrayList<>());

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

        for (int i = 0; i < total; i++) {
            gridCoordinates.add(new Vector3Int());
            chunkCoordinates.add(new Vector3Int());
        }

        this.wrappedValue = new Vector3();

        this.loadedChunkCoordinates = new HashSet<>();
        this.chunkToGridMap = new HashMap<>();
        this.nearbyChunk = new Vector3Int();
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

        for (ModelInstance model : chunkModels) {

            if (model != null)
                modelBatch.render(model);
        }
    }

    // Main \\

    public void LoadChunks(Vector3Int chunkCoordinate) {

        if (this.currentChunkCoordinate.equals(chunkCoordinate))
            return;

        this.currentChunkCoordinate.set(chunkCoordinate);

        RebuildChunksAroundChunk();

        BuildQueue(gridCoordinates, chunkCoordinates);
    }

    private void RebuildChunksAroundChunk() {

        int index = 0;

        for (int x = -range / 2; x < range / 2; x++) {

            for (int y = -height / 2; y < height / 2; y++) {

                for (int z = -range / 2; z < range / 2; z++) {

                    Vector3Int key = gridCoordinates.get(index);
                    key.set(x, y, z);

                    int ax = currentChunkCoordinate.x + x * size;
                    int ay = currentChunkCoordinate.y + y * size;
                    int az = currentChunkCoordinate.z + z * size;

                    wrappedValue.set(ax, ay, az);
                    Vector3Int wrapped = WorldSystem.WrapChunksAroundWorld(wrappedValue);

                    Vector3Int value = chunkCoordinates.get(index);
                    value.set(wrapped.x, wrapped.y, wrapped.z);

                    index++;
                }
            }
        }
    }

    public void BuildQueue(ArrayList<Vector3Int> gridCoordinates, ArrayList<Vector3Int> chunkCoordinates) {

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
            ModelInstance mesh = loadedChunk.getMesh();

            RemoveFromNeightborMap(loadedChunk);

            if (mesh != null) {
                chunkModels.remove(mesh);
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
                chunk = WorldSystem.GenerateChunk(chunkCoordinate, gridCoordinate);

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

        Vector3Int coordinate = chunk.coordinate;
        NeighborChunks neighborChunks = chunk.getNeighbors();

        neighborChunks = SetNearbyChunks(coordinate, neighborChunks);

        for (Chunk nearyChunk : neighborChunks.chunks())
            AssessNeighbors(nearyChunk);
    }

    private void AssessNeighbors(Chunk chunk) {
        Vector3Int coordinate = chunk.coordinate;
        NeighborChunks neighborChunks = chunk.getNeighbors();

        neighborChunks = SetNearbyChunks(coordinate, neighborChunks);
    }

    private NeighborChunks SetNearbyChunks(Vector3Int coordinate, NeighborChunks neighborChunks) {

        for (Direction dir : Direction.values()) {

            int offsetX = coordinate.x + dir.x;
            int offsetY = coordinate.y + dir.y;
            int offsetZ = coordinate.z + dir.z;

            nearbyChunk.set(offsetX, offsetY, offsetZ);
            Chunk neighbor = loadedChunks.get(nearbyChunk);

            neighborChunks.set(dir, neighbor);

            PutToNeightborMap(neighbor, neighborChunks);
        }

        return neighborChunks;
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

        if (neighbors != null) {

            for (NeighborChunks neighbor : neighbors) {

                if (neighbor != null)
                    neighbor.remove(chunk);
            }
            neighborMap.remove(chunk);
        }
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
