package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.StructPackage;

public class WeatherPatternStruct extends StructPackage {

    /*
     * One persistent, large-scale weather system shared by the overhead
     * volumetric layer and the sky dome. Position and UBO slot are fixed for
     * its lifetime. Lobes are regenerated whenever the pattern's resolved
     * weather changes. Bounding geometry (footprint radius, altitude range)
     * is recomputed alongside the lobes and is the single source of truth
     * the sky dome preview derives its own box from.
     *
     * intensity and spread are tracked separately. intensity already folds
     * in the resolved weather's own cloudCoverage and drives opacity/density
     * — a weather with low coverage should still read faint even at the
     * dead center of its own zone. spread is coverage-independent band
     * purity (0 at the edge of the zone this pattern's weather owns, 1 at
     * its center) and drives lobe geometry — how far lobes sit from the
     * pattern's own center and how large they are. Splitting these two
     * means a pattern approaching the boundary of its weather zone visibly
     * condenses toward its own center and shrinks, rather than keeping a
     * full-spread cluster of lobes that simply fades in alpha — which reads
     * as a static, hard-edged blob rather than a natural congregation.
     */

    public static final float WEATHER_TRANSITION_DURATION_SECONDS = 10.0f;

    // Lobes never fully collapse to a point nor fully vanish in size —
    // floors keep a condensing pattern reading as a small, tight cluster
    // rather than degenerating into a single stacked point.
    private static final float MIN_EFFECTIVE_SPREAD = 0.15f;
    private static final float MIN_LOBE_SIZE_RATIO = 0.45f;

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

    private float spread;
    private float targetSpread;

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
            float spread,
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

    public float getSpread() {
        return spread;
    }

    // Lobe Geometry \\

    private float effectiveSpread() {
        return MIN_EFFECTIVE_SPREAD + (1f - MIN_EFFECTIVE_SPREAD) * Math.max(0f, Math.min(1f, spread));
    }

    /*
     * Effective world chunk position of one lobe. Scales the lobe's fixed
     * offset from pattern center by this pattern's own band purity, so a
     * pattern sitting near the edge of its weather's zone pulls its lobes
     * in toward the center rather than showing the full-spread cluster
     * shape it would show at the zone's core. Both the sky dome and the
     * overhead volumetric system read lobe position exclusively through
     * this method, so the two visual layers can never disagree about
     * where a lobe actually sits.
     */
    public double getLobeChunkX(WeatherPatternLobeStruct lobe) {
        return getCurrentChunkX() + lobe.getOffsetChunkX() * effectiveSpread();
    }

    public double getLobeChunkZ(WeatherPatternLobeStruct lobe) {
        return getCurrentChunkZ() + lobe.getOffsetChunkZ() * effectiveSpread();
    }

    /*
     * Effective size variance of one lobe. Shrinks alongside spread so a
     * condensing pattern reads as fewer, smaller, tighter puffs rather than
     * full-size clouds merely fading in alpha.
     */
    public float getLobeSizeVariance(WeatherPatternLobeStruct lobe) {
        float sizeT = MIN_LOBE_SIZE_RATIO + (1f - MIN_LOBE_SIZE_RATIO) * Math.max(0f, Math.min(1f, spread));
        return lobe.getSizeVariance() * sizeT;
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