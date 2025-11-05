package com.AdventureRPG.Core.Framework;

import com.AdventureRPG.Core.RootManager;
import com.AdventureRPG.SettingsSystem.Settings;

public abstract class GameSystem extends DebugFramework {

    // Root
    protected Settings settings;
    protected RootManager rootManager;

    public void rootInit(
            Settings settings,
            RootManager rootManager) {

        // Root
        this.settings = settings;
        this.rootManager = rootManager;
    }

    // Base \\

    public void init() {
    }

    public void awake() {
    }

    public void start() {
    }

    public void menuExclusiveUpdate() {

    }

    public void gameExclusiveUpdate() {
    }

    public void update() {
    }

    public void fixedUpdate() {
    }

    public void lateUpdate() {
    }

    public void render() {
    }

    public void dispose() {
    }
}
