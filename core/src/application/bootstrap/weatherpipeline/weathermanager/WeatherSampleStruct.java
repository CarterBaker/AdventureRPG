// WeatherSampleStruct.java
package application.bootstrap.weatherpipeline.weathermanager;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

class WeatherSampleStruct extends StructPackage {

    /*
     * Pure data holder for one sampled weather region's blended shader-facing
     * values. RegionSampleBranch owns five instances — one per cardinal
     * direction plus the centre — and writes into them each update.
     *
     * cloudColor/cloudTopColor/cloudShadowColor and every cloud shading
     * field below (cloudDensity, cloudShadeStrength, cloudRimLightStrength,
     * cloudAmbientOcclusionStrength, cloudBrightnessMultiplier,
     * cloudToonBands, cloudDensityNoiseScale, cloudNoiseWarpStrength,
     * cloudCoverageBias, cloudSilhouetteSoftness) are all sourced from each
     * blended weather's primary cloud (see WeatherHandle.getPrimaryCloud())
     * — this is the FULL CloudData shading surface, not just a single
     * representative tint. Previously only cloudColor/cloudAltitude made it
     * this far, which is why every direction and every weather type used to
     * render with identical toon banding/shading regardless of which cloud
     * archetype was actually resolved there — see Clouds.glsl's own class
     * comment for the full account. cloudToonBands is carried as a float so
     * it can glide smoothly through advanceSmoothing() like everything
     * else; callers round it back to an int band count.
     *
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

    // Cloud — color
    private final Vector3 cloudColor;
    private final Vector3 cloudTopColor;
    private final Vector3 cloudShadowColor;
    private float cloudCoverage;
    private float cloudAltitude;

    // Cloud — shape/shading (full CloudData surface, see class comment)
    private float cloudDensity;
    private float cloudShadeStrength;
    private float cloudRimLightStrength;
    private float cloudAmbientOcclusionStrength;
    private float cloudBrightnessMultiplier;
    private float cloudToonBands;
    private float cloudDensityNoiseScale;
    private float cloudNoiseWarpStrength;
    private float cloudCoverageBias;
    private float cloudSilhouetteSoftness;

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
        this.cloudTopColor = new Vector3();
        this.cloudShadowColor = new Vector3();
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
        this.cloudTopColor.set(source.cloudTopColor.x, source.cloudTopColor.y, source.cloudTopColor.z);
        this.cloudShadowColor.set(source.cloudShadowColor.x, source.cloudShadowColor.y, source.cloudShadowColor.z);
        this.cloudCoverage = source.cloudCoverage;
        this.cloudAltitude = source.cloudAltitude;
        this.cloudDensity = source.cloudDensity;
        this.cloudShadeStrength = source.cloudShadeStrength;
        this.cloudRimLightStrength = source.cloudRimLightStrength;
        this.cloudAmbientOcclusionStrength = source.cloudAmbientOcclusionStrength;
        this.cloudBrightnessMultiplier = source.cloudBrightnessMultiplier;
        this.cloudToonBands = source.cloudToonBands;
        this.cloudDensityNoiseScale = source.cloudDensityNoiseScale;
        this.cloudNoiseWarpStrength = source.cloudNoiseWarpStrength;
        this.cloudCoverageBias = source.cloudCoverageBias;
        this.cloudSilhouetteSoftness = source.cloudSilhouetteSoftness;
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
        this.cloudTopColor.set(
                this.cloudTopColor.x + (target.cloudTopColor.x - this.cloudTopColor.x) * alpha,
                this.cloudTopColor.y + (target.cloudTopColor.y - this.cloudTopColor.y) * alpha,
                this.cloudTopColor.z + (target.cloudTopColor.z - this.cloudTopColor.z) * alpha);
        this.cloudShadowColor.set(
                this.cloudShadowColor.x + (target.cloudShadowColor.x - this.cloudShadowColor.x) * alpha,
                this.cloudShadowColor.y + (target.cloudShadowColor.y - this.cloudShadowColor.y) * alpha,
                this.cloudShadowColor.z + (target.cloudShadowColor.z - this.cloudShadowColor.z) * alpha);
        this.cloudCoverage += (target.cloudCoverage - this.cloudCoverage) * alpha;
        this.cloudAltitude += (target.cloudAltitude - this.cloudAltitude) * alpha;
        this.cloudDensity += (target.cloudDensity - this.cloudDensity) * alpha;
        this.cloudShadeStrength += (target.cloudShadeStrength - this.cloudShadeStrength) * alpha;
        this.cloudRimLightStrength += (target.cloudRimLightStrength - this.cloudRimLightStrength) * alpha;
        this.cloudAmbientOcclusionStrength += (target.cloudAmbientOcclusionStrength
                - this.cloudAmbientOcclusionStrength) * alpha;
        this.cloudBrightnessMultiplier += (target.cloudBrightnessMultiplier - this.cloudBrightnessMultiplier) * alpha;
        this.cloudToonBands += (target.cloudToonBands - this.cloudToonBands) * alpha;
        this.cloudDensityNoiseScale += (target.cloudDensityNoiseScale - this.cloudDensityNoiseScale) * alpha;
        this.cloudNoiseWarpStrength += (target.cloudNoiseWarpStrength - this.cloudNoiseWarpStrength) * alpha;
        this.cloudCoverageBias += (target.cloudCoverageBias - this.cloudCoverageBias) * alpha;
        this.cloudSilhouetteSoftness += (target.cloudSilhouetteSoftness - this.cloudSilhouetteSoftness) * alpha;
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

    Vector3 getCloudTopColor() {
        return cloudTopColor;
    }

    void setCloudTopColor(float r, float g, float b) {
        cloudTopColor.set(r, g, b);
    }

    Vector3 getCloudShadowColor() {
        return cloudShadowColor;
    }

    void setCloudShadowColor(float r, float g, float b) {
        cloudShadowColor.set(r, g, b);
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

    float getCloudDensity() {
        return cloudDensity;
    }

    void setCloudDensity(float cloudDensity) {
        this.cloudDensity = cloudDensity;
    }

    float getCloudShadeStrength() {
        return cloudShadeStrength;
    }

    void setCloudShadeStrength(float cloudShadeStrength) {
        this.cloudShadeStrength = cloudShadeStrength;
    }

    float getCloudRimLightStrength() {
        return cloudRimLightStrength;
    }

    void setCloudRimLightStrength(float cloudRimLightStrength) {
        this.cloudRimLightStrength = cloudRimLightStrength;
    }

    float getCloudAmbientOcclusionStrength() {
        return cloudAmbientOcclusionStrength;
    }

    void setCloudAmbientOcclusionStrength(float cloudAmbientOcclusionStrength) {
        this.cloudAmbientOcclusionStrength = cloudAmbientOcclusionStrength;
    }

    float getCloudBrightnessMultiplier() {
        return cloudBrightnessMultiplier;
    }

    void setCloudBrightnessMultiplier(float cloudBrightnessMultiplier) {
        this.cloudBrightnessMultiplier = cloudBrightnessMultiplier;
    }

    float getCloudToonBands() {
        return cloudToonBands;
    }

    void setCloudToonBands(float cloudToonBands) {
        this.cloudToonBands = cloudToonBands;
    }

    float getCloudDensityNoiseScale() {
        return cloudDensityNoiseScale;
    }

    void setCloudDensityNoiseScale(float cloudDensityNoiseScale) {
        this.cloudDensityNoiseScale = cloudDensityNoiseScale;
    }

    float getCloudNoiseWarpStrength() {
        return cloudNoiseWarpStrength;
    }

    void setCloudNoiseWarpStrength(float cloudNoiseWarpStrength) {
        this.cloudNoiseWarpStrength = cloudNoiseWarpStrength;
    }

    float getCloudCoverageBias() {
        return cloudCoverageBias;
    }

    void setCloudCoverageBias(float cloudCoverageBias) {
        this.cloudCoverageBias = cloudCoverageBias;
    }

    float getCloudSilhouetteSoftness() {
        return cloudSilhouetteSoftness;
    }

    void setCloudSilhouetteSoftness(float cloudSilhouetteSoftness) {
        this.cloudSilhouetteSoftness = cloudSilhouetteSoftness;
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