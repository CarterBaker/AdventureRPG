package application.bootstrap.weatherpipeline.weather;

import engine.root.EngineSetting;
import engine.root.InstancePackage;

public class WeatherInstance extends InstancePackage {

    /*
     * One live weather occurrence — either a cell of the scrolling weather
     * image, fixed to its pixel in noise space so it travels rigidly with the
     * flow, or the single instance a grid holds for the weather overhead of
     * its own location. A cell's noise percentile never changes; only the
     * biome drifting beneath it can change its weather, and every visual and
     * atmospheric value then cross-fades from previousWeatherHandle to
     * weatherHandle across transitionT instead of cutting.
     */

    // Cell
    private long cellKey;
    private int cellX;
    private int cellZ;
    private float noisePercentile;
    private long retainedFrame;

    // Weather
    private boolean configured;
    private WeatherHandle weatherHandle;
    private WeatherHandle previousWeatherHandle;
    private float transitionT;

    // Base \\

    @Override
    protected void create() {
        this.configured = false;
    }

    // Constructor \\

    public void constructor(WeatherHandle weatherHandle) {
        this.weatherHandle = weatherHandle;
        this.previousWeatherHandle = weatherHandle;
        this.transitionT = 1f;
        this.configured = true;
    }

    // Cell \\

    public void assignCell(long cellKey, int cellX, int cellZ, float noisePercentile) {
        this.cellKey = cellKey;
        this.cellX = cellX;
        this.cellZ = cellZ;
        this.noisePercentile = noisePercentile;
    }

    public void retain(long frame) {
        this.retainedFrame = frame;
    }

    public long getCellKey() {
        return cellKey;
    }

    public int getCellX() {
        return cellX;
    }

    public int getCellZ() {
        return cellZ;
    }

    public float getNoisePercentile() {
        return noisePercentile;
    }

    public long getRetainedFrame() {
        return retainedFrame;
    }

    public boolean isConfigured() {
        return configured;
    }

    // Weather Transition \\

    public void beginWeatherTransition(WeatherHandle newWeatherHandle) {
        this.previousWeatherHandle = this.weatherHandle;
        this.weatherHandle = newWeatherHandle;
        this.transitionT = 0f;
    }

    public void advanceWeatherTransition(float deltaTime) {

        if (transitionT >= 1f)
            return;

        transitionT = Math.min(1f, transitionT + deltaTime / EngineSetting.WEATHER_TRANSITION_DURATION_SECONDS);
    }

    public float getEasedTransitionT() {

        if (transitionT >= 1f)
            return 1f;

        float t = Math.max(0f, transitionT);

        return t * t * (3f - 2f * t);
    }

    public WeatherHandle getPreviousWeatherHandle() {
        return previousWeatherHandle;
    }

    public WeatherHandle getWeatherHandle() {
        return weatherHandle;
    }

    // Blended Atmosphere \\

    public float getBlendedTemperatureModifier() {
        return lerp(previousWeatherHandle.getTemperatureModifier(), weatherHandle.getTemperatureModifier());
    }

    public float getBlendedPrecipitationIntensity() {
        return lerp(previousWeatherHandle.getPrecipitationIntensity(), weatherHandle.getPrecipitationIntensity());
    }

    public float getBlendedWindSpeedScale() {
        return lerp(previousWeatherHandle.getWindSpeedScale(), weatherHandle.getWindSpeedScale());
    }

    public float getBlendedWindTurbulenceScale() {
        return lerp(previousWeatherHandle.getWindTurbulenceScale(), weatherHandle.getWindTurbulenceScale());
    }

    public float getBlendedHumidity() {
        return lerp(previousWeatherHandle.getHumidity(), weatherHandle.getHumidity());
    }

    public float getBlendedVisibility() {
        return lerp(previousWeatherHandle.getVisibility(), weatherHandle.getVisibility());
    }

    public float getBlendedFogDensityScale() {
        return lerp(previousWeatherHandle.getFogDensityScale(), weatherHandle.getFogDensityScale());
    }

    public float getBlendedCloudCoverage() {
        return lerp(previousWeatherHandle.getCloudCoverage(), weatherHandle.getCloudCoverage());
    }

    // Utility \\

    private float lerp(float from, float to) {
        return from + (to - from) * getEasedTransitionT();
    }
}
