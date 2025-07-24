package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.SaveSystem.ChunkData;
import com.AdventureRPG.WorldSystem.WorldSystem;

public class Loader {

    // Chunk System
    private final ChunkData ChunkData;
    public final Settings Settings;

    public Loader(WorldSystem WorldSystem) {

        // Chunk System
        this.ChunkData = WorldSystem.GameManager.SaveSystem.ChunkData;
        this.Settings = WorldSystem.settings;
    }

}
