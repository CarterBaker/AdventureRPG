package com.AdventureRPG.SaveSystem;

import java.io.File;
import java.io.IOException;

import com.AdventureRPG.WorldSystem.Chunks.Chunk;

public class ChunkData {

    // Save System
    public final SaveSystem SaveSystem;
    public final File path;

    // ChunkData
    private File ChunkDataFile;
    private boolean usingTempData;

    public ChunkData(SaveSystem SaveSystem) {

        // Save System
        this.SaveSystem = SaveSystem;
        this.path = SaveSystem.path;
    }

    public void LoadChunkData(File Save) {
        // Not implemented yet
    }

    public void CreateTemporaryChunkData() {
        ChunkDataFile = new File(path, "ChunkData.db");
        usingTempData = true;

        try {
            if (ChunkDataFile.createNewFile()) {
                System.out.println("Temporary chunk file created: " + ChunkDataFile.getAbsolutePath());
            } else {
                System.out.println("Temporary chunk file already exists: " + ChunkDataFile.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Temporary chunk file created: " + ChunkDataFile.getAbsolutePath());
    }

    public void WriteChunk(Chunk chunk) {

    }

    public Chunk ReadChunk(int x, int y, int z) {
        return null;
    }

    public void dispose() {
        if (usingTempData)
            DeleteTempFile();
    }

    private void DeleteTempFile() {
        ChunkDataFile.delete();
        usingTempData = false;

        System.out.println("Temporary chunk file deleted");
    }
}
