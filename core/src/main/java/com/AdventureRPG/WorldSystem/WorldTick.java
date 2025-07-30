package com.AdventureRPG.WorldSystem;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;

public class WorldTick {

    // Settings
    private final GameManager GameManager;
    private final Settings settings;
    private final WorldSystem WorldSystem;
    private final float tickTime;

    // Tick
    private float currentTime = 0f;
    private boolean Tick;

    public WorldTick(WorldSystem WorldSystem) {

        this.GameManager = WorldSystem.GameManager;
        this.settings = WorldSystem.settings;
        this.WorldSystem = GameManager.WorldSystem;
        this.tickTime = settings.WORLD_TICK;
    }

    public void Update() {

        if (currentTime >= tickTime) {
            this.Tick = true;
            currentTime = 0f;
        } else {
            this.Tick = false;
            currentTime += GameManager.DeltaTime();
        }
    }

    public boolean Tick() {
        return Tick;
    }
}
