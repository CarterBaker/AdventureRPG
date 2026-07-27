package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.StructPackage;

public class WeatherPatternStruct extends StructPackage {

    /*
     * One persistent, large-scale weather system shared by the overhead
     * volumetric layer and the sky dome. Position and UBO slot are fixed
     * for its lifetime. The per-cell lobe geometry this used to carry has
     * been removed entirely — a pattern currently only tracks which
     * weather it has resolved to, its drift, and its fade/intensity/spread
     * state. The new single-mesh overhead and sky renderers will read
     * their own geometry from here once that design lands.
     *
     * intensity already folds in the resolved weather's own cloudCoverage;
     * spread is coverage-independent band purity (0 at the edge of the
     * zone this pattern's weather owns, 1 at its center). getIntensity()
     * also folds in a transition damper so a weather swap reads as
     * thickening/clearing rather than an instant pop.
     */

    public static final float WEATHER_TRANSITION_DURATION_SECONDS = 10.0f;

    private static final float TRANSITION_DIP_STRENGTH = 0.6f;

    private final long patternKey;

    private WeatherHandle weatherHandle;
    private WeatherHandle previousWeatherHandle;
    private float transitionT;

    private final int homeChunkX;
    private final int homeChunkZ;

    private float driftSpeedScale;

    private final int slot;

    private double driftChunkX;
    private double driftChunkZ;

    private float fadeAlpha;
    private boolean retiring;

    private float intensity;
    private float targetIntensity;

    private float spread;
    private float targetSpread;

    private double nextReevaluationTime;

    public WeatherPatternStruct(
            long patternKey,
            int homeChunkX,
            int homeChunkZ,
            WeatherHandle weatherHandle,
            float driftSpeedScale,
            float intensity,
            float spread,
            int slot) {

        this.patternKey = patternKey;
        this.weatherHandle = weatherHandle;
        this.previousWeatherHandle = weatherHandle;
        this.transitionT = 1f;
        this.homeChunkX = homeChunkX;
        this.homeChunkZ = homeChunkZ;
        this.driftSpeedScale = driftSpeedScale;
        this.slot = slot;
        this.fadeAlpha = 0f;
        this.retiring = false;
        this.intensity = intensity;
        this.targetIntensity = intensity;
        this.spread = spread;
        this.targetSpread = spread;
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

    public void setDriftSpeedScale(float driftSpeedScale) {
        this.driftSpeedScale = driftSpeedScale;
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

    public void setTargetSpread(float targetSpread) {
        this.targetSpread = targetSpread;
    }

    public void advanceSpreadSmoothing(float alpha) {
        this.spread += (targetSpread - this.spread) * alpha;
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

    public int getHomeChunkX() {
        return homeChunkX;
    }

    public int getHomeChunkZ() {
        return homeChunkZ;
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
        return intensity * transitionDampingMultiplier();
    }

    public float getSpread() {
        return spread;
    }

    public WeatherHandle getWeatherHandle() {
        return weatherHandle;
    }

    private float transitionDampingMultiplier() {
        if (transitionT >= 1f)
            return 1f;
        return 1f - (float) Math.sin(transitionT * Math.PI) * TRANSITION_DIP_STRENGTH;
    }
}