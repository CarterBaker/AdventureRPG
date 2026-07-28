package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector4;

public class WeatherPatternStruct extends StructPackage {

    /*
     * One persistent weather system — either a spatial cell streamed for
     * the visual weather map, or the single instance pinned to the
     * player's own reference coordinate for temperature and wind. Which
     * clouds a spatial pattern draws is read directly off
     * getWeatherHandle().getCloudEntries() — no separate per-pattern cloud
     * list is kept here. getIntensity() folds in a transition damper so a
     * weather swap reads as thickening/clearing rather than an instant pop
     * for cloud coverage. The getBlended*() family instead cross-fades a
     * raw previous/current weather value directly across the same window —
     * the right choice for a number like temperature or wind speed, where
     * dipping toward zero would read as a calm/cold snap that never happened.
     */

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

    private float distanceFromReferenceChunks;
    private final Vector4 bounds = new Vector4();

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
        transitionT = Math.min(1f, transitionT + deltaTime / EngineSetting.WEATHER_PATTERN_TRANSITION_DURATION_SECONDS);
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

    // Blended Atmosphere \\

    public float getBlendedTemperatureModifier() {
        return lerp(previousWeatherHandle.getTemperatureModifier(), weatherHandle.getTemperatureModifier(),
                transitionT);
    }

    public float getBlendedPrecipitationIntensity() {
        return lerp(previousWeatherHandle.getPrecipitationIntensity(), weatherHandle.getPrecipitationIntensity(),
                transitionT);
    }

    public float getBlendedWindSpeedScale() {
        return lerp(previousWeatherHandle.getWindSpeedScale(), weatherHandle.getWindSpeedScale(), transitionT);
    }

    public float getBlendedWindTurbulenceScale() {
        return lerp(previousWeatherHandle.getWindTurbulenceScale(), weatherHandle.getWindTurbulenceScale(),
                transitionT);
    }

    public float getBlendedHumidity() {
        return lerp(previousWeatherHandle.getHumidity(), weatherHandle.getHumidity(), transitionT);
    }

    public float getBlendedVisibility() {
        return lerp(previousWeatherHandle.getVisibility(), weatherHandle.getVisibility(), transitionT);
    }

    public float getBlendedFogDensityScale() {
        return lerp(previousWeatherHandle.getFogDensityScale(), weatherHandle.getFogDensityScale(), transitionT);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    // Spatial Data \\

    public void setDistanceFromReferenceChunks(float distanceFromReferenceChunks) {
        this.distanceFromReferenceChunks = distanceFromReferenceChunks;
    }

    public float getDistanceFromReferenceChunks() {
        return distanceFromReferenceChunks;
    }

    public void updateBounds() {

        float radius = getFootprintRadiusChunks();
        double currentX = getCurrentChunkX();
        double currentZ = getCurrentChunkZ();

        bounds.set(
                (float) (currentX - radius),
                (float) (currentZ - radius),
                (float) (currentX + radius),
                (float) (currentZ + radius));
    }

    public Vector4 getBounds() {
        return bounds;
    }

    public float getFootprintRadiusChunks() {
        return (EngineSetting.WEATHER_PATTERN_SKY_FOOTPRINT_CHUNKS * 0.5f) * weatherHandle.getVisualScale();
    }
}