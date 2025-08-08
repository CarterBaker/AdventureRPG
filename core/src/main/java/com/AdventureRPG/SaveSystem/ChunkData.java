package com.AdventureRPG.SaveSystem;

import java.io.File;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;

public class ChunkData {

    // Save System
    private final Settings settings;
    private final File path;

    // Base \\

    public ChunkData(SaveSystem saveSystem) {

        // Save System
        this.settings = saveSystem.settings;
        this.path = saveSystem.path;
    }

    // Save System \\

    // TODO: The whole saving and loading of worlds needs to be well thought out
    public void LoadChunkData(File Save) {

    }

    public void WriteChunk(Chunk chunk) {

    }

    public Chunk ReadChunk(Vector3Int position) {
        return null;
    }

}
