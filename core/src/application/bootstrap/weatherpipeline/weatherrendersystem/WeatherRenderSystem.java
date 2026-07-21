package application.bootstrap.weatherpipeline.weatherrendersystem;

import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WeatherRenderSystem extends ManagerPackage {

    /*
     * Owns all weather-driven world-space rendering. Overhead volumetric
     * clouds render into their own OverheadScene FBO — one clone per
     * window, created lazily here — and composite as their own
     * alpha-blended layer between the sky and the lit world. This keeps
     * clouds out of the deferred G-buffer entirely: lighting is computed
     * once, self-contained, inside CloudVolumeShader, and composited with
     * genuine hardware alpha blending rather than an approximated dither.
     *
     * All weather-specific CPU logic is delegated to CloudRenderSystem —
     * this class only sequences updating the shared instance data once
     * globally, then submitting and queuing a blit per window.
     */

    private static final String OVERHEAD_SCENE_FBO_NAME = "OverheadScene";
    private static final int OVERHEAD_COMPOSITE_LAYER = -5;

    // Internal
    private CloudRenderSystem cloudRenderSystem;
    private WorldStreamManager worldStreamManager;
    private FboManager fboManager;
    private FboRenderSystem fboRenderSystem;
    private RenderManager renderManager;

    // Per-Window Render Target
    private Object2ObjectOpenHashMap<WindowInstance, FboInstance> window2OverheadFbo;

    // Internal \\

    @Override
    protected void create() {
        this.cloudRenderSystem = create(CloudRenderSystem.class);
        this.window2OverheadFbo = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
        this.fboManager = get(FboManager.class);
        this.fboRenderSystem = get(FboRenderSystem.class);
        this.renderManager = get(RenderManager.class);
    }

    @Override
    protected void lateUpdate() {
        cloudRenderSystem.updateInstances();
        submitToGrids();
    }

    // Submit \\

    private void submitToGrids() {

        if (!worldStreamManager.hasGrids())
            return;

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int count = grids.size();

        for (int i = 0; i < count; i++) {

            GridInstance grid = (GridInstance) elements[i];
            WindowInstance window = grid.getWindowInstance();

            if (window == null)
                continue;

            FboInstance overheadFbo = getOrCreateOverheadFbo(window);

            // Keep the target fresh even on frames with zero active cloud
            // instances — otherwise a clear-sky frame would blit whatever
            // this FBO last held.
            renderManager.ensureFboRendered(overheadFbo, window);

            cloudRenderSystem.submit(overheadFbo, window);
            fboRenderSystem.pushFbo(overheadFbo, OVERHEAD_COMPOSITE_LAYER, window);
        }
    }

    // Overhead FBO \\

    private FboInstance getOrCreateOverheadFbo(WindowInstance window) {

        FboInstance existing = window2OverheadFbo.get(window);

        if (existing != null)
            return existing;

        FboInstance overheadFbo = fboManager.cloneFbo(OVERHEAD_SCENE_FBO_NAME, window);
        window2OverheadFbo.put(window, overheadFbo);

        return overheadFbo;
    }
}