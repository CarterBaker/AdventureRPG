package com.AdventureRPG.WorldSystem.Chunks;

import java.util.ArrayList;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Vector3;

public class ChunkSystem {

    // Chunk System
    public final Settings settings;
    private final WorldSystem WorldSystem;
    private final Loader Loader;

    // Settings
    private final int range;
    private final int height;
    private final int size;

    // Rendered Chunks
    private Vector3Int chunk;

    // Temp values for fast iteration
    ArrayList<Vector3Int> gridCoordinates;
    ArrayList<Vector3Int> chunkCoordinates;
    private Vector3 wrappedValue;

    public ChunkSystem(WorldSystem WorldSystem) {

        // Chunk System
        this.settings = WorldSystem.settings;
        this.WorldSystem = WorldSystem;
        this.Loader = new Loader(WorldSystem);

        // Settings
        this.range = settings.MAX_RENDER_DISTANCE;
        this.height = settings.MAX_RENDER_HEIGHT;
        this.size = settings.CHUNK_SIZE;

        // Rendered Chunks
        this.chunk = new Vector3Int();

        // Temp
        int total = range * range * height;

        gridCoordinates = new ArrayList<>(total);
        chunkCoordinates = new ArrayList<>(total);

        for (int i = 0; i < total; i++) {
            gridCoordinates.add(new Vector3Int());
            chunkCoordinates.add(new Vector3Int());
        }

        this.wrappedValue = new Vector3();
    }

    public void Update() {
        Loader.Update();
    }

    public void Render(ModelBatch modelBatch) {
        Loader.Render(modelBatch);
    }

    public Vector3Int Center() {
        return chunk;
    }

    public void LoadChunks(Vector3Int chunk) {

        if (this.chunk == chunk)
            return;

        this.chunk = chunk;

        RebuildChunksAroundChunk();

        Loader.BuildQueue(gridCoordinates, chunkCoordinates);
    }

    private void RebuildChunksAroundChunk() {

        int index = 0;

        for (int x = -range / 2; x < range / 2; x++) {
            for (int y = -height / 2; y < height / 2; y++) {
                for (int z = -range / 2; z < range / 2; z++) {

                    Vector3Int key = gridCoordinates.get(index);
                    key.set(x, y, z);

                    int ax = chunk.x + x * size;
                    int ay = chunk.y + y * size;
                    int az = chunk.z + z * size;

                    wrappedValue.set(ax, ay, az);
                    Vector3Int wrapped = WorldSystem.WrapChunksAroundWorld(wrappedValue);

                    Vector3Int value = chunkCoordinates.get(index);
                    value.set(wrapped.x, wrapped.y, wrapped.z);

                    index++;
                }
            }
        }
    }

    // Accessible

    public boolean HasQueue() {
        return Loader.HasQueue();
    }

    public int QueueSize() {
        return Loader.QueueSize();
    }

    public NeighborChunks GetNearbyChunks(Vector3Int coordinnate) {
        return Loader.GetNearbyChunks(coordinnate);
    }

    public NeighborChunks SetNearbyChunks(Vector3Int coordinate, NeighborChunks neighborChunks) {
        return Loader.SetNearbyChunks(coordinate, neighborChunks);
    }
}
