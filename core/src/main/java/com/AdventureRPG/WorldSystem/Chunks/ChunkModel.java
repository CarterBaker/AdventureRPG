package com.AdventureRPG.WorldSystem.Chunks;

import com.badlogic.gdx.utils.IntArray;

public class ChunkModel {

    // Game Manager
    private final Chunk chunk;

    // Data
    private IntArray[] quads;

    public ChunkModel(Chunk chunk) {

        // Game Manager
        this.chunk = chunk;

        // Data
        quads = new IntArray[chunk.settings.WORLD_HEIGHT];
    }

    public void build(int subChunkIndex, IntArray quads) {
        this.quads[subChunkIndex] = quads;
    }
}
