package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.GameManager;
import com.AdventureRPG.SaveSystem.ChunkData;
import com.AdventureRPG.WorldSystem.WorldSystem;

public class ChunkSystem {

    // Game
    public final GameManager GameManager;

    // Save System
    private final ChunkData ChunkData;

    // World System
    public final WorldSystem WorldSystem;

    public ChunkSystem(WorldSystem WorldSystem) {

        // Game
        this.GameManager = WorldSystem.GameManager;

        // Save System
        this.ChunkData = GameManager.SaveSystem.ChunkData;

        // World
        this.WorldSystem = WorldSystem;
    }

    public void ReloadChunks(Vector3Int currentChunk) {
        
    }
}
