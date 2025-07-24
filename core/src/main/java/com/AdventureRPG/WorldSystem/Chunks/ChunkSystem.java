package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.WorldSystem;

public class ChunkSystem {

    // Chunk System
    private final Loader Loader;

    public ChunkSystem(WorldSystem WorldSystem) {

        // Chunk System
        this.Loader = new Loader(WorldSystem);
    }

    public void ReloadChunks(Vector3Int currentChunk) {
        
    }

}
