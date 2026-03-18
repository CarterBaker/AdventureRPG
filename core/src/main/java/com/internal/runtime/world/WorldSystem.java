package com.internal.runtime.world;

import com.internal.bootstrap.entitypipeline.playermanager.PlayerManager;
import com.internal.bootstrap.renderpipeline.windowmanager.WindowManager;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import com.internal.core.engine.SystemPackage;

public class WorldSystem extends SystemPackage {

    /*
     * Creates the world streaming grid at runtime startup for the player entity.
     * Passes the main window so the grid knows which camera to cull against.
     * Editor preview overrides this by creating a grid with its own window.
     */

    // Internal
    private PlayerManager playerManager;
    private WorldStreamManager worldStreamManager;
    private WindowManager windowManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.playerManager = get(PlayerManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void awake() {
        worldStreamManager.createGrid(
                playerManager.getPlayer(),
                windowManager.getMainWindow());
    }
}