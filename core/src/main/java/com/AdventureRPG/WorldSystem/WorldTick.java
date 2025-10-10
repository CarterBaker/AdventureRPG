package com.AdventureRPG.WorldSystem;

import com.AdventureRPG.Util.GlobalConstant;
import com.badlogic.gdx.Gdx;

public class WorldTick {

    // Settings
    private final float WORLD_TICK;

    // Tick
    private float currentTime = 0f;
    private boolean tick;

    // Base \\

    public WorldTick() {

        // Settings
        this.WORLD_TICK = GlobalConstant.WORLD_TICK;
    }

    public void awake() {

    }

    public void start() {

    }

    public void update() {

        if (currentTime >= WORLD_TICK) {
            this.tick = true;
            currentTime = 0f;
        } else {
            this.tick = false;
            currentTime += Gdx.graphics.getDeltaTime();
        }
    }

    public void render() {

    }

    // Tick \\

    public boolean tick() {
        return tick;
    }
}
