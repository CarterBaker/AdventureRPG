package application.bootstrap.weatherpipeline;

import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.weatherpipeline.windmanager.WindManager;
import engine.root.PipelinePackage;

public class WeatherPipeline extends PipelinePackage {

    /*
     * Registers the wind and weather managers. WindManager has no
     * per-frame dependency on WeatherManager beyond a read-only speed-scale
     * lookup in LocalWindBranch; WeatherManager itself will depend on
     * WindManager once RegionSampleBranch is wired to consume local wind for
     * its drift direction (next step). WeatherManager also depends on
     * ClockManager (season resolution) and BiomeManager (seasonal weather
     * pools), both resolved during its get() phase — so this pipeline must
     * bootstrap after CalendarPipeline and WorldPipeline.
     */

    @Override
    protected void create() {
        create(WindManager.class);
        create(WeatherManager.class);
    }
}