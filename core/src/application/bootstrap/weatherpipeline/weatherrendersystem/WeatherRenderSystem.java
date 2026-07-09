package application.bootstrap.weatherpipeline.weatherrendersystem;

import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WeatherRenderSystem extends ManagerPackage {

    /*
     * Owns all weather-driven world-space rendering. Mirrors
     * WorldRenderManager's own shape: a thin per-frame driver that reads a
     * live streamed data set (OverheadManager's active cells, here, in
     * place of WorldStreamManager's active chunks) and pushes draw
     * submissions through the shared render pipeline for every active
     * grid window.
     *
     * All weather-specific CPU/GPU logic is delegated to CloudRenderSystem
     * — this class only sequences the two passes (rebuild once globally,
     * then submit once per window) and owns the one cross-pipeline lookup
     * CloudRenderSystem itself has no business reaching for
     * (WorldStreamManager lives in worldpipeline, not weatherpipeline).
     *
     * Belongs entirely to the weather pipeline — see WeatherPipeline. The
     * only points where this touches the shared render pipeline are the
     * ones that are genuinely unavoidable: reading each grid's own window
     * and world-scene fbo, and CloudRenderSystem's calls into
     * RenderManager.pushWeatherCall() to actually queue a draw.
     */

    // Internal
    private CloudRenderSystem cloudRenderSystem;
    private WorldStreamManager worldStreamManager;

    // Internal \\

    @Override
    protected void create() {
        this.cloudRenderSystem = create(CloudRenderSystem.class);
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    @Override
    protected void lateUpdate() {

        cloudRenderSystem.rebuildInstances();

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
            FboInstance worldFbo = grid.getRenderTargetFbo();

            if (window == null || worldFbo == null)
                continue;

            cloudRenderSystem.submit(worldFbo, window);
        }
    }
}