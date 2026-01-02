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
        this.sky = create(Sky.class);
        this.sun = create(Sun.class);
    }
}