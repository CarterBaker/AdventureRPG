package com.AdventureRPG.SaveSystem;

import java.io.File;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;

// TODO: Eventually the whole save system will need a refactor
public class SaveSystem {

    // Game Manager
    public final Settings settings;
    public final GameManager gameManager;

    // Settings
    public final File path;

    // Save System
    public final UserData userData;
    public final ChunkData chunkData;

    // Base \\

    public SaveSystem(GameManager gameManager) {

        // Game
        this.settings = gameManager.settings;
        this.gameManager = gameManager;
        this.path = gameManager.path;

        // Save System
        this.userData = new UserData(this);
        this.chunkData = new ChunkData(this);
    }

    public void awake() {

    }

    public void start() {

    }

    public void update() {

    }

    public void render() {

    }

    // Save System \\

    private boolean hasNewestSave() {
        // TODO: This needs to be configured once we have our save system in place
        return false;
    }

    // User Data \\

    public void loadUserData() {
        if (hasNewestSave())
            userData.loadUserData(path);
    }

    public void loadUserData(File Save) {
        userData.loadUserData(Save);
    }

    // Chunk Data \\

    public void loadChunkData() {
        if (hasNewestSave())
            chunkData.loadChunkData(path);
    }

    public void loadChunkData(File Save) {
        chunkData.loadChunkData(Save);
    }
}
