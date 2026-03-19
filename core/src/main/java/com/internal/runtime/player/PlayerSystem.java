package com.internal.runtime.player;

import com.internal.bootstrap.entitypipeline.playermanager.PlayerManager;
import com.internal.core.engine.SystemPackage;

public class PlayerSystem extends SystemPackage {

    /*
     * Triggers player spawning at runtime startup. Passes the context window
     * to spawnPlayer() so the player renders into whatever window this context
     * targets. The editor reuses RuntimeContext unchanged — it sets a different
     * window on the context before startup, and this system behaves identically.
     */

    // Internal
    private PlayerManager playerManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.playerManager = get(PlayerManager.class);
    }

    @Override
    protected void awake() {
        playerManager.spawnPlayer(context.getWindow());
    }
}