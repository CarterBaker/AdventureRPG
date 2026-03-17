package com.internal.runtime.player;

import com.internal.bootstrap.entitypipeline.playermanager.PlayerManager;
import com.internal.core.engine.SystemPackage;

public class PlayerSystem extends SystemPackage {

    /*
     * Triggers player spawning at runtime startup.
     * Owned by RuntimePipeline. Delegates entirely to PlayerManager.
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
        playerManager.spawnPlayer();
    }
}