package com.AdventureRPG.SaveSystem;

import java.io.File;

import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;

public class ChunkData {

    // Settings
    private final Settings settings;
    private final File path;

    // Base \\

    public ChunkData(SaveSystem saveSystem) {

        // Settings
        this.settings = saveSystem.settings;
        this.path = saveSystem.path;
    }

    // Save System \\

    // TODO: The whole saving and loading of worlds needs to be well thought out
    public void loadChunkData(File save) {

    }

    public void writeChunk(Chunk chunk) {

    }

    public Chunk readChunk(long chunkCoordinate) {

        // TODO: When I add the load system make sure to return the chunk with
        // ChunkState.generated
        return null;
    }
}
