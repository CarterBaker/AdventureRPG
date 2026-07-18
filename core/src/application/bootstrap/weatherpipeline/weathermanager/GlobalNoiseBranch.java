// GlobalNoiseBranch.java
package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;

/*
 * Owns the planet's rotation angle, seasonal tilt drift, and meander wobble,
 * folding all three into the 2D weather noise field. Rotation and meander
 * both advance from the same KPH-derived angular speed, scaled by the
 * meander's own wave number, so the two are always the same physical
 * current rather than two independently-tuned rates. RegionSampleBranch
 * samples its own local noise with the same wave number and phase so the
 * two layers meander coherently together.
 */
class GlobalNoiseBranch extends BranchPackage {

    private static final long NOISE_SEED = 0x9E3779B97F4A7C15L;
    private static final double WAVELENGTH_CHUNKS = 768.0;

    private WorldManager worldManager;
    private ClockManager clockManager;

    private float globalInfluence;
    private float tiltInfluence;

    private double rotationAngleDegrees;
    private double latitudeDriftChunks;
    private double meanderPhase;

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

    // Angular Speed \\

    private double angularSpeedRadiansPerSecond(int worldWidthChunks) {
        return (getWorldDriftChunksPerSecondX() / worldWidthChunks) * (Math.PI * 2.0);
    }

    // Rotation \\

    private void advanceRotation() {

        int worldWidthChunks = Math.max(1, worldWidthChunks());
        double degreesPerSecond = Math.toDegrees(angularSpeedRadiansPerSecond(worldWidthChunks));

        this.rotationAngleDegrees += degreesPerSecond * internal.getDeltaTime();
        this.rotationAngleDegrees %= EngineSetting.DEGREES_PER_FULL_ROTATION;

        if (this.rotationAngleDegrees < 0)
            this.rotationAngleDegrees += EngineSetting.DEGREES_PER_FULL_ROTATION;
    }

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
        this.latitudeDriftChunks = Math.sin(yearProgress * 2.0 * Math.PI) * tiltFraction * tiltInfluence
                * WAVELENGTH_CHUNKS;
    }

    // Meander \\

    private void advanceMeander() {

        int worldWidthChunks = Math.max(1, worldWidthChunks());
        double radiansPerSecond = angularSpeedRadiansPerSecond(worldWidthChunks)
                * EngineSetting.GLOBAL_WEATHER_MEANDER_WAVE_NUMBER;

        this.meanderPhase += radiansPerSecond * internal.getDeltaTime();
        this.meanderPhase %= (Math.PI * 2.0);

        if (this.meanderPhase < 0)
            this.meanderPhase += Math.PI * 2.0;
    }

    double getMeanderWaveNumber() {
        return EngineSetting.GLOBAL_WEATHER_MEANDER_WAVE_NUMBER;
    }

    double getMeanderPhase() {
        return meanderPhase;
    }

    // Sampling \\

    float sampleGlobalIntensity(long chunkCoordinate) {

        WorldHandle activeWorld = worldManager.getActiveWorld();
        double worldWidthChunks = activeWorld.getWorldScale().x / (double) EngineSetting.CHUNK_SIZE;

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        double rotationPhase = (rotationAngleDegrees / EngineSetting.DEGREES_PER_FULL_ROTATION) * (Math.PI * 2.0);
        double meanderAmplitudeChunks = EngineSetting.GLOBAL_WEATHER_MEANDER_INFLUENCE * WAVELENGTH_CHUNKS;

        return WeatherNoiseUtility.sample(
                NOISE_SEED,
                chunkX, chunkZ,
                worldWidthChunks,
                WAVELENGTH_CHUNKS,
                rotationPhase, latitudeDriftChunks,
                getMeanderWaveNumber(), meanderAmplitudeChunks, getMeanderPhase());
    }

    float getGlobalInfluence() {
        return globalInfluence;
    }

    double getRotationAngleDegrees() {
        return rotationAngleDegrees;
    }
}