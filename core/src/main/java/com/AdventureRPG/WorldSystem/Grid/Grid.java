package com.AdventureRPG.WorldSystem.Grid;

import com.badlogic.gdx.math.Vector3;

import com.AdventureRPG.Util.*;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.Chunks.ChunkSystem;

public class Grid {

    private Vector3 position = new Vector3();
    private Vector3Int currentChunk = new Vector3Int();

    private final WorldSystem WorldSystem;
    private final ChunkSystem ChunkSystem;

    public Grid(WorldSystem WorldSystem) {
        this.WorldSystem = WorldSystem;
        this.ChunkSystem = WorldSystem.ChunkSystem;

        ReloadChunks(); //This needs to be run once on start
    }

    public void SetPosition(Vector3 input) {
        position = WorldSystem.WrapAroundChunk(input);
        Vector3Int calculatedChunk = WorldSystem.WrapChunksAroundWorld(input);

        if (calculatedChunk == currentChunk)
            return;

        currentChunk = calculatedChunk;
        ReloadChunks();
    }

    private void ReloadChunks() {
        ChunkSystem.ReloadChunks(currentChunk);
    }

}
