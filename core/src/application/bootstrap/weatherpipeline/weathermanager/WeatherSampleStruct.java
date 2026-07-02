package application.bootstrap.weatherpipeline.weathermanager;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

class WeatherSampleStruct extends StructPackage {

    /*
     * Pure data holder for one sampled weather region's blended shader-facing
     * values. RegionSampleBranch owns five instances — one per cardinal
     * direction plus the centre — and writes into them each update.
     */

    // Cloud
    private final Vector3 cloudColor;
    private float cloudCoverage;
    private float cloudType;

    // Atmosphere
    private float precipitationIntensity;
    private float windSpeedScale;
    private float fogDensityScale;

    // Constructor \\

    WeatherSampleStruct() {
        this.cloudColor = new Vector3();
    }

    // Accessible \\

    Vector3 getCloudColor() {
        return cloudColor;
    }

    void setCloudColor(float r, float g, float b) {
        cloudColor.set(r, g, b);
    }

    float getCloudCoverage() {
        return cloudCoverage;
    }

    void setCloudCoverage(float cloudCoverage) {
        this.cloudCoverage = cloudCoverage;
    }

    float getCloudType() {
        return cloudType;
    }

    void setCloudType(float cloudType) {
        this.cloudType = cloudType;
    }

    float getPrecipitationIntensity() {
        return precipitationIntensity;
    }

    void setPrecipitationIntensity(float precipitationIntensity) {
        this.precipitationIntensity = precipitationIntensity;
    }

    float getWindSpeedScale() {
        return windSpeedScale;
    }

    void setWindSpeedScale(float windSpeedScale) {
        this.windSpeedScale = windSpeedScale;
    }

    float getFogDensityScale() {
        return fogDensityScale;
    }

    void setFogDensityScale(float fogDensityScale) {
        this.fogDensityScale = fogDensityScale;
    }
}