package application.bootstrap.weatherpipeline.season;

import engine.root.DataPackage;

public class SeasonData extends DataPackage {

    /*
     * Immutable climate definition for one season, loaded from JSON. Keyed
     * by the Season enum directly rather than a hashed ID — seasons are a
     * fixed four-value set, not an open-ended named registry. Feeds base
     * wind and temperature values that WindManager and WeatherManager read
     * each frame. Owned by SeasonHandle for the full engine session.
     */

    // Identity
    private final Season season;

    // Wind
    private final float baseWindSpeed;
    private final float windVariance;
    private final float prevailingWindDirectionDegrees;

    // Temperature
    private final float baseTemperature;
    private final float temperatureVariance;

    // Weather Influence
    private final float precipitationChanceScale;

    // Constructor \\

    public SeasonData(
            Season season,
            float baseWindSpeed,
            float windVariance,
            float prevailingWindDirectionDegrees,
            float baseTemperature,
            float temperatureVariance,
            float precipitationChanceScale) {

        // Identity
        this.season = season;

        // Wind
        this.baseWindSpeed = baseWindSpeed;
        this.windVariance = windVariance;
        this.prevailingWindDirectionDegrees = prevailingWindDirectionDegrees;

        // Temperature
        this.baseTemperature = baseTemperature;
        this.temperatureVariance = temperatureVariance;

        // Weather Influence
        this.precipitationChanceScale = precipitationChanceScale;
    }

    // Accessible \\

    public Season getSeason() {
        return season;
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
}