package com.AdventureRPG.WorldSystem;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;

public class WorldTick {

    // Game Manager
    private final GameManager gameManager;
    private final Settings settings;
    private final float tickTime;

    // Tick
    private float currentTime = 0f;
    private boolean Tick;

    // Base \\

    public WorldTick(WorldSystem worldSystem) {

        // Game Manager
        this.gameManager = worldSystem.gameManager;
        this.settings = worldSystem.settings;

        // Tick
        this.tickTime = settings.WORLD_TICK;
    }

    public void Update() {

        if (currentTime >= tickTime) {
            this.Tick = true;
            currentTime = 0f;
        } else {
            this.Tick = false;
            currentTime += gameManager.DeltaTime();
        }
    }

    // Tick \\

    public boolean Tick() {
        return Tick;
    }
}
