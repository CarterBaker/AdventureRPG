package application.bootstrap.weatherpipeline;

import application.bootstrap.weatherpipeline.cloudmanager.CloudManager;
import application.bootstrap.weatherpipeline.overheadmanager.OverheadManager;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.weatherpipeline.windmanager.WindManager;
import engine.root.PipelinePackage;

public class WeatherPipeline extends PipelinePackage {

    /*
     * Registers the wind, weather, season, and overhead-cloud-grid
     * managers. WindManager has no per-frame dependency on WeatherManager
     * beyond a read-only speed-scale lookup in LocalWindBranch;
     * WeatherManager itself will depend on WindManager once
     * RegionSampleBranch is wired to consume local wind for its drift
     * direction (next step). WeatherManager also depends on ClockManager
     * (season resolution) and BiomeManager (seasonal weather pools), both
     * resolved during its get() phase — so this pipeline must bootstrap
     * after CalendarPipeline and WorldPipeline. OverheadManager depends on
     * WeatherManager (weather resolution) and WorldManager (world scale for
     * wrap-safe grid math), both resolved the same way.
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