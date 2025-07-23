package com.AdventureRPG.SaveSystem;

import java.io.File;

import com.AdventureRPG.GameManager;

public class SaveSystem {

    // Game
    public final GameManager GameManager;
    public final File path;

    // Save System
    public final ChunkData ChunkData;

    public SaveSystem(GameManager GameManager) {

        // Game
        this.GameManager = GameManager;
        this.path = GameManager.path;

        // Save System
        this.ChunkData = new ChunkData(this);
        LoadChunkData();

    }

    public void LoadChunkData(File Save) {
        ChunkData.LoadChunkData(Save);
    }

    private void LoadChunkData() {
        if (HasNewestSave())
            ChunkData.LoadChunkData(path);
        else
            ChunkData.CreateTemporaryChunkData();
    }

    private boolean HasNewestSave() {
        return false;
    }

    public void dispose() {
        ChunkData.dispose();
    }

}
