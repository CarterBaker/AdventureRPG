package com.AdventureRPG.WorldSystem;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;

public class WorldTick {

    // Game Manager
    private final GameManager gameManager;
    private final Settings settings;

    // Settings
    private final float WORLD_TICK;

    // Tick
    private float currentTime = 0f;
    private boolean tick;

    // Base \\

    public WorldTick(WorldSystem worldSystem) {

        // Game Manager
        this.gameManager = worldSystem.gameManager;
        this.settings = worldSystem.settings;

        // Settings
        this.WORLD_TICK = settings.WORLD_TICK;
    }

    public void update() {

        if (currentTime >= WORLD_TICK) {
            this.tick = true;
            currentTime = 0f;
        } else {
            this.tick = false;
            currentTime += gameManager.deltaTime();
        }
    }

    // Tick \\

    public boolean tick() {
        return tick;
    }
}
