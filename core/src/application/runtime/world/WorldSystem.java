package application.runtime.world;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbo.FboManager;
import application.bootstrap.renderpipeline.fborendermanager.FboRenderManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.SystemPackage;

public class WorldSystem extends SystemPackage {

    private static final String WORLD_FBO = "MainScene";

    private PlayerManager playerManager;
    private WorldStreamManager worldStreamManager;
    private FboManager fboManager;
    private FboRenderManager fboRenderManager;

    private FboInstance worldFbo;

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.fboManager = get(FboManager.class);
        this.fboRenderManager = get(FboRenderManager.class);
    }

    @Override
    protected void awake() {
        int windowID = context.getWindow().getWindowID();
        worldStreamManager.createGrid(
                playerManager.getPlayerForWindow(windowID),
                context.getWindow());

        this.worldFbo = fboManager.getFbo(WORLD_FBO);
    }

    @Override
    protected void update() {
        fboRenderManager.pushFbo(worldFbo);
    }
}
