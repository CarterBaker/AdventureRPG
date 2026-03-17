package com.internal.runtime.world;

import com.internal.bootstrap.entitypipeline.playermanager.PlayerManager;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import com.internal.core.engine.SystemPackage;

public class WorldSystem extends SystemPackage {

    /*
     * Triggers world streaming startup at runtime. Gets the spawned player
     * from PlayerManager and registers it as the focal entity for the first
     * grid. Owned by RuntimeContext — runs after PlayerSystem so the player
     * is guaranteed to exist.
     */

    // Internal
    private PlayerManager playerManager;
    private WorldStreamManager worldStreamManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.playerManager = get(PlayerManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    @Override
    protected void awake() {
        worldStreamManager.createGrid(playerManager.getPlayer());
    }
}