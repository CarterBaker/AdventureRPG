package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.badlogic.gdx.math.Vector3;

public class ChunkSystem {

    // Chunk System
    private final WorldSystem WorldSystem;
    private final Settings settings;
    private final Loader Loader;

    // Rendered Chunks
    private final int range;
    private final int height;
    private final int size;
    private final Vector3Int[][][] chunks;

    // Variables
    private Vector3Int currentChunk;

    public ChunkSystem(WorldSystem WorldSystem) {

        // Chunk System
        this.WorldSystem = WorldSystem;
        this.settings = WorldSystem.GameManager.settings;
        this.Loader = new Loader(WorldSystem);

        // Rendered Chunks
        this.range = settings.MAX_RENDER_DISTANCE;
        this.height = settings.MAX_RENDER_HEIGHT;
        this.size = settings.CHUNK_SIZE;
        this.chunks = new Vector3Int[range][height][range];
    };

    public void Update() {
        Loader.Update();
    }

    public void LoadChunks(Vector3Int currentChunk) {

        this.currentChunk = currentChunk;

        for (int x = 0; x <= range - 1; x++) {
            for (int y = 0; y <= height - 1; y++) {
                for (int z = 0; z <= range - 1; z++) {
                    int ax = (x - (range / 2)) * size;
                    int ay = (y - (height / 2)) * size;
                    int az = (z - (range / 2)) * size;

                    int bx = currentChunk.x + ax;
                    int by = currentChunk.y + ay;
                    int bz = currentChunk.z + az;

                    Vector3 cPosition = new Vector3(bx, by, bz);
                    chunks[x][y][z] = WorldSystem.WrapChunksAroundWorld(cPosition);

                    System.out.println("Added Chunk: " + chunks[x][y][z].toString());
                }
            }
        }

        Loader.LoadChunks(chunks);
    }

    public void UpdateChunks(Vector3Int newChunk) {
        Vector3Int chunkShift = new Vector3Int(
                newChunk.x - currentChunk.x,
                newChunk.y - currentChunk.y,
                newChunk.z - currentChunk.z);
        Loader.UpdateChunks(chunkShift);
    }

}
