package application.runtime.world;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.settingspipeline.settingssystem.SettingsSystem;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.grid.GridRangeStruct;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.runtime.RuntimeSetting;
import engine.root.SystemPackage;

public class WorldSystem extends SystemPackage {

    /*
     * Initializes the world stream grid for the context window at startup,
     * binding the world render target so chunk rendering composites
     * correctly. Retains the created GridInstance so other per-window
     * systems (lighting, sky) can read this window's own grid, and removes
     * the grid from streaming when the context closes. The grid follows the
     * play range unless useRange() points it elsewhere; a range chosen before
     * the grid exists is the one it is born on, so it is never laid twice.
     */

    // Internal
    private PlayerManager playerManager;
    private WorldStreamManager worldStreamManager;
    private FBOManager fboManager;

    // Render Target
    private FBOInstance worldFbo;

    // Grid
    private GridInstance gridInstance;
    private GridRangeStruct range;

    @Override
    protected void get() {

        // Internal
        this.playerManager = get(PlayerManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.fboManager = get(FBOManager.class);

        // Grid
        this.range = get(SettingsSystem.class).getPlayRange();
    }

    @Override
    protected void awake() {
        this.worldFbo = fboManager.cloneFbo(RuntimeSetting.FBO_WORLD, context.getWindow());

        int windowID = context.getWindow().getWindowID();
        this.gridInstance = worldStreamManager.createGrid(
                playerManager.getPlayerForWindow(windowID),
                context.getWindow(),
                worldFbo,
                range);
    }

    @Override
    protected void dispose() {
        worldStreamManager.removeGrid(gridInstance);
    }

    // Range \\

    public void useRange(GridRangeStruct range) {

        this.range = range;

        if (gridInstance != null)
            worldStreamManager.applyRange(gridInstance, range);
    }

    // Accessible \\

    public FBOInstance getWorldFbo() {
        return worldFbo;
    }

    public GridInstance getGridInstance() {
        return gridInstance;
    }
}
