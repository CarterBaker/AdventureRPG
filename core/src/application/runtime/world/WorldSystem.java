package application.runtime.world;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.runtime.RuntimeSetting;
import engine.root.SystemPackage;

public class WorldSystem extends SystemPackage {

    private PlayerManager playerManager;
    private WorldStreamManager worldStreamManager;
    private FboManager fboManager;

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.fboManager = get(FboManager.class);
    }

    @Override
    protected void awake() {
        int windowID = context.getWindow().getWindowID();
        worldStreamManager.createGrid(
                playerManager.getPlayerForWindow(windowID),
                context.getWindow(),
                fboManager.getFbo(RuntimeSetting.FBO_WORLD));
    }
}
