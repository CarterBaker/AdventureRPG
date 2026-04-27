package application.runtime.world;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.SystemPackage;

public class WorldSystem extends SystemPackage {

    private PlayerManager playerManager;
    private WorldStreamManager worldStreamManager;

    @Override
    protected void get() {
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
