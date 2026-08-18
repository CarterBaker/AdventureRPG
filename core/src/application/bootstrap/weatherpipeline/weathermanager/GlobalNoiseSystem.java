// GlobalNoiseSystem.java
package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.NoiseUtility;
import engine.util.mathematics.extras.SeamlessAxisNoiseUtility;

class GlobalNoiseSystem extends SystemPackage {

    /*
     * Drives the planet-scale motion behind the regional weather noise
     * field — rotation, meander, and seasonal drift — and exposes a
     * coarser second noise layer for global storm intensity. Wraps
     * seamlessly on both world axes the same way WeatherNoiseUtility does:
     * X rides the rotation circle, Z wraps through the shared
     * SeamlessAxisNoiseUtility.
     */

    private WorldManager worldManager;
    private ClockManager clockManager;

    private WorldHandle activeWorld;
    private double worldWidthChunks;
    private double worldHeightChunks;
    private float worldDriftChunksPerSecondX;

    private double elapsedDriftChunksX;
    private double rotationAngleDegrees;
    private double meanderPhase;
    private double seasonalDriftZChunks;

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
        this.clockManager = get(ClockManager.class);
    }

    @Override
    protected void awake() {

        this.activeWorld = worldManager.getActiveWorld();

        if (activeWorld == null)
            throwException("GlobalNoiseSystem could not resolve an active world.");

        this.worldWidthChunks = activeWorld.getWorldScale().x / (double) EngineSetting.CHUNK_SIZE;
        this.worldHeightChunks = activeWorld.getWorldScale().y / (double) EngineSetting.CHUNK_SIZE;

        if (worldWidthChunks <= 0.0 || worldHeightChunks <= 0.0)
            throwException("Active world resolved a non-positive width or height in chunks.");

        float driftMetersPerSecond = EngineSetting.WEATHER_BASE_DRIFT_SPEED_KPH
                * EngineSetting.KPH_TO_METERS_PER_SECOND;
        float driftChunksPerSecond = driftMetersPerSecond / (EngineSetting.BLOCK_SIZE * EngineSetting.CHUNK_SIZE);

        this.worldDriftChunksPerSecondX = driftChunksPerSecond * activeWorld.getRotationSpeed();
    }

    @Override
    protected void update() {

        float deltaTime = internal.getDeltaTime();

        advanceRotation(deltaTime);
        advanceMeander(deltaTime);
        advanceSeasonalDrift();
    }

    private void advanceRotation(float deltaTime) {

        elapsedDriftChunksX += worldDriftChunksPerSecondX * deltaTime;
        elapsedDriftChunksX %= worldWidthChunks;

        rotationAngleDegrees = (elapsedDriftChunksX / worldWidthChunks) * EngineSetting.DEGREES_PER_FULL_ROTATION;
    }

    private void advanceMeander(float deltaTime) {

        double meanderPhaseSpeed = (Math.PI * 2.0) / EngineSetting.WEATHER_LOCAL_EVOLUTION_PERIOD;

        meanderPhase += meanderPhaseSpeed * deltaTime;
        meanderPhase %= (Math.PI * 2.0);
    }

    private void advanceSeasonalDrift() {

        double yearProgress = clockManager.getClockHandle().getVisualYearProgress();
        double seasonWave = Math.sin(yearProgress * Math.PI * 2.0);
        double tiltFraction = activeWorld.getAxialTilt() / 90.0;

        this.seasonalDriftZChunks = seasonWave * tiltFraction * EngineSetting.GLOBAL_WEATHER_TILT_INFLUENCE
                * EngineSetting.WEATHER_NOISE_CELL_SIZE;
    }

    /*
     * Embeds chunkX on the same rotating circle the local/regional noise
     * uses (see WeatherNoiseUtility.sample) — without the rotation phase
     * added here too, this layer sampled a DIFFERENT effective angle than
     * the local layer for the exact same drifting pattern, since the local
     * layer's angle is already stable (position drift and rotation phase
     * cancel by design — see WeatherPatternManager.assignVelocity). That
     * mismatch let global storm intensity drift out from under an
     * otherwise-stable pattern, occasionally flipping its resolved weather
     * type mid-transit for no reason the CPU-side motion would explain.
     * Both layers now rotate identically, so a pattern's combined noise —
     * and therefore its weather type — stays exactly as stable while it
     * drifts as the design intends.
     */
    float sampleGlobalIntensity(long chunkCoordinate) {

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        double spatialAngle = (chunkX / worldWidthChunks) * (Math.PI * 2.0);
        double rotationPhaseRadians = (rotationAngleDegrees / EngineSetting.DEGREES_PER_FULL_ROTATION)
                * (Math.PI * 2.0);
        double angle = spatialAngle + rotationPhaseRadians;

        double embeddingRadius = worldWidthChunks / (Math.PI * 2.0 * EngineSetting.GLOBAL_WEATHER_NOISE_CELL_SIZE);

        double ex = Math.cos(angle) * embeddingRadius;
        double ey = Math.sin(angle) * embeddingRadius;

        double zWavelength = EngineSetting.GLOBAL_WEATHER_NOISE_CELL_SIZE;

        float raw = SeamlessAxisNoiseUtility.sample(
                chunkZ, zWavelength, worldHeightChunks, EngineSetting.NOISE_SEAM_BLEND_WAVELENGTHS,
                ez -> NoiseUtility.noise3_ImproveXY(EngineSetting.GLOBAL_WEATHER_INTENSITY_SEED, ex, ey, ez));

        return clamp01(raw * 0.5f + 0.5f);
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    float getGlobalInfluence() {
        return EngineSetting.GLOBAL_WEATHER_INFLUENCE;
    }

    double getRotationAngleDegrees() {
        return rotationAngleDegrees;
    }

    double getMeanderWaveNumber() {
        return EngineSetting.GLOBAL_WEATHER_MEANDER_WAVE_NUMBER;
    }

    double getMeanderPhase() {
        return meanderPhase;
    }

    double getSeasonalDriftZChunks() {
        return seasonalDriftZChunks;
    }

    float getWorldDriftChunksPerSecondX() {
        return worldDriftChunksPerSecondX;
    }
}