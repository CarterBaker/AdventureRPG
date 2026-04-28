package application.runtime.world;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.runtime.RuntimeSetting;
import engine.root.SystemPackage;

public class WorldSystem extends SystemPackage {

    /*
     * Initializes the world stream grid for the context window at startup,
     * binding the world render target so chunk rendering composites correctly.
     */

    // Internal
    private PlayerManager playerManager;
    private WorldStreamManager worldStreamManager;
    private FboManager fboManager;

    // Render Target
    private FboInstance worldFbo;

    @Override
    protected void get() {

        // Internal
        this.playerManager = get(PlayerManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.fboManager = get(FboManager.class);
    }

    @Override
    protected void awake() {
        this.worldFbo = fboManager.getFbo(EngineSetting.FBO_WORLD);

        int windowID = context.getWindow().getWindowID();
        worldStreamManager.createGrid(
                playerManager.getPlayerForWindow(windowID),
                context.getWindow(),
                worldFbo);
    }
}