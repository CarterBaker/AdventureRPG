package com.internal.runtime.player;

import com.internal.bootstrap.entitypipeline.playermanager.PlayerManager;
import com.internal.bootstrap.renderpipeline.windowmanager.WindowManager;
import com.internal.core.engine.SystemPackage;

public class PlayerSystem extends SystemPackage {

    /*
     * Triggers player spawning at runtime startup. Gets the main window and
     * passes it to spawnPlayer() — the player renders into the main game window.
     * Editor play panel overrides this behaviour by calling spawnPlayer() with
     * its own window directly.
     */

    // Internal
    private PlayerManager playerManager;
    private WindowManager windowManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.playerManager = get(PlayerManager.class);
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void awake() {
        playerManager.spawnPlayer(windowManager.getMainWindow());
    }
}