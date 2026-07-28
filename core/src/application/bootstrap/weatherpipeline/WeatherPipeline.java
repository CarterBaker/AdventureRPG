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
     * Registers the cloud, wind, weather, season, weather-pattern, and
     * sky managers in dependency order.
     */

    @Override
    protected void create() {
        create(CloudManager.class);
        create(WindManager.class);
        create(WeatherManager.class);
        create(SeasonManager.class);
        create(WeatherPatternManager.class);
        create(SkyManager.class);
    }
}