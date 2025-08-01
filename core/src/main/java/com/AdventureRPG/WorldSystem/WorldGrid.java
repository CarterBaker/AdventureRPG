package com.AdventureRPG.WorldSystem;

import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.Chunks.ChunkSystem;
import com.badlogic.gdx.math.Vector3;

public class WorldGrid {

    private Vector3 position = new Vector3();
    private Vector3Int currentChunk = new Vector3Int();

    private final WorldSystem WorldSystem;
    private final ChunkSystem ChunkSystem;

    public WorldGrid(WorldSystem WorldSystem) {
        this.WorldSystem = WorldSystem;
        this.ChunkSystem = WorldSystem.ChunkSystem;
    }

    public void SetTo(Vector3 input) {
        position = WorldSystem.WrapAroundChunk(input);
        Vector3Int calculatedChunk = WorldSystem.WrapChunksAroundWorld(input);

        if (calculatedChunk == currentChunk)
            return;

        currentChunk = calculatedChunk;
        LoadChunks();
    }

    public void MoveTo(Vector3 input) {
        position = WorldSystem.WrapAroundChunk(input);
        Vector3Int calculatedChunk = WorldSystem.WrapChunksAroundWorld(input);

        if (calculatedChunk == currentChunk)
            return;

        currentChunk = calculatedChunk;
        UpdateChunks();
    }

    public void LoadChunks() {
        ChunkSystem.LoadChunks(currentChunk);
    }

    public void UpdateChunks() {
        ChunkSystem.UpdateChunks(currentChunk);
    }

}
