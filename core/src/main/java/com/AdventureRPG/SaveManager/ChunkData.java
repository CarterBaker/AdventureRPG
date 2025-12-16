package com.AdventureRPG.savemanager;

import java.io.File;

import com.AdventureRPG.WorldPipeline.chunks.Chunk;
import com.AdventureRPG.core.kernel.SystemFrame;

public class ChunkData extends SystemFrame {

    // Settings
    private File path;

    // Base \\

    @Override
    protected void init() {

        // Settings
        this.path = gameEngine.path;
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
