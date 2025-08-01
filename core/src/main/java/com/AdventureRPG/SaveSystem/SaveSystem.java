package com.AdventureRPG.SaveSystem;

import java.io.File;

import com.AdventureRPG.GameManager;

public class SaveSystem {

    // Game
    public final GameManager GameManager;
    public final File path;

    // Save System
    public final UserData UserData;
    public final ChunkData ChunkData;

    public SaveSystem(GameManager GameManager) {

        // Game
        this.GameManager = GameManager;
        this.path = GameManager.path;

        // Save System
        this.UserData = new UserData(this);
        this.ChunkData = new ChunkData(this);
    }

    // Save System
    private boolean HasNewestSave() {
        return false;
    }

    // User Data

    public void LoadUserData() {
        if (HasNewestSave())
            UserData.LoadUserData(path);
    }

    public void LoadUserData(File Save) {
        UserData.LoadUserData(Save);
    }

    // Chunk Data

    public void LoadChunkData() {
        if (HasNewestSave())
            ChunkData.LoadChunkData(path);
    }

    public void LoadChunkData(File Save) {
        ChunkData.LoadChunkData(Save);
    }

}
