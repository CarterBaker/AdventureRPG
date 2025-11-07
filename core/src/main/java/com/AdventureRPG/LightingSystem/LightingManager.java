package com.AdventureRPG.LightingSystem;

import com.AdventureRPG.Core.Root.ManagerFrame;

public class LightingManager extends ManagerFrame {

    // Lighting
    public Sky sky;
    public Sun sun;

    // Base \\

    @Override
    protected void create() {

        // Lighting
        this.sky = (Sky) register(new Sky());
        this.sun = (Sun) register(new Sun());
    }
}