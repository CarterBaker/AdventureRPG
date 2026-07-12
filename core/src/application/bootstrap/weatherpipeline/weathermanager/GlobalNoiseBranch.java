package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;

class GlobalNoiseBranch extends BranchPackage {

    /*
     * Owns the planet's continuous rotation angle, its seasonal axial-tilt
     * latitude drift, a slower meander wobble layered on top of both, and
     * the CPU-side global weather noise overlay driven by all three.
     * Sampling works in normalized, toroidally wrapped world-UV space, and
     * rotation drives a uniform east-west scroll of that field. That same
     * rotation-driven scroll rate is exposed as a chunk-space drift rate so
     * every other weather system moves in lockstep with this one, rather
     * than each maintaining its own independently-tuned drift constant.
     */

    private WorldManager worldManager;
    private ClockManager clockManager;

    private float noiseCellSize;
    private float globalInfluence;
    private float tiltInfluence;
    private int meanderWaveNumber;
    private float meanderInfluence;
    private float meanderPhaseSpeed;

    private double rotationAngleDegrees;
    private double latitudeShift;
    private double meanderPhase;

    @Override
    protected void create() {
        this.noiseCellSize = EngineSetting.GLOBAL_WEATHER_NOISE_CELL_SIZE;
        this.globalInfluence = EngineSetting.GLOBAL_WEATHER_INFLUENCE;
        this.tiltInfluence = EngineSetting.GLOBAL_WEATHER_TILT_INFLUENCE;
        this.meanderWaveNumber = EngineSetting.GLOBAL_WEATHER_MEANDER_WAVE_NUMBER;
        this.meanderInfluence = EngineSetting.GLOBAL_WEATHER_MEANDER_INFLUENCE;
        this.meanderPhaseSpeed = EngineSetting.GLOBAL_WEATHER_MEANDER_PHASE_SPEED;
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

    /*
     * Chunk-space drift rate along the world's longitudinal (X) axis,
     * matching exactly how fast rotationAngleDegrees scrolls the U
     * coordinate every query. Any system whose own position should migrate
     * with the planet's spin reads this instead of maintaining its own
     * drift constant.
     */
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
        double tiltPhase = yearProgress * 2.0 * Math.PI;

        this.latitudeShift = Math.sin(tiltPhase) * tiltFraction * tiltInfluence;
    }

    // Meander \\

    private void advanceMeander() {
        this.meanderPhase += meanderPhaseSpeed * internal.getDeltaTime();
        this.meanderPhase %= (Math.PI * 2.0);
    }

    // Sampling \\

    float sampleGlobalIntensity(long chunkCoordinate) {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        double worldWidthChunks = activeWorld.getWorldScale().x / (double) EngineSetting.CHUNK_SIZE;
        double worldHeightChunks = activeWorld.getWorldScale().y / (double) EngineSetting.CHUNK_SIZE;

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkY = Coordinate2Long.unpackY(chunkCoordinate);

        double u = wrap01(chunkX / worldWidthChunks);
        double rotationProgress = rotationAngleDegrees / EngineSetting.DEGREES_PER_FULL_ROTATION;
        double rotatedU = wrap01(u + rotationProgress);

        double meanderShift = Math.sin(rotatedU * meanderWaveNumber * Math.PI * 2.0 + meanderPhase)
                * meanderInfluence;

        double v = wrap01(chunkY / worldHeightChunks + latitudeShift + meanderShift);

        int cellsX = (int) Math.max(1L, Math.round(worldWidthChunks / noiseCellSize));
        int cellsY = (int) Math.max(1L, Math.round(worldHeightChunks / noiseCellSize));

        double sampleX = rotatedU * cellsX;
        double sampleY = v * cellsY;

        return sampleNoise(sampleX, sampleY, cellsX, cellsY);
    }

    float getGlobalInfluence() {
        return globalInfluence;
    }

    double getRotationAngleDegrees() {
        return rotationAngleDegrees;
    }

    double getLatitudeShift() {
        return latitudeShift;
    }

    double getMeanderPhase() {
        return meanderPhase;
    }

    // Wrap \\

    private double wrap01(double value) {
        double wrapped = value % 1.0;
        return wrapped < 0 ? wrapped + 1.0 : wrapped;
    }

    // Noise \\

    private float sampleNoise(double sampleX, double sampleY, int cellsX, int cellsY) {

        int x0 = (int) Math.floor(sampleX);
        int y0 = (int) Math.floor(sampleY);

        float tx = (float) (sampleX - x0);
        float ty = (float) (sampleY - y0);

        int wrappedX0 = Math.floorMod(x0, cellsX);
        int wrappedY0 = Math.floorMod(y0, cellsY);
        int wrappedX1 = Math.floorMod(x0 + 1, cellsX);
        int wrappedY1 = Math.floorMod(y0 + 1, cellsY);

        float n00 = hash(wrappedX0, wrappedY0);
        float n10 = hash(wrappedX1, wrappedY0);
        float n01 = hash(wrappedX0, wrappedY1);
        float n11 = hash(wrappedX1, wrappedY1);

        float smoothTx = smoothstep(tx);
        float smoothTy = smoothstep(ty);

        float nx0 = lerp(n00, n10, smoothTx);
        float nx1 = lerp(n01, n11, smoothTx);

        return lerp(nx0, nx1, smoothTy);
    }

    private float hash(int x, int y) {

        int h = x * 668265263 + y * 374761393;
        h = (h ^ (h >>> 13)) * 1274126177;

        return ((h ^ (h >>> 16)) & 0x7fffffff) / (float) Integer.MAX_VALUE;
    }

    private float smoothstep(float t) {
        return t * t * (3f - 2f * t);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}