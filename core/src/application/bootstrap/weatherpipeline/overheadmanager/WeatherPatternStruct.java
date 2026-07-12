package application.bootstrap.weatherpipeline.overheadmanager;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.StructPackage;

class WeatherPatternStruct extends StructPackage {

    /*
     * One persistent, large-scale weather system — a whole storm, a whole
     * fair-weather puff field, and so on — never a single small cloud. See
     * OverheadManager's own "Stage 1 retrofit" doc comment for why this
     * replaced the previous design's dense grid of small, independently-
     * resolved cells.
     *
     * Owns one fixed WeatherHandle identity for its entire lifetime,
     * resolved once at stream-in and periodically re-checked on its own
     * jittered cadence (see OverheadManager.advanceWeatherReevaluation())
     * — exactly the "persistent, non-reblending identity" the original
     * per-cell design already provided, just now at pattern granularity.
     * Both that initial resolution and every later recheck go through
     * WeatherManager.resolveWeatherBandTowardHorizon() rather than the
     * plain resolveWeatherBand() — a pattern streamed in near the player
     * shows the weather that is actually there; one streamed in near the
     * outer edge of the streaming radius instead agrees with whatever the
     * sky dome is already showing along that bearing, for as long as the
     * pattern lives.
     *
     * A pattern's visual shape is approximated by a small, fixed array of
     * lobes (see WeatherPatternLobeStruct) built once at stream-in and
     * never re-rolled — each lobe is one physical cloud-volume mesh
     * instance, offset from the pattern's own home center, and each is
     * free to draw a different cloud archetype pulled from this same
     * weather's own chance-weighted cloud pool, so one pattern can
     * legitimately read as a mixed, organically-shaped mass rather than
     * one uniform cube.
     *
     * Drift, fade, retirement, and intensity are all owned here rather
     * than per-lobe, since every lobe belonging to one pattern must move,
     * fade in/out, and strengthen/weaken together, as one weather system
     * — see OverheadCellStruct, which reads all of this by live
     * delegation rather than holding any of its own mutable state.
     * driftSpeedScale is likewise resolved once here, as the average
     * CloudData.driftSpeedScale of every cloud-bearing lobe (falling back
     * to a neutral 1.0 if none drew a cloud) — a pattern moves through
     * the sky as one cohesive system, never as several independently-
     * timed cloud archetypes drifting apart from each other.
     */

    // Identity
    private final long patternKey;
    private final WeatherHandle weatherHandle;

    // Home Position — fixed for the pattern's lifetime
    private final int homeChunkX;
    private final int homeChunkZ;

    // Shape — fixed for the pattern's lifetime
    private final WeatherPatternLobeStruct[] lobes;
    private final float driftSpeedScale;

    // Drift — advanced every frame by wind, shared by every lobe
    private double driftChunkX;
    private double driftChunkZ;

    // Fade
    private float fadeAlpha;
    private boolean retiring;

    // Intensity — see OverheadManager.advanceIntensity()
    private float intensity;

    // Weather Reevaluation — see OverheadManager.advanceWeatherReevaluation().
    // The simulation-time (elapsedSimTime) at which this pattern should
    // next check whether the weather at its own home coordinate has
    // changed.
    private double nextReevaluationTime;

    // Constructor \\

    WeatherPatternStruct(
            long patternKey,
            int homeChunkX,
            int homeChunkZ,
            WeatherHandle weatherHandle,
            WeatherPatternLobeStruct[] lobes,
            float driftSpeedScale,
            float intensity) {

        // Identity
        this.patternKey = patternKey;
        this.weatherHandle = weatherHandle;

        // Home Position
        this.homeChunkX = homeChunkX;
        this.homeChunkZ = homeChunkZ;

        // Shape
        this.lobes = lobes;
        this.driftSpeedScale = driftSpeedScale;

        // Fade
        this.fadeAlpha = 0f;
        this.retiring = false;

        // Intensity
        this.intensity = intensity;
    }

    // Drift \\

    void advanceDrift(double deltaChunkX, double deltaChunkZ) {
        this.driftChunkX += deltaChunkX;
        this.driftChunkZ += deltaChunkZ;
    }

    // Fade \\

    void setRetiring(boolean retiring) {
        this.retiring = retiring;
    }

    void setFadeAlpha(float fadeAlpha) {
        this.fadeAlpha = fadeAlpha;
    }

    // Intensity \\

    void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    // Weather Reevaluation \\

    double getNextReevaluationTime() {
        return nextReevaluationTime;
    }

    void setNextReevaluationTime(double nextReevaluationTime) {
        this.nextReevaluationTime = nextReevaluationTime;
    }

    // Accessible \\

    long getPatternKey() {
        return patternKey;
    }

    WeatherHandle getWeatherHandle() {
        return weatherHandle;
    }

    int getHomeChunkX() {
        return homeChunkX;
    }

    int getHomeChunkZ() {
        return homeChunkZ;
    }

    WeatherPatternLobeStruct[] getLobes() {
        return lobes;
    }

    int getLobeCount() {
        return lobes.length;
    }

    float getDriftSpeedScale() {
        return driftSpeedScale;
    }

    double getCurrentChunkX() {
        return homeChunkX + driftChunkX;
    }

    double getCurrentChunkZ() {
        return homeChunkZ + driftChunkZ;
    }

    float getFadeAlpha() {
        return fadeAlpha;
    }

    boolean isRetiring() {
        return retiring;
    }

    float getIntensity() {
        return intensity;
    }
}