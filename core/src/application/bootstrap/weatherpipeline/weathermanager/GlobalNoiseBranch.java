package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;

/*
 * Owns the planet's continuous rotation angle, its seasonal axial-tilt
 * latitude drift, and a slow bounded meander wobble, then folds all three
 * into one continuous, seamlessly torus-wrapped noise overlay — see
 * ToroidalNoiseUtility. Rotation also drives the shared eastward drift rate
 * every other weather system reads via getWorldDriftChunksPerSecondX(), so
 * nothing can fall out of lockstep with this field's own scroll.
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

        WorldHandle activeWorld = worldManager.getActiveWorld();
        float rotationSpeed = activeWorld.getRotationSpeed();

        this.rotationAngleDegrees += rotationSpeed * internal.getDeltaTime();
        this.rotationAngleDegrees %= EngineSetting.DEGREES_PER_FULL_ROTATION;

        if (this.rotationAngleDegrees < 0)
            this.rotationAngleDegrees += EngineSetting.DEGREES_PER_FULL_ROTATION;
    }

    float getWorldDriftChunksPerSecondX() {

        WorldHandle activeWorld = worldManager.getActiveWorld();
        double worldWidthChunks = activeWorld.getWorldScale().x / (double) EngineSetting.CHUNK_SIZE;
        float rotationSpeed = activeWorld.getRotationSpeed();

        return (float) ((rotationSpeed / EngineSetting.DEGREES_PER_FULL_ROTATION) * worldWidthChunks);
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