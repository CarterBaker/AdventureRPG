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
     * Registers the weather managers in update order: weather flow, then each
     * grid's weather pattern, then wind and temperature, precipitation, and
     * finally the sky, which reads all of them.
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