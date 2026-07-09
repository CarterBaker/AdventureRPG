// WeatherSampleStruct.java
package application.bootstrap.weatherpipeline.weathermanager;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

class WeatherSampleStruct extends StructPackage {

    /*
     * Pure data holder for one sampled weather region's blended shader-facing
     * values. RegionSampleBranch owns five instances — one per cardinal
     * direction plus the centre — and writes into them each update.
     * cloudColor and cloudAltitude are sourced from each blended weather's
     * primary cloud (see WeatherHandle.getPrimaryCloud()) — representative
     * single values for horizon tinting, not a full per-cloud breakdown.
     * windTurbulenceScale mirrors windSpeedScale — both are blended the
     * same way, one feeding LocalWindBranch's flat speed multiplier, the
     * other its gust/direction-wobble amplitude.
     */

    // Cloud
    private final Vector3 cloudColor;
    private float cloudCoverage;
    private float cloudAltitude;

    // Atmosphere
    private float precipitationIntensity;
    private float windSpeedScale;
    private float windTurbulenceScale;
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

    float getCloudAltitude() {
        return cloudAltitude;
    }

    void setCloudAltitude(float cloudAltitude) {
        this.cloudAltitude = cloudAltitude;
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

    float getWindTurbulenceScale() {
        return windTurbulenceScale;
    }

    void setWindTurbulenceScale(float windTurbulenceScale) {
        this.windTurbulenceScale = windTurbulenceScale;
    }

    float getFogDensityScale() {
        return fogDensityScale;
    }

    void setFogDensityScale(float fogDensityScale) {
        this.fogDensityScale = fogDensityScale;
    }
}