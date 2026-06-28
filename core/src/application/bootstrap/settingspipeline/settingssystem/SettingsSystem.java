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
     * that flushes this UBO to the GPU. onRenderSettingsChanged() rebuilds
     * the grid then calls it, so both are always in sync.
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

        pushEngineSettings();
        pushRenderSettings();
    }

    // Settings \\

    /*
     * Writes compile-time engine constants into the UBO buffer once on awake.
     * These values never change at runtime so they are never re-written.
     * No GPU flush here — the following pushRenderSettings() call uploads
     * the full buffer, carrying these values along with it.
     */
    private void pushEngineSettings() {

        // Source: EngineSetting.CHUNK_SIZE — compile-time constant (16).
        // Cast to float so the shader can use it in division without a cast.
        renderSettingsData.updateUniform("u_chunkSize", (float) EngineSetting.CHUNK_SIZE);
    }

    /*
     * The only method that flushes RenderSettingsData to the GPU.
     * Add every new runtime render setting uniform here and nowhere else.
     */
    private void pushRenderSettings() {

        // Source: application Settings — runtime, user-configurable
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