package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.StructPackage;

public class WeatherPatternStruct extends StructPackage {

    /*
     * One persistent, large-scale weather system shared by the overhead
     * volumetric layer and the sky dome. Position and UBO slot are fixed for
     * its lifetime. Lobes are regenerated whenever the pattern's resolved
     * weather changes, so the overhead cloud layer actually reflects live
     * weather instead of freezing at spawn time; previousWeatherHandle and
     * transitionT let shading blend smoothly across that change. Bounding
     * geometry (footprint radius, altitude range) is recomputed alongside
     * the lobes and is the single source of truth the sky dome preview
     * derives its own box from, so the two visual layers can never disagree
     * about where or how large a pattern's weather actually is.
     */

    public static final float WEATHER_TRANSITION_DURATION_SECONDS = 10.0f;

    private final long patternKey;

    private WeatherHandle weatherHandle;
    private WeatherHandle previousWeatherHandle;
    private float transitionT;

    private final int homeChunkX;
    private final int homeChunkZ;

    private WeatherPatternLobeStruct[] lobes;
    private float driftSpeedScale;
    private int generation;
    private int previousLobeCount;

    private final int slot;

    private double driftChunkX;
    private double driftChunkZ;

    private float fadeAlpha;
    private boolean retiring;

    private float intensity;
    private float targetIntensity;

    private double nextReevaluationTime;

    // Bounding Geometry
    private float footprintRadiusChunks;
    private float altitudeCenter;
    private float altitudeHalfThickness;

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
        this.generation = 0;
        this.previousLobeCount = lobes.length;
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

    /*
     * Replaces this pattern's lobes — called whenever its resolved weather
     * changes so the overhead volumetric layer actually shows clouds
     * matching current weather rather than whatever was rolled at spawn.
     * previousLobeCount is recorded so OverheadManager can find and remove
     * every cell key the old lobe array occupied before the new one is
     * added under the same indices.
     */
    public void refreshLobes(WeatherPatternLobeStruct[] newLobes) {
        this.previousLobeCount = this.lobes.length;
        this.lobes = newLobes;
        this.generation++;
    }

    public void setDriftSpeedScale(float driftSpeedScale) {
        this.driftSpeedScale = driftSpeedScale;
    }

    public void setBounds(float footprintRadiusChunks, float altitudeCenter, float altitudeHalfThickness) {
        this.footprintRadiusChunks = footprintRadiusChunks;
        this.altitudeCenter = altitudeCenter;
        this.altitudeHalfThickness = altitudeHalfThickness;
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

    public int getPreviousLobeCount() {
        return previousLobeCount;
    }

    public int getGeneration() {
        return generation;
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

    public float getFootprintRadiusChunks() {
        return footprintRadiusChunks;
    }

    public float getAltitudeCenter() {
        return altitudeCenter;
    }

    public float getAltitudeHalfThickness() {
        return altitudeHalfThickness;
    }
}