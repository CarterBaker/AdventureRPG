package application.bootstrap.weatherpipeline;

import application.bootstrap.weatherpipeline.cloudmanager.CloudManager;
import application.bootstrap.weatherpipeline.overheadmanager.OverheadManager;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.weatherpipeline.windmanager.WindManager;
import engine.root.PipelinePackage;

public class WeatherPipeline extends PipelinePackage {

    /*
     * Registers the wind, weather, season, overhead-cloud-grid, and cloud
     * volume managers. WindManager has no per-frame dependency on
     * WeatherManager beyond a read-only speed-scale lookup in
     * LocalWindBranch. WeatherManager depends on ClockManager (season
     * resolution) and BiomeManager (seasonal weather pools), both resolved
     * during its get() phase — so this pipeline must bootstrap after
     * CalendarPipeline and WorldPipeline.
     *
     * OverheadManager is created last, after WeatherManager — both are
     * siblings under this same pipeline, so their update() calls run in
     * creation order within the same frame. That ordering guarantees
     * OverheadManager always reads a reference coordinate WeatherManager
     * has already refreshed this frame, never last frame's stale value.
     * It depends on WeatherManager (weather/cloud resolution) and
     * WorldManager (world bounds, so streamed cells never land outside the
     * world's own PNG-defined extent).
     */

    @Override
    protected void create() {
        create(CloudManager.class);
        create(WindManager.class);
        create(WeatherManager.class);
        create(SeasonManager.class);
        create(OverheadManager.class);
    }
}