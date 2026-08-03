package application.bootstrap.weatherpipeline;

import application.bootstrap.weatherpipeline.cloudmanager.CloudManager;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.skymanager.SkyManager;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.weatherpipeline.weatherpatternmanager.WeatherPatternManager;
import application.bootstrap.weatherpipeline.windmanager.WindManager;
import engine.root.PipelinePackage;

public class WeatherPipeline extends PipelinePackage {

    /*
     * Registers the cloud, weather, season, weather-pattern, wind, and sky
     * managers in dependency order.
     *
     * Update order matters here independent of get()-phase wiring:
     * WindManager reads each grid's local WeatherInstance (blended wind
     * speed/turbulence scale) and its TemperatureInstance — both are only
     * current for this frame once WeatherPatternManager's own update() has
     * run. WeatherPatternManager is therefore registered (and so updated)
     * before WindManager, or wind/temperature would read one frame stale.
     * SkyManager reads that same per-grid temperature plus the calendar's
     * season blend, so it stays last — it should always see the most
     * current state every other weather system produced this frame.
     */

    @Override
    protected void create() {
        create(CloudManager.class);
        create(WeatherManager.class);
        create(SeasonManager.class);
        create(WeatherPatternManager.class);
        create(WindManager.class);
        create(SkyManager.class);
    }
}