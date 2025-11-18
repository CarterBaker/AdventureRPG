package com.AdventureRPG.SaveManager;

import java.io.File;

import com.AdventureRPG.Core.Bootstrap.ManagerFrame;

// TODO: Eventually the whole save system will need a refactor
public class SaveManager extends ManagerFrame {

    // Settings
    public File path;

    // Save System
    public UserData userData;
    public ChunkData chunkData;

    // Base \\

    @Override
    protected void create() {

        // Save System
        this.userData = (UserData) register(new UserData());
        this.chunkData = (ChunkData) register(new ChunkData());
    }

    @Override
    protected void init() {

        // Settings
        this.path = gameEngine.path;
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
