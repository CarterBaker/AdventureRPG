package com.AdventureRPG.SaveManager;

import java.io.File;

import com.AdventureRPG.Core.SystemFrame;
import com.AdventureRPG.WorldManager.Chunks.Chunk;

public class ChunkData extends SystemFrame {

    // Settings
    private File path;

    // Base \\

    @Override
    public void init() {

        // Settings
        this.path = rootManager.path;
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
