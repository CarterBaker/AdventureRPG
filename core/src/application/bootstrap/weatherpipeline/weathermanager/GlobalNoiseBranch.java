package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;

class GlobalNoiseBranch extends BranchPackage {

    /*
     * Owns the planet's continuous rotation angle and the CPU-side global
     * weather noise overlay driven by it. Rotation advances independently of
     * the in-game calendar — a flat rotationSpeed (degrees per real second)
     * times deltaTime — so this is a pure atmospheric-simulation concept,
     * never a calendar one. A fixed world-space coordinate's sampled
     * intensity drifts over time purely because the sample point is rotated
     * into the noise field's frame before sampling — the field itself never
     * moves, the planet turns underneath it, the same way real global
     * circulation bands sweep past a stationary point on the ground.
     *
     * Queried directly by RegionSampleBranch, and exposed further by
     * WeatherManager for any other system (wind, horizon, overhead) that
     * wants it — always as a plain CPU value. Never pushed to a UBO on its
     * own; only the weather values it influences travel to the GPU, through
     * the existing WeatherData / WeatherRegionData UBOs.
     */

    // Internal
    private WorldManager worldManager;

    // Settings
    private float noiseCellSize;
    private float globalInfluence;

    // Rotation
    private double rotationAngleDegrees;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.noiseCellSize = EngineSetting.GLOBAL_WEATHER_NOISE_CELL_SIZE;
        this.globalInfluence = EngineSetting.GLOBAL_WEATHER_INFLUENCE;
    }

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
    }

    @Override
    protected void update() {
        advanceRotation();
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

    // Sampling \\

    /*
     * Samples the rotating global noise field at a world-space chunk
     * coordinate, returning a coherent [0, 1] "global storm intensity"
     * value. The coordinate is rotated around the active world's center by
     * the current rotation angle before sampling, so the same world
     * position sweeps through different noise values as the planet turns.
     */
    float sampleGlobalIntensity(long chunkCoordinate) {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        int worldCenterX = activeWorld.getWorldScale().x / 2;
        int worldCenterY = activeWorld.getWorldScale().y / 2;

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkY = Coordinate2Long.unpackY(chunkCoordinate);

        float localX = chunkX - worldCenterX;
        float localY = chunkY - worldCenterY;

        double angleRadians = Math.toRadians(rotationAngleDegrees);
        float cos = (float) Math.cos(angleRadians);
        float sin = (float) Math.sin(angleRadians);

        float rotatedX = localX * cos - localY * sin;
        float rotatedY = localX * sin + localY * cos;

        return sampleNoise(rotatedX / noiseCellSize, rotatedY / noiseCellSize);
    }

    float getGlobalInfluence() {
        return globalInfluence;
    }

    double getRotationAngleDegrees() {
        return rotationAngleDegrees;
    }

    // Noise \\

    /*
     * Coherent 2D value noise, identical in structure to RegionSampleBranch's
     * local noise but with the hash term order swapped so the two fields
     * never correlate at the same coordinates.
     */
    private float sampleNoise(float sampleX, float sampleY) {

        int x0 = (int) Math.floor(sampleX);
        int y0 = (int) Math.floor(sampleY);
        int x1 = x0 + 1;
        int y1 = y0 + 1;

        float tx = sampleX - x0;
        float ty = sampleY - y0;

        float n00 = hash(x0, y0);
        float n10 = hash(x1, y0);
        float n01 = hash(x0, y1);
        float n11 = hash(x1, y1);

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