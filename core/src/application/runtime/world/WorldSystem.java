package application.runtime.world;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.runtime.RuntimeSetting;
import engine.root.SystemPackage;

public class WorldSystem extends SystemPackage {

    /*
     * Initializes the world stream grid for the context window at startup,
     * binding the world render target so chunk rendering composites
     * correctly. Retains the created GridInstance so other per-window
     * systems (lighting, sky) can read this window's own grid.
     */

    // Internal
    private PlayerManager playerManager;
    private WorldStreamManager worldStreamManager;
    private FboManager fboManager;

    // Render Target
    private FboInstance worldFbo;

    // Grid
    private GridInstance gridInstance;

    @Override
    protected void get() {

        // Internal
        this.playerManager = get(PlayerManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.fboManager = get(FboManager.class);
    }

    @Override
    protected void awake() {
        this.worldFbo = fboManager.cloneFbo(RuntimeSetting.FBO_WORLD, context.getWindow());

        int windowID = context.getWindow().getWindowID();
        this.gridInstance = worldStreamManager.createGrid(
                playerManager.getPlayerForWindow(windowID),
                context.getWindow(),
                worldFbo);
    }

    // Accessible \\

    public FboInstance getWorldFbo() {
        return worldFbo;
    }

    public GridInstance getGridInstance() {
        return gridInstance;
    }
}