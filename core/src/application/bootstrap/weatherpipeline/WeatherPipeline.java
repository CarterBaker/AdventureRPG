package application.bootstrap.weatherpipeline;

import application.bootstrap.weatherpipeline.cloudmanager.CloudManager;
import application.bootstrap.weatherpipeline.overheadmanager.OverheadManager;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.weatherrendersystem.WeatherRenderSystem;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.weatherpipeline.windmanager.WindManager;
import engine.root.PipelinePackage;

public class WeatherPipeline extends PipelinePackage {

    /*
     * Registers the wind, weather, season, overhead-cloud-grid, cloud
     * buffer, and weather render managers. WindManager has no per-frame
     * dependency on WeatherManager beyond a read-only speed-scale lookup in
     * LocalWindBranch. WeatherManager depends on ClockManager (season
     * resolution) and BiomeManager (seasonal weather pools), both resolved
     * during its get() phase — so this pipeline must bootstrap after
     * CalendarPipeline and WorldPipeline.
     *
     * CloudBufferManager is registered right after CloudManager — it owns
     * the GPU-side instanced buffer per cloud archetype that
     * WeatherRenderSystem's CloudRenderSystem rebuilds every frame from
     * OverheadManager's live cell grid.
     *
     * OverheadManager is created after WeatherManager — both are
     * siblings under this same pipeline, so their update() calls run in
     * creation order within the same frame. That ordering guarantees
     * OverheadManager always reads a reference coordinate WeatherManager
     * has already refreshed this frame, never last frame's stale value.
     * It depends on WeatherManager (weather/cloud resolution) and
     * WorldManager (world bounds, so streamed cells never land outside the
     * world's own PNG-defined extent).
     *
     * WeatherRenderSystem is created last. It owns no per-frame simulation
     * of its own — it only reads OverheadManager's already-updated cell
     * grid and CloudBufferManager's already-registered buffer map during
     * its own lateUpdate(), so creation order relative to either doesn't
     * strictly matter (all cross-manager lookups resolve in the later,
     * fully-deferred get() phase — see ManagerPackage/EnginePackage), but
     * it's placed last here to read top-to-bottom as "everything weather
     * rendering depends on, then weather rendering itself."
     */

    @Override
    protected void create() {
        create(CloudManager.class);
        create(WindManager.class);
        create(WeatherManager.class);
        create(SeasonManager.class);
        create(OverheadManager.class);
        create(WeatherRenderSystem.class);
    }
}