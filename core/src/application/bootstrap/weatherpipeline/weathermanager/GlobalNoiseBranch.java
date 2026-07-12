package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;

/*
 * Owns the planet's rotation angle and slow seasonal/meander drift, folding
 * them into the toroidal weather noise field. Linear drift speed is derived
 * from a fixed real-world KPH baseline scaled by the world's own rotation
 * multiplier, not from raw degrees times world width, so drift speed stays
 * physically reasonable at any world size. The noise phase and every
 * pattern's physical position both read this same value, so they can never
 * fall out of lockstep.
 */
class GlobalNoiseBranch extends BranchPackage {

    private static final long NOISE_SEED = 0x9E3779B97F4A7C15L;
    private static final double WAVELENGTH_CHUNKS = 768.0;
    private static final double MEANDER_SPEED = 0.015;
    private static final double MEANDER_AMPLITUDE = 0.6;

    private WorldManager worldManager;
    private ClockManager clockManager;

    private float globalInfluence;
    private float tiltInfluence;

    private double rotationAngleDegrees;
    private double latitudePhase;
    private double meanderElapsed;

    // Internal \\

    @Override
    protected void create() {
        this.globalInfluence = EngineSetting.GLOBAL_WEATHER_INFLUENCE;
        this.tiltInfluence = EngineSetting.GLOBAL_WEATHER_TILT_INFLUENCE;
    }

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
        this.clockManager = get(ClockManager.class);
    }

    @Override
    protected void update() {
        advanceRotation();
        advanceTilt();
        advanceMeander();
    }

    // Rotation \\

    private void advanceRotation() {

        int worldWidthChunks = Math.max(1, worldWidthChunks());
        double degreesPerSecond = (getWorldDriftChunksPerSecondX() / worldWidthChunks)
                * EngineSetting.DEGREES_PER_FULL_ROTATION;

        this.rotationAngleDegrees += degreesPerSecond * internal.getDeltaTime();
        this.rotationAngleDegrees %= EngineSetting.DEGREES_PER_FULL_ROTATION;

        if (this.rotationAngleDegrees < 0)
            this.rotationAngleDegrees += EngineSetting.DEGREES_PER_FULL_ROTATION;
    }

    /*
     * Real-world-scale linear drift speed, in chunks per second. Every
     * weather system reads this exact value — the noise field's own
     * rotational phase above, and every streamed WeatherPatternStruct's
     * physical position via WeatherPatternManager.advanceWorldDrift() — so
     * nothing can independently drift out of sync.
     */
    float getWorldDriftChunksPerSecondX() {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        float metersPerSecond = EngineSetting.WEATHER_BASE_DRIFT_SPEED_KPH * EngineSetting.KPH_TO_METERS_PER_SECOND;
        float blocksPerSecond = metersPerSecond / EngineSetting.BLOCK_SIZE;
        float chunksPerSecond = blocksPerSecond / EngineSetting.CHUNK_SIZE;

        return chunksPerSecond * activeWorld.getRotationSpeed();
    }

    private int worldWidthChunks() {
        return worldManager.getActiveWorld().getWorldScale().x / EngineSetting.CHUNK_SIZE;
    }

    // Tilt \\

    private void advanceTilt() {

        float axialTiltDegrees = worldManager.getActiveWorld().getAxialTilt();
        double yearProgress = clockManager.getClockHandle().getYearProgress();

        double tiltFraction = axialTiltDegrees / 90.0;
        this.latitudePhase = Math.sin(yearProgress * 2.0 * Math.PI) * tiltFraction * tiltInfluence * Math.PI;
    }

    // Meander \\

    private void advanceMeander() {
        this.meanderElapsed += internal.getDeltaTime();
    }

    // Sampling \\

    float sampleGlobalIntensity(long chunkCoordinate) {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        double worldWidthChunks = activeWorld.getWorldScale().x / (double) EngineSetting.CHUNK_SIZE;
        double worldHeightChunks = activeWorld.getWorldScale().y / (double) EngineSetting.CHUNK_SIZE;

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        double rotationPhase = (rotationAngleDegrees / EngineSetting.DEGREES_PER_FULL_ROTATION) * (Math.PI * 2.0);
        double meanderPhase = Math.sin(meanderElapsed * MEANDER_SPEED) * MEANDER_AMPLITUDE;
        double zPhase = latitudePhase + meanderPhase;

        return ToroidalNoiseUtility.sample(
                NOISE_SEED,
                chunkX, chunkZ,
                worldWidthChunks, worldHeightChunks,
                WAVELENGTH_CHUNKS,
                rotationPhase, zPhase);
    }

    float getGlobalInfluence() {
        return globalInfluence;
    }

    double getRotationAngleDegrees() {
        return rotationAngleDegrees;
    }
}