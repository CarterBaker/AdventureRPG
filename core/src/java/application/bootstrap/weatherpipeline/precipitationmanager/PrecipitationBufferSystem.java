package application.bootstrap.weatherpipeline.precipitationmanager;

import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.precipitation.PrecipitationInstance;
import application.bootstrap.weatherpipeline.weather.WeatherInstance;
import application.bootstrap.weatherpipeline.wind.WindInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

class PrecipitationBufferSystem extends SystemPackage {

    /*
     * Resolves what is falling on one grid and pushes it to that grid's
     * PrecipitationData UBO. Intensity is the local weather's blended
     * precipitation, so rain eases in and out with the weather transition;
     * the share that falls as snow eases in as the grid's own temperature
     * drops through freezing; the drift is that grid's local wind.
     */

    // Internal
    private UBOManager uboManager;

    // Base \\

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    // State \\

    void resolveState(GridInstance grid) {

        WeatherInstance weather = grid.getWeatherInstance();
        WindInstance wind = grid.getWindInstance();

        float intensity = weather.isConfigured()
                ? Math.max(0f, Math.min(1f, weather.getBlendedPrecipitationIntensity()))
                : 0f;
        float snow = resolveSnowShare(grid.getTemperatureInstance().getTemperature());

        Vector3 windDirection = wind.getLocalWindDirection();
        float drift = wind.getLocalWindSpeed() * EngineSetting.PRECIPITATION_WIND_DRIFT_SCALE;

        grid.getPrecipitationInstance().setState(intensity, snow, windDirection.x * drift, windDirection.z * drift);
    }

    private float resolveSnowShare(float temperature) {

        float low = EngineSetting.PRECIPITATION_SNOW_TEMPERATURE - EngineSetting.PRECIPITATION_SNOW_BLEND_RANGE;
        float high = EngineSetting.PRECIPITATION_SNOW_TEMPERATURE + EngineSetting.PRECIPITATION_SNOW_BLEND_RANGE;
        float t = Math.max(0f, Math.min(1f, (temperature - low) / (high - low)));

        return 1f - t * t * (3f - 2f * t);
    }

    // Push \\

    void pushData(GridInstance grid) {

        PrecipitationInstance precipitation = grid.getPrecipitationInstance();
        UBOInstance precipitationData = grid.getPrecipitationDataUBO();

        precipitationData.updateUniform(EngineSetting.UNIFORM_PRECIPITATION_COLUMNS, precipitation.getPackedColumns());
        precipitationData.updateUniform(EngineSetting.UNIFORM_PRECIPITATION_WINDOW, precipitation.getWindow());
        precipitationData.updateUniform(EngineSetting.UNIFORM_PRECIPITATION_STATE, precipitation.getState());

        uboManager.push(precipitationData);
    }
}
