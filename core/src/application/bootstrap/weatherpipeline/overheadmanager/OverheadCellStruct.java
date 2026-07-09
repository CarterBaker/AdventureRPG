package application.bootstrap.weatherpipeline.overheadmanager;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.StructPackage;

public class OverheadCellStruct extends StructPackage {

    /*
     * One streamed cell in the overhead cloud grid. Represents a single
     * physical patch of the world's cloud layer — never a single visual
     * cloud object; the render pipeline decides how many actual cloud
     * instances a cell renders as (currently: exactly one, or zero — see
     * below). Holds a persistent, non-reblending WeatherHandle identity
     * (resolved once via WeatherManager.resolveWeatherBand().getPrimary()
     * at stream-in time — see OverheadManager.streamInCell()) and the
     * CloudHandle + effective altitude picked from that weather's chance-
     * weighted cloud pool at that same moment. All three stay fixed for the
     * cell's entire lifetime — a cell never re-rolls its own weather or
     * cloud choice in place; only the region sampling used for horizon/skybox
     * rendering reblends continuously.
     *
     * That fixed identity does eventually change, though — see
     * nextReevaluationTime and OverheadManager.advanceWeatherReevaluation().
     * On a slow, per-cell jittered cadence, the manager re-resolves the
     * weather at this cell's own home coordinate; if it no longer matches
     * this cell's weatherHandle, the cell is retired through the exact same
     * fade-out path a cell that drifted out of streaming range uses. This is
     * deliberately never an in-place field swap — a cloud silently mutating
     * into a different type/color/altitude would look like a glitch, not
     * like weather changing. Fading out and letting a fresh cell fade back
     * in at that same physical slot (picking up whatever now resolves there)
     * is what makes weather form and dissipate over time rather than only
     * ever changing because the player walked away.
     *
     * cloudHandle may be null — a cell whose resolved weather defines no
     * clouds at all (a Clear weather; see WeatherHandle.hasClouds()) still
     * streams in and holds its weatherHandle identity like any other cell,
     * since the weather itself is active there even though nothing is
     * drawn. hasCloud() is the single source of truth for whether this
     * cell should ever reach CloudRenderSystem's instance buffers.
     *
     * Public rather than package-private — mirrors WeatherBandStruct's own
     * precedent — because the render pipeline (a different package) reads
     * these directly every frame to rebuild its instance buffers. Mutation
     * stays restricted to OverheadManager via package-private setters.
     *
     * homeChunkX/homeChunkZ is the fixed world-space chunk-coordinate
     * center this cell was streamed in at. driftChunkX/driftChunkZ is how
     * far the cell's visual position has since drifted from that home
     * center, advanced every frame by the same local wind vector every
     * other wind-aware system reads — see OverheadManager.advanceWindDrift().
     * The cell is retired once its home distance from the player exits the
     * streaming radius (measured from home, not drift, so retirement never
     * depends on drift direction) — see OverheadManager.advanceFadesAndRetire()
     * — so unbounded drift is never actually reached; a fresh cell streams
     * back in on the upwind edge to replace it.
     *
     * randomSeed is a stable per-cell float handed to the render system so
     * each cell's cloud instance can vary shape/warp without ever
     * re-rolling and without every cell in the world looking identical.
     *
     * fadeAlpha ramps 0 -> 1 over the cell's first moments alive and
     * 1 -> 0 just before it is retired, so streaming pop-in/pop-out at the
     * ring edge is never visually abrupt. Owned and mutated exclusively by
     * OverheadManager.
     *
     * intensity is a live, continuously-updated measure (see
     * OverheadManager.advanceIntensity()) of how strongly this cell's
     * weather is currently expressed at its own home coordinate — derived
     * fresh every recompute from WeatherBandStruct.getPrimaryIntensity(),
     * never stored/latched between recomputes. Unlike weatherHandle/
     * cloudHandle/effectiveAltitude, which are fixed for the cell's entire
     * lifetime, intensity is expected to rise and fall continuously as the
     * underlying noise field evolves — this is what lets a physical
     * weather system visibly strengthen and weaken over time rather than
     * only ever existing at full presence or not at all. It is recomputed
     * on a fast, shared cadence deliberately decoupled from
     * nextReevaluationTime's slow, per-cell-jittered identity check — see
     * OverheadManager.advanceIntensity() for why the two must stay
     * separate. A cell whose intensity decays near zero is retired there
     * exactly like an identity mismatch, so a weather system that has
     * genuinely weakened away dissipates rather than reviving in place.
     */

    // Identity
    private final long cellKey;
    private final WeatherHandle weatherHandle;
    private final CloudHandle cloudHandle;
    private final float effectiveAltitude;
    private final float randomSeed;

    // Home Position — fixed for the cell's lifetime
    private final int homeChunkX;
    private final int homeChunkZ;

    // Drift — advanced every frame by wind
    private double driftChunkX;
    private double driftChunkZ;

    // Fade
    private float fadeAlpha;
    private boolean retiring;

    // Intensity — see OverheadManager.advanceIntensity().
    private float intensity;

    // Weather Reevaluation — see OverheadManager.advanceWeatherReevaluation().
    // The simulation-time (elapsedSimTime) at which this cell should next
    // check whether the weather at its own home coordinate has changed.
    private double nextReevaluationTime;

    // Constructor \\

    OverheadCellStruct(
            long cellKey,
            int homeChunkX,
            int homeChunkZ,
            WeatherHandle weatherHandle,
            CloudHandle cloudHandle,
            float effectiveAltitude,
            float randomSeed,
            float intensity) {

        // Identity
        this.cellKey = cellKey;
        this.weatherHandle = weatherHandle;
        this.cloudHandle = cloudHandle;
        this.effectiveAltitude = effectiveAltitude;
        this.randomSeed = randomSeed;

        // Home Position
        this.homeChunkX = homeChunkX;
        this.homeChunkZ = homeChunkZ;

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

    public long getCellKey() {
        return cellKey;
    }

    public WeatherHandle getWeatherHandle() {
        return weatherHandle;
    }

    public CloudHandle getCloudHandle() {
        return cloudHandle;
    }

    public boolean hasCloud() {
        return cloudHandle != null;
    }

    public float getEffectiveAltitude() {
        return effectiveAltitude;
    }

    public float getRandomSeed() {
        return randomSeed;
    }

    public int getHomeChunkX() {
        return homeChunkX;
    }

    public int getHomeChunkZ() {
        return homeChunkZ;
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