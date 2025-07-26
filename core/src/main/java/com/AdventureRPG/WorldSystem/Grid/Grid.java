package com.AdventureRPG.WorldSystem.Grid;

import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.Chunks.ChunkSystem;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.badlogic.gdx.math.Vector3;

public class Grid {

    private Vector3 position = new Vector3();
    private Vector3Int currentChunk = new Vector3Int();

    private final WorldSystem WorldSystem;
    private final ChunkSystem ChunkSystem;

    public Grid(WorldSystem WorldSystem) {
        this.WorldSystem = WorldSystem;
        this.ChunkSystem = WorldSystem.ChunkSystem;

        // Init
        LoadChunks(); // This needs to be run once on start
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

    private void LoadChunks() {
        ChunkSystem.LoadChunks(currentChunk);
    }

    private void UpdateChunks() {
        ChunkSystem.UpdateChunks(currentChunk);
    }

}
