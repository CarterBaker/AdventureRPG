package application.bootstrap.weatherpipeline;

import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import engine.root.PipelinePackage;

public class WeatherPipeline extends PipelinePackage {

    /*
     * Registers the weather manager. WeatherManager depends on ClockManager
     * (season resolution) and BiomeManager (seasonal weather pools), both
     * resolved during its get() phase — so this pipeline must bootstrap
     * after CalendarPipeline and WorldPipeline.
     */

    @Override
    protected void create() {
        create(WeatherManager.class);
    }
}