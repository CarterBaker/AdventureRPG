package com.AdventureRPG.LightingSystem;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;

public class LightingSystem {

    // Game Manager
    public final Settings settings;

    // Lighting
    public final Sky sky;
    public final Sun sun;

    // Base \\

    public LightingSystem(GameManager gameManager) {

        // Game manager
        this.settings = gameManager.settings;

        // Lighting
        this.sky = new Sky(gameManager);
        this.sun = new Sun();
    }

    public void awake() {
        sky.awake();
    }

    public void start() {
        sky.start();
    }

    public void update() {
        sky.update();
    }
}