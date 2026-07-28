package application.bootstrap.weatherpipeline.season;

import engine.root.DataPackage;
import engine.util.mathematics.vectors.Vector3;

public class SeasonData extends DataPackage {

    /*
     * Immutable climate and sky-color definition for one named season,
     * loaded from JSON. Wind and temperature values drive WindManager and
     * WeatherManager; tintColor and sunriseColor are the season's own
     * contribution to the sky's blended color palette, consumed by the
     * weather pipeline's sky system. Season identity and calendar
     * ordering are defined by the active calendar instead — this class
     * only carries the values a named season contributes once active.
     */

    private final String seasonName;

    private final float baseWindSpeed;
    private final float windVariance;
    private final float prevailingWindDirectionDegrees;

    private final float baseTemperature;
    private final float temperatureVariance;

    private final float precipitationChanceScale;

    private final Vector3 tintColor;
    private final Vector3 sunriseColor;

    public SeasonData(
            String seasonName,
            float baseWindSpeed,
            float windVariance,
            float prevailingWindDirectionDegrees,
            float baseTemperature,
            float temperatureVariance,
            float precipitationChanceScale,
            Vector3 tintColor,
            Vector3 sunriseColor) {

        this.seasonName = seasonName;
        this.baseWindSpeed = baseWindSpeed;
        this.windVariance = windVariance;
        this.prevailingWindDirectionDegrees = prevailingWindDirectionDegrees;
        this.baseTemperature = baseTemperature;
        this.temperatureVariance = temperatureVariance;
        this.precipitationChanceScale = precipitationChanceScale;
        this.tintColor = tintColor;
        this.sunriseColor = sunriseColor;
    }

    public String getSeasonName() {
        return seasonName;
    }

    public float getBaseWindSpeed() {
        return baseWindSpeed;
    }

    public float getWindVariance() {
        return windVariance;
    }

    public float getPrevailingWindDirectionDegrees() {
        return prevailingWindDirectionDegrees;
    }

    public float getBaseTemperature() {
        return baseTemperature;
    }

    public float getTemperatureVariance() {
        return temperatureVariance;
    }

    public float getPrecipitationChanceScale() {
        return precipitationChanceScale;
    }

    public Vector3 getTintColor() {
        return tintColor;
    }

    public Vector3 getSunriseColor() {
        return sunriseColor;
    }
}