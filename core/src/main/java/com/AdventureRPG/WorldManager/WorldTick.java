package com.AdventureRPG.WorldManager;

import com.AdventureRPG.Core.Bootstrap.EngineSetting;
import com.AdventureRPG.Core.Bootstrap.SystemFrame;
import com.badlogic.gdx.Gdx;

public class WorldTick extends SystemFrame {

    // Settings
    private float WORLD_TICK;

    // Tick
    private float currentTime;
    private boolean tick;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.WORLD_TICK = EngineSetting.WORLD_TICK;

        // Tick
        this.currentTime = 0f;
        this.tick = true;
    }

    @Override
    protected void update() {

        tickCOunter();
    }

    // World Tick \\

    private void tickCOunter() {

        if (currentTime >= WORLD_TICK) {
            this.tick = true;
            currentTime = 0f;
        }

        else {
            this.tick = false;
            currentTime += Gdx.graphics.getDeltaTime();
        }
    }

    // Accessible \\

    public boolean tick() {
        return tick;
    }
}
