package com.AdventureRPG.LightingSystem;

import com.AdventureRPG.Core.Framework.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;

public class LightingSystem extends GameManager {

    // Lighting
    public Sky sky;
    public Sun sun;

    // Base \\

    @Override
    public void init() {

        // Lighting
        this.sky = (Sky) register(new Sky());
        this.sun = (Sun) register(new Sun());
    }
}