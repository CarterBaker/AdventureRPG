package com.AdventureRPG.SaveSystem;

import java.io.File;

import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;

public class ChunkData {

    // Save System
    private final Settings Settings;
    private final File path;

    public ChunkData(SaveSystem SaveSystem) {

        // Save System
        this.Settings = SaveSystem.GameManager.settings;
        this.path = SaveSystem.path;
    }

    public void LoadChunkData(File Save) {

    }

    public void WriteChunk(Chunk chunk) {

    }

    public Chunk ReadChunk(Vector3Int position) {
        return null;
    }

}
