// WeatherPipeline.java
package application.bootstrap.weatherpipeline;

import application.bootstrap.weatherpipeline.cloudmanager.CloudManager;
import application.bootstrap.weatherpipeline.overheadmanager.OverheadManager;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.weatherrendersystem.WeatherRenderSystem;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.weatherpipeline.weatherpatternmanager.WeatherPatternManager;
import application.bootstrap.weatherpipeline.windmanager.WindManager;
import engine.root.PipelinePackage;

public class WeatherPipeline extends PipelinePackage {

    /*
     * Registers the wind, weather, season, weather-pattern, overhead-render,
     * and weather-render managers in dependency order. WeatherPatternManager
     * is created before OverheadManager — both are siblings here, so their
     * update() calls fire in creation order within the same frame, and
     * OverheadManager always reacts to patterns WeatherPatternManager
     * already streamed in or retired that same frame, never last frame's.
     */

    @Override
    protected void create() {
        create(CloudManager.class);
        create(WindManager.class);
        create(WeatherManager.class);
        create(SeasonManager.class);
        create(WeatherPatternManager.class);
        create(OverheadManager.class);
        create(WeatherRenderSystem.class);
    }
}