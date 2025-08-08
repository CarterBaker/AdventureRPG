package com.AdventureRPG.SaveSystem;

import java.io.File;
import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;

// TODO: Eventually the whole save system will need a refactor
public class SaveSystem {

    // Game
    public final Settings settings;
    public final GameManager gameManager;
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

    public void Awake() {

    }

    public void Start() {

    }

    public void Update() {

    }

    public void Render() {

    }

    // Save System \\

    private boolean HasNewestSave() {
        // TODO: This needs to be configured once we have our save system in place
        return false;
    }

    // User Data \\

    public void LoadUserData() {
        if (HasNewestSave())
            userData.LoadUserData(path);
    }

    public void LoadUserData(File Save) {
        userData.LoadUserData(Save);
    }

    // Chunk Data \\

    public void LoadChunkData() {
        if (HasNewestSave())
            chunkData.LoadChunkData(path);
    }

    public void LoadChunkData(File Save) {
        chunkData.LoadChunkData(Save);
    }

}
