package com.AdventureRPG;

import com.AdventureRPG.SaveSystem.SaveSystem;

public class GameDispose {

    // Main
    public GameManager GameManager;

    // Systems
    public SaveSystem SaveSystem;

    public GameDispose(GameManager GameManager) {

        // Main
        this.GameManager = GameManager;

        // Systems
        this.SaveSystem = GameManager.SaveSystem;
    }

    public void cleanup() {
        SaveSystem.dispose();
    }
}