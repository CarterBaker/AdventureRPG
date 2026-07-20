package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.NoiseUtility;

class GlobalNoiseBranch extends BranchPackage {

    /*
     * Owns the planet-scale motion behind the regional weather noise field.
     * Rotation is derived directly from accumulated drift distance divided
     * by the world's own circumference in chunks, so a full drift lap is
     * always exactly one full wrap regardless of world size. Meander runs
     * on its own separate, slow clock, matching WeatherNoiseUtility's own
     * expectation that the north-south wobble stay decoupled from rotation
     * speed. seasonalDriftZChunks applies that same idea across a full year
     * instead of a full rotation — the active world's own axial tilt
     * migrates the whole field north/south as the calendar progresses.
     * sampleGlobalIntensity() is a second, much coarser noise layer, using
     * the same circular embedding as the local field so it also wraps
     * seamlessly, just at a far lower frequency.
     */

    private static final long GLOBAL_INTENSITY_SEED = 0xB16B00B5DEADC0DEL;

    // Internal
    private WorldManager worldManager;
    private ClockManager clockManager;

    // World
    private WorldHandle activeWorld;
    private double worldWidthChunks;
    private float worldDriftChunksPerSecondX;

    // Motion State
    private double elapsedDriftChunksX;
    private double rotationAngleDegrees;
    private double meanderPhase;
    private double seasonalDriftZChunks;

    // Internal \\

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
        this.clockManager = get(ClockManager.class);
    }

    @Override
    protected void awake() {

        this.activeWorld = worldManager.getActiveWorld();

        if (activeWorld == null)
            throwException(
                    "GlobalNoiseBranch could not resolve an active world — weather noise has nothing to scroll against.");

        this.worldWidthChunks = activeWorld.getWorldScale().x / (double) EngineSetting.CHUNK_SIZE;

        if (worldWidthChunks <= 0.0)
            throwException(
                    "Active world resolved a non-positive width in chunks — cannot drive global weather rotation.");

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

    // Rotation \\

    private void advanceRotation(float deltaTime) {

        elapsedDriftChunksX += worldDriftChunksPerSecondX * deltaTime;
        elapsedDriftChunksX %= worldWidthChunks;

        rotationAngleDegrees = (elapsedDriftChunksX / worldWidthChunks) * EngineSetting.DEGREES_PER_FULL_ROTATION;
    }

    // Meander \\

    private void advanceMeander(float deltaTime) {

        double meanderPhaseSpeed = (Math.PI * 2.0) / EngineSetting.WEATHER_LOCAL_EVOLUTION_PERIOD;

        meanderPhase += meanderPhaseSpeed * deltaTime;
        meanderPhase %= (Math.PI * 2.0);
    }

    // Seasonal Drift \\

    private void advanceSeasonalDrift() {

        double yearProgress = clockManager.getClockHandle().getVisualYearProgress();
        double seasonWave = Math.sin(yearProgress * Math.PI * 2.0);
        double tiltFraction = activeWorld.getAxialTilt() / 90.0;

        this.seasonalDriftZChunks = seasonWave * tiltFraction * EngineSetting.GLOBAL_WEATHER_TILT_INFLUENCE
                * EngineSetting.WEATHER_NOISE_CELL_SIZE;
    }

    // Global Intensity \\

    float sampleGlobalIntensity(long chunkCoordinate) {

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        double spatialAngle = (chunkX / worldWidthChunks) * (Math.PI * 2.0);
        double embeddingRadius = worldWidthChunks / (Math.PI * 2.0 * EngineSetting.GLOBAL_WEATHER_NOISE_CELL_SIZE);

        double ex = Math.cos(spatialAngle) * embeddingRadius;
        double ey = Math.sin(spatialAngle) * embeddingRadius;
        double ez = chunkZ / EngineSetting.GLOBAL_WEATHER_NOISE_CELL_SIZE;

        float raw = NoiseUtility.noise3_ImproveXY(GLOBAL_INTENSITY_SEED, ex, ey, ez);

        return clamp01(raw * 0.5f + 0.5f);
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    // Accessible \\

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