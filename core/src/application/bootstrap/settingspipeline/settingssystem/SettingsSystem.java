package application.bootstrap.settingspipeline.settingssystem;

import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SettingsSystem extends SystemPackage {

    /*
     * Owns the RenderSettingsData UBO — the single source of truth for
     * render settings on the GPU. pushRenderSettings() is the only method
     * that writes to this UBO. onRenderSettingsChanged() rebuilds the grid
     * then calls it, so both are always in sync.
     */

    // Internal
    private UBOManager uboManager;
    private WorldStreamManager worldStreamManager;

    // UBO
    private UBOHandle renderSettingsData;

    // Internal \\

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    @Override
    protected void awake() {
        this.renderSettingsData = uboManager.getUBOHandleFromUBOName(EngineSetting.SETTINGS_UBO);
        pushRenderSettings();
    }

    // Settings \\

    /*
     * The only method that writes to RenderSettingsData.
     * Add every new render setting uniform here and nowhere else.
     */
    private void pushRenderSettings() {
        renderSettingsData.updateUniform("u_renderDistance", (float) settings.maxRenderDistance);
        uboManager.push(renderSettingsData);
    }

    /*
     * Call this whenever render settings change at runtime.
     * Rebuilds the grid first so slot distances are correct,
     * then pushes the UBO so the shader denominator matches.
     */
    public void onRenderSettingsChanged() {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();

        for (int i = 0; i < grids.size(); i++)
            worldStreamManager.rebuildGrid(grids.get(i));

        pushRenderSettings();
    }
}