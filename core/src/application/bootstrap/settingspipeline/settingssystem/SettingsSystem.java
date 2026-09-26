package application.bootstrap.settingspipeline.settingssystem;

import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.settings.SettingsUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SettingsSystem extends SystemPackage {

    /*
     * Applies user settings to the running engine and writes them to disk.
     * Every settings change routes through one method per category: display
     * mode through the window platform, field of view onto every window's
     * camera, render settings through onRenderSettingsChanged(), and
     * saveSettings() for the file. Owns the RenderSettingsData UBO — the
     * single source of truth for render settings on the GPU. The grid is only
     * rebuilt when the render distance actually differs from the one last
     * applied, since a rebuild reloads every chunk around the player.
     */

    // Internal
    private UBOManager uboManager;
    private WorldStreamManager worldStreamManager;
    private WindowManager windowManager;

    // UBO
    private UBOHandle renderSettingsData;

    // Render State
    private int appliedRenderDistance;

    // Internal \\

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void awake() {

        this.renderSettingsData = uboManager.getUBOHandleFromUBOName(EngineSetting.SETTINGS_UBO);
        this.appliedRenderDistance = settings.maxRenderDistance;

        pushEngineSettings();
        pushRenderSettings();
    }

    // Settings \\

    private void pushEngineSettings() {

        // Source: EngineSetting.CHUNK_SIZE — compile-time constant (16).
        // Cast to float so the shader can use it in division without a cast.
        renderSettingsData.updateUniform(EngineSetting.UNIFORM_CHUNK_SIZE, (float) EngineSetting.CHUNK_SIZE);
    }

    private void pushRenderSettings() {

        // Source: application Settings — runtime, user-configurable
        renderSettingsData.updateUniform(EngineSetting.UNIFORM_RENDER_DISTANCE, (float) settings.maxRenderDistance);
        renderSettingsData.updateUniform(
                EngineSetting.UNIFORM_NEAR_TESSELLATION_RADIUS,
                (float) settings.nearTessellationRadius);
        uboManager.push(renderSettingsData);
    }

    public void onRenderSettingsChanged() {

        if (settings.maxRenderDistance != appliedRenderDistance)
            rebuildGrids();

        pushRenderSettings();
    }

    private void rebuildGrids() {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();

        for (int i = 0; i < grids.size(); i++)
            worldStreamManager.rebuildGrid(grids.get(i));

        this.appliedRenderDistance = settings.maxRenderDistance;
    }

    // Display \\

    public void applyFullscreen() {
        internal.windowPlatform.setFullscreen(settings.fullscreen);
    }

    public void applyVsync() {
        internal.windowPlatform.setVsync(settings.vsync);
    }

    // Camera \\

    public void applyFieldOfView() {

        ObjectArrayList<WindowInstance> windows = windowManager.getWindows();

        for (int i = 0; i < windows.size(); i++)
            windows.get(i).getActiveCamera().setFOV(settings.FOV);
    }

    // Bindings \\

    public void resetBindings() {
        SettingsUtility.resetBindings(settings);
    }

    // Persistence \\

    public void saveSettings() {
        SettingsUtility.flushBindings(settings);
        SettingsUtility.save(internal.settingsFile, settings, internal.gson);
    }
}
