package application.bootstrap.weatherpipeline.weather;

import engine.root.DataPackage;
import engine.util.mathematics.vectors.Vector3;

public class WeatherData extends DataPackage {

    /*
     * Immutable weather definition loaded from JSON. Holds the shader-facing
     * cloud and precipitation values for one named weather, plus its
     * selection chance used by RegionSampleBranch's weighted blend. Owned by
     * WeatherHandle for the engine lifetime.
     */

    // Internal
    private final String weatherName;
    private final short weatherID;

    // Cloud
    private final CloudType cloudType;
    private final float cloudCoverage;
    private final Vector3 cloudColor;

    // Atmosphere
    private final float precipitationIntensity;
    private final float windSpeedScale;
    private final float fogDensityScale;

    // Selection
    private final float chance;

    // Constructor \\

    public WeatherData(
            String weatherName,
            short weatherID,
            CloudType cloudType,
            float cloudCoverage,
            Vector3 cloudColor,
            float precipitationIntensity,
            float windSpeedScale,
            float fogDensityScale,
            float chance) {

        // Internal
        this.weatherName = weatherName;
        this.weatherID = weatherID;

        // Cloud
        this.cloudType = cloudType;
        this.cloudCoverage = cloudCoverage;
        this.cloudColor = cloudColor;

        // Atmosphere
        this.precipitationIntensity = precipitationIntensity;
        this.windSpeedScale = windSpeedScale;
        this.fogDensityScale = fogDensityScale;

        // Selection
        this.chance = chance;
    }

    // Accessible \\

    public String getWeatherName() {
        return weatherName;
    }

    public short getWeatherID() {
        return weatherID;
    }

    public CloudType getCloudType() {
        return cloudType;
    }

    public float getCloudCoverage() {
        return cloudCoverage;
    }

    public Vector3 getCloudColor() {
        return cloudColor;
    }

    public float getPrecipitationIntensity() {
        return precipitationIntensity;
    }

    public float getWindSpeedScale() {
        return windSpeedScale;
    }

    public float getFogDensityScale() {
        return fogDensityScale;
    }

    public float getChance() {
        return chance;
    }
}