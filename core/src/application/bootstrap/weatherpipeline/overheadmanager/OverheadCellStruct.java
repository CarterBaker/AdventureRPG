package application.bootstrap.weatherpipeline.overheadmanager;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.StructPackage;

class OverheadCellStruct extends StructPackage {

    /*
     * One streamed cell in the overhead cloud grid. Represents a single
     * physical patch of the world's cloud layer — never a single visual
     * cloud object; Stage 2/3 decide how many actual cloud instances a
     * cell renders as. Holds a persistent, non-reblending WeatherHandle
     * identity (resolved once via WeatherManager.resolveWeatherBand().
     * getPrimary() at stream-in time — see OverheadManager.streamInCell())
     * and the CloudHandle + effective altitude picked from that weather's
     * chance-weighted cloud pool at that same moment. All three stay fixed
     * for the cell's entire lifetime — a cell never re-rolls its own
     * weather or cloud choice; only the region sampling used for
     * horizon/skybox rendering reblends continuously.
     *
     * homeChunkX/homeChunkZ is the fixed world-space chunk-coordinate
     * center this cell was streamed in at. driftChunkX/driftChunkZ is how
     * far the cell's visual position has since drifted from that home
     * center, advanced every frame by the same local wind vector every
     * other wind-aware system reads — see OverheadManager.advanceWindDrift().
     * The cell is retired once its drifted world position exits the
     * streaming radius (measured from home, not drift, so retirement never
     * depends on drift direction) — see OverheadManager.advanceFadesAndRetire()
     * — so unbounded drift is never actually reached; a fresh cell streams
     * back in on the upwind edge to replace it.
     *
     * randomSeed is a stable per-cell float handed to the Stage 3 render
     * system so each cell's cloud instance(s) can vary shape/warp without
     * ever re-rolling and without every cell in the world looking
     * identical.
     *
     * fadeAlpha ramps 0 -> 1 over the cell's first moments alive and
     * 1 -> 0 just before it is retired, so streaming pop-in/pop-out at the
     * ring edge is never visually abrupt. Owned and mutated exclusively by
     * OverheadManager.
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

    // Constructor \\

    OverheadCellStruct(
            long cellKey,
            int homeChunkX,
            int homeChunkZ,
            WeatherHandle weatherHandle,
            CloudHandle cloudHandle,
            float effectiveAltitude,
            float randomSeed) {

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
}