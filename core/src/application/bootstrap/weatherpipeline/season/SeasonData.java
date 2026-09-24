package application.bootstrap.weatherpipeline.season;

import engine.root.DataPackage;

public class SeasonData extends DataPackage {

    /*
     * Immutable climate and sky-color definition for one named season,
     * loaded from JSON. Wind and temperature values drive WindManager and
     * WeatherManager; skyPalette is the season's own sky and cloud colors
     * for every phase of the day, blended across the year by the weather
     * pipeline's sky system. Season identity and calendar
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

    private final SkyPaletteStruct skyPalette;

    public SeasonData(
            String seasonName,
            float baseWindSpeed,
            float windVariance,
            float prevailingWindDirectionDegrees,
            float baseTemperature,
            float temperatureVariance,
            float precipitationChanceScale,
            SkyPaletteStruct skyPalette) {

        this.seasonName = seasonName;
        this.baseWindSpeed = baseWindSpeed;
        this.windVariance = windVariance;
        this.prevailingWindDirectionDegrees = prevailingWindDirectionDegrees;
        this.baseTemperature = baseTemperature;
        this.temperatureVariance = temperatureVariance;
        this.precipitationChanceScale = precipitationChanceScale;
        this.skyPalette = skyPalette;
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

    public SkyPaletteStruct getSkyPalette() {
        return skyPalette;
    }
}