package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SaveSystem.ChunkData;
import com.AdventureRPG.SettingsSystem.Settings;

public class Loader {

    // Chunk System
    private final ChunkData ChunkData;
    public final Settings Settings;

    public Loader(GameManager GameManager) {

        // Chunk System
        this.ChunkData = GameManager.SaveSystem.ChunkData;
        this.Settings = GameManager.settings;
    }

}
