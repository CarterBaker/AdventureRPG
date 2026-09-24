package application.bootstrap.weatherpipeline;

import application.bootstrap.weatherpipeline.cloudmanager.CloudManager;
import application.bootstrap.weatherpipeline.precipitationmanager.PrecipitationManager;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.skymanager.SkyManager;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.weatherpipeline.weatherpatternmanager.WeatherPatternManager;
import application.bootstrap.weatherpipeline.windmanager.WindManager;
import engine.root.PipelinePackage;

public class WeatherPipeline extends PipelinePackage {

    /*
     * Registers the cloud, weather, season, weather-pattern, wind,
     * precipitation, and sky managers in dependency order.
     *
     * Update order matters here independent of get()-phase wiring:
     * WeatherManager advances the shared weather flow before
     * WeatherPatternManager places every grid's window of cells against it.
     * WindManager then reads each grid's local WeatherInstance and its
     * TemperatureInstance, both only current once WeatherPatternManager has
     * updated this frame. PrecipitationManager reads that same local weather,
     * temperature, and wind to decide what falls and how it drifts.
     * SkyManager reads that same per-grid temperature
     * plus the calendar's season blend, so it stays last.
     */

    @Override
    protected void create() {
        create(CloudManager.class);
        create(WeatherManager.class);
        create(SeasonManager.class);
        create(WeatherPatternManager.class);
        create(WindManager.class);
        create(PrecipitationManager.class);
        create(SkyManager.class);
    }
}