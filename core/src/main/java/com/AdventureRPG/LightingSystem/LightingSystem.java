package com.AdventureRPG.LightingSystem;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;

public class LightingSystem {

    // Game Manager
    public final Settings settings;

    // Lighting
    public final Sun sun;

    // Base \\

    public LightingSystem(GameManager gameManager) {

        // Game manager
        this.settings = gameManager.settings;

        // Lighting
        this.sun = new Sun();
    }
}