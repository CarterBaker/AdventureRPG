package com.AdventureRPG.lightingsystem;

import com.AdventureRPG.core.engine.ManagerPackage;

public class LightingManager extends ManagerPackage {

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