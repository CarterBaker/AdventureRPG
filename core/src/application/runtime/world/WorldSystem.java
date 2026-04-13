package application.runtime.world;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.SystemPackage;

public class WorldSystem extends SystemPackage {

    /*
     * Creates the world streaming grid at runtime startup for the player entity.
     * Passes the context window so the grid culls against the correct camera
     * regardless of which window this context was paired with.
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
        int windowID = context.getWindow().getWindowID();
        worldStreamManager.createGrid(
                playerManager.getPlayerForWindow(windowID),
                context.getWindow());
    }
}