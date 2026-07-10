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
     * other its gust/direction-wobble amplitude. humidity and visibility
     * complete the full Weather property set (precipitation, wind,
     * humidity, visibility) — previously parsed per-Weather but never
     * actually carried through sampling; WeatherManager exposes both via
     * getHumidity()/getVisibility(). temperatureModifier is blended the
     * same way and read by TemperatureBranch as an additive term on top of
     * the season/diurnal/precipitation-cooling terms it already computes.
     *
     * Instances of this struct are never written to directly from a raw
     * noise resolution — see RegionSampleBranch.advanceSmoothing(). Each
     * externally-read sample glides toward a same-shaped "target" instance
     * every frame via lerpToward(); copyFrom() is used exactly once, the
     * first time a target is ever resolved, so the sky doesn't have to
     * visibly fade up from a zeroed default over the full smoothing window
     * at world start.
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
    private float humidity;
    private float visibility;

    // Temperature
    private float temperatureModifier;

    // Constructor \\

    WeatherSampleStruct() {
        this.cloudColor = new Vector3();
    }

    // Smoothing \\

    /*
     * Hard-copies every field from source. Used once per sample, the first
     * time it's ever resolved, so this sample doesn't have to glide up from
     * a zeroed (black, zero-coverage) default over
     * EngineSetting.WEATHER_SAMPLE_SMOOTHING_TIME_SECONDS at world start —
     * see RegionSampleBranch.advanceSmoothing().
     */
    void copyFrom(WeatherSampleStruct source) {
        this.cloudColor.set(source.cloudColor.x, source.cloudColor.y, source.cloudColor.z);
        this.cloudCoverage = source.cloudCoverage;
        this.cloudAltitude = source.cloudAltitude;
        this.precipitationIntensity = source.precipitationIntensity;
        this.windSpeedScale = source.windSpeedScale;
        this.windTurbulenceScale = source.windTurbulenceScale;
        this.fogDensityScale = source.fogDensityScale;
        this.humidity = source.humidity;
        this.visibility = source.visibility;
        this.temperatureModifier = source.temperatureModifier;
    }

    /*
     * Glides every field a fraction `alpha` of the way toward target's
     * current value. See RegionSampleBranch.advanceSmoothing() for how
     * alpha is derived from EngineSetting.WEATHER_SAMPLE_SMOOTHING_TIME_SECONDS
     * — this is what turns an instantaneous jump in the raw resolved
     * weather (a chance-band boundary crossing, a season pool swap, the sky
     * dome's own daily cloud reseed) into a multi-second glide everywhere
     * this sample is read, rather than every reader having to smooth it
     * independently.
     */
    void lerpToward(WeatherSampleStruct target, float alpha) {
        this.cloudColor.set(
                this.cloudColor.x + (target.cloudColor.x - this.cloudColor.x) * alpha,
                this.cloudColor.y + (target.cloudColor.y - this.cloudColor.y) * alpha,
                this.cloudColor.z + (target.cloudColor.z - this.cloudColor.z) * alpha);
        this.cloudCoverage += (target.cloudCoverage - this.cloudCoverage) * alpha;
        this.cloudAltitude += (target.cloudAltitude - this.cloudAltitude) * alpha;
        this.precipitationIntensity += (target.precipitationIntensity - this.precipitationIntensity) * alpha;
        this.windSpeedScale += (target.windSpeedScale - this.windSpeedScale) * alpha;
        this.windTurbulenceScale += (target.windTurbulenceScale - this.windTurbulenceScale) * alpha;
        this.fogDensityScale += (target.fogDensityScale - this.fogDensityScale) * alpha;
        this.humidity += (target.humidity - this.humidity) * alpha;
        this.visibility += (target.visibility - this.visibility) * alpha;
        this.temperatureModifier += (target.temperatureModifier - this.temperatureModifier) * alpha;
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

    float getHumidity() {
        return humidity;
    }

    void setHumidity(float humidity) {
        this.humidity = humidity;
    }

    float getVisibility() {
        return visibility;
    }

    void setVisibility(float visibility) {
        this.visibility = visibility;
    }

    float getTemperatureModifier() {
        return temperatureModifier;
    }

    void setTemperatureModifier(float temperatureModifier) {
        this.temperatureModifier = temperatureModifier;
    }
}