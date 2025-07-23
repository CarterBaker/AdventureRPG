package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.WorldSystem;

public class ChunkSystem {

    // Game
    public final GameManager GameManager;

    // World System
    public final WorldSystem WorldSystem;

    public ChunkSystem(WorldSystem WorldSystem) {

        // Game
        this.GameManager = WorldSystem.GameManager;

        // World
        this.WorldSystem = WorldSystem;
    }

    public void ReloadChunks(Vector3Int currentChunk) {
        
    }
}
