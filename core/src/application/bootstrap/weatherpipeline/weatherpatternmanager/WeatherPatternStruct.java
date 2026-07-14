package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.StructPackage;

public class WeatherPatternStruct extends StructPackage {

    /*
     * One persistent, large-scale weather system shared by the overhead
     * volumetric layer and the sky dome. Position, lobes, and UBO slot are
     * fixed for its lifetime. Its resolved WeatherHandle may be reassigned
     * by WeatherPatternManager as conditions change; previousWeatherHandle
     * and transitionT let consumers blend smoothly across that change
     * instead of popping instantly, and intensity eases toward
     * targetIntensity every frame rather than snapping whenever it's
     * resampled.
     */

    public static final float WEATHER_TRANSITION_DURATION_SECONDS = 10.0f;

    private final long patternKey;

    private WeatherHandle weatherHandle;
    private WeatherHandle previousWeatherHandle;
    private float transitionT;

    private final int homeChunkX;
    private final int homeChunkZ;

    private final WeatherPatternLobeStruct[] lobes;
    private final float driftSpeedScale;

    private final int slot;

    private double driftChunkX;
    private double driftChunkZ;

    private float fadeAlpha;
    private boolean retiring;

    private float intensity;
    private float targetIntensity;

    private double nextReevaluationTime;

    public WeatherPatternStruct(
            long patternKey,
            int homeChunkX,
            int homeChunkZ,
            WeatherHandle weatherHandle,
            WeatherPatternLobeStruct[] lobes,
            float driftSpeedScale,
            float intensity,
            int slot) {

        this.patternKey = patternKey;
        this.weatherHandle = weatherHandle;
        this.previousWeatherHandle = weatherHandle;
        this.transitionT = 1f;
        this.homeChunkX = homeChunkX;
        this.homeChunkZ = homeChunkZ;
        this.lobes = lobes;
        this.driftSpeedScale = driftSpeedScale;
        this.slot = slot;
        this.fadeAlpha = 0f;
        this.retiring = false;
        this.intensity = intensity;
        this.targetIntensity = intensity;
    }

    public void advanceDrift(double deltaChunkX, double deltaChunkZ) {
        this.driftChunkX += deltaChunkX;
        this.driftChunkZ += deltaChunkZ;
    }

    public void beginWeatherTransition(WeatherHandle newWeatherHandle) {
        this.previousWeatherHandle = this.weatherHandle;
        this.weatherHandle = newWeatherHandle;
        this.transitionT = 0f;
    }

    public void advanceWeatherTransition(float deltaTime) {
        if (transitionT >= 1f)
            return;
        transitionT = Math.min(1f, transitionT + deltaTime / WEATHER_TRANSITION_DURATION_SECONDS);
    }

    public float getTransitionT() {
        return transitionT;
    }

    public WeatherHandle getPreviousWeatherHandle() {
        return previousWeatherHandle;
    }

    public void setRetiring(boolean retiring) {
        this.retiring = retiring;
    }

    public void setFadeAlpha(float fadeAlpha) {
        this.fadeAlpha = fadeAlpha;
    }

    public void setTargetIntensity(float targetIntensity) {
        this.targetIntensity = targetIntensity;
    }

    public void advanceIntensitySmoothing(float alpha) {
        this.intensity += (targetIntensity - this.intensity) * alpha;
    }

    public double getNextReevaluationTime() {
        return nextReevaluationTime;
    }

    public void setNextReevaluationTime(double nextReevaluationTime) {
        this.nextReevaluationTime = nextReevaluationTime;
    }

    public long getPatternKey() {
        return patternKey;
    }

    public WeatherHandle getWeatherHandle() {
        return weatherHandle;
    }

    public int getHomeChunkX() {
        return homeChunkX;
    }

    public int getHomeChunkZ() {
        return homeChunkZ;
    }

    public WeatherPatternLobeStruct[] getLobes() {
        return lobes;
    }

    public int getLobeCount() {
        return lobes.length;
    }

    public float getDriftSpeedScale() {
        return driftSpeedScale;
    }

    public int getSlot() {
        return slot;
    }

    public double getCurrentChunkX() {
        return homeChunkX + driftChunkX;
    }

    public double getCurrentChunkZ() {
        return homeChunkZ + driftChunkZ;
    }

    public float getFadeAlpha() {
        return fadeAlpha;
    }

    public boolean isRetiring() {
        return retiring;
    }

    public float getIntensity() {
        return intensity;
    }
}