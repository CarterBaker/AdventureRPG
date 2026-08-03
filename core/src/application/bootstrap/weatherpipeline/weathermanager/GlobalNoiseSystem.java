// GlobalNoiseSystem.java
package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.NoiseUtility;

class GlobalNoiseSystem extends SystemPackage {

    /*
     * Drives the planet-scale motion behind the regional weather noise
     * field — rotation, meander, and seasonal drift — and exposes a
     * coarser second noise layer for global storm intensity. Wraps
     * seamlessly on both world axes the same way WeatherNoiseUtility
     * does: X rides the rotation circle, Y cross-fades across its seam.
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

    float sampleGlobalIntensity(long chunkCoordinate) {

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        double spatialAngle = (chunkX / worldWidthChunks) * (Math.PI * 2.0);
        double embeddingRadius = worldWidthChunks / (Math.PI * 2.0 * EngineSetting.GLOBAL_WEATHER_NOISE_CELL_SIZE);

        double ex = Math.cos(spatialAngle) * embeddingRadius;
        double ey = Math.sin(spatialAngle) * embeddingRadius;

        double zWavelength = EngineSetting.GLOBAL_WEATHER_NOISE_CELL_SIZE;
        double z = wrapIntoRange(chunkZ, worldHeightChunks);

        float direct = NoiseUtility.noise3_ImproveXY(
                EngineSetting.GLOBAL_WEATHER_INTENSITY_SEED, ex, ey, z / zWavelength);
        float oneWorldBack = NoiseUtility.noise3_ImproveXY(
                EngineSetting.GLOBAL_WEATHER_INTENSITY_SEED, ex, ey, (z - worldHeightChunks) / zWavelength);

        float t = (float) (z / worldHeightChunks);
        float raw = direct * (1f - t) + oneWorldBack * t;

        return clamp01(raw * 0.5f + 0.5f);
    }

    private static double wrapIntoRange(double value, double range) {
        double wrapped = value % range;
        if (wrapped < 0)
            wrapped += range;
        return wrapped;
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