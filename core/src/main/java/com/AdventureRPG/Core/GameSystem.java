package com.AdventureRPG.Core;

import com.AdventureRPG.SettingsSystem.Settings;

public abstract class GameSystem extends RootCore {

    // Root
    protected Settings settings;
    protected RootManager rootManager;

    // Init \\

    @Override
    void internalInit(
            Settings settings,
            RootManager rootManager) {

        // Root
        this.settings = settings;
        this.rootManager = rootManager;

        init();
    }

    public void init() {
    }

    // Awake

    void internalAwake() {
        awake();
    }

    public void awake() {
    }

    // Start \\

    void internalStart() {
        start();
    }

    public void start() {
    }

    // Menu Exclusive Update \\

    void internalMenuExclusiveUpdate() {
        menuExclusiveUpdate();
    }

    public void menuExclusiveUpdate() {
    }

    // Game Exclusive Update \\

    void internalGameExclusiveUpdate() {
        gameExclusiveUpdate();
    }

    public void gameExclusiveUpdate() {
    }

    // Update \\

    void internalUpdate() {
        update();
    }

    public void update() {
    }

    // Fixed Update \\

    void internalFixedUpdate() {
        fixedUpdate();
    }

    public void fixedUpdate() {
    }

    // Late Update \\

    void internalLateUpdate() {
        lateUpdate();
    }

    public void lateUpdate() {
    }

    // Render \\

    void internalRender() {
        render();
    }

    public void render() {
    }

    // Dispose \\

    void internalDispose() {
        dispose();
    }

    public void dispose() {
    }
}
