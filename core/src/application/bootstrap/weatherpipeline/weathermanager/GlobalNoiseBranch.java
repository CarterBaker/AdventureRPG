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
     * never a calendar one.
     *
     * Sampling works entirely in normalized, wrapped world-UV space rather
     * than raw chunk coordinates:
     *
     * 1. A chunk coordinate is divided by the world's own chunk-space width
     * and height (converted from WorldHandle's block-space scale) to get a
     * UV pair, then wrapped into [0, 1) — this is what makes the world a
     * torus for weather purposes, matching the fact that it wraps, and it
     * keeps every value that ever reaches a trig or noise function bounded
     * to a tiny, precision-safe range no matter how large the world is.
     * Previously this method rotated raw chunk coordinates (which can run
     * into the millions on a large world) directly through sin/cos in
     * float — float only carries ~7 significant digits, so at that
     * magnitude the fractional part the noise function needs was already
     * gone before rotation even started. It also mixed block-space
     * (WorldHandle.getWorldScale()) with chunk-space (the coordinate here)
     * without converting between them, which quietly placed the rotation
     * pivot CHUNK_SIZE times farther from the real map center than
     * intended. Both are fixed by this UV approach.
     *
     * 2. Instead of rotating the sample point around the map's center (which
     * barely moves a point near the center while sweeping a point near the
     * edge through many cells per rotation — a real asymmetry the old
     * design had), rotation drives a uniform east-west scroll of the UV
     * field instead. Every point on the map drifts at the same apparent
     * rate regardless of where it sits, which also mirrors how real
     * planetary rotation drives broadly zonal (east-west) atmospheric
     * banding rather than swirling everything around one fixed pole.
     *
     * 3. The nominal cell size (GLOBAL_WEATHER_NOISE_CELL_SIZE) is rounded
     * to the nearest whole number of cells across the world's width and
     * height, and the noise hash lookup wraps cell indices with floorMod —
     * both necessary so the field tiles with zero seam at the wrap
     * boundary, rather than sampling two unrelated hash values on either
     * side of the seam.
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
     * value. See the class comment for why this works in wrapped UV space
     * rather than raw coordinates.
     */
    float sampleGlobalIntensity(long chunkCoordinate) {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        double worldWidthChunks = activeWorld.getWorldScale().x / (double) EngineSetting.CHUNK_SIZE;
        double worldHeightChunks = activeWorld.getWorldScale().y / (double) EngineSetting.CHUNK_SIZE;

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkY = Coordinate2Long.unpackY(chunkCoordinate);

        double u = wrap01(chunkX / worldWidthChunks);
        double v = wrap01(chunkY / worldHeightChunks);

        int cellsX = (int) Math.max(1L, Math.round(worldWidthChunks / noiseCellSize));
        int cellsY = (int) Math.max(1L, Math.round(worldHeightChunks / noiseCellSize));

        double rotationProgress = rotationAngleDegrees / EngineSetting.DEGREES_PER_FULL_ROTATION;

        double sampleX = wrap01(u + rotationProgress) * cellsX;
        double sampleY = v * cellsY;

        return sampleNoise(sampleX, sampleY, cellsX, cellsY);
    }

    float getGlobalInfluence() {
        return globalInfluence;
    }

    double getRotationAngleDegrees() {
        return rotationAngleDegrees;
    }

    // Wrap \\

    private double wrap01(double value) {
        double wrapped = value % 1.0;
        return wrapped < 0 ? wrapped + 1.0 : wrapped;
    }

    // Noise \\

    /*
     * Coherent 2D value noise over a bounded [0, cellsX) x [0, cellsY) cell
     * grid, wrapping seamlessly at the edges via floorMod on the integer
     * cell indices. Identical hash/lerp/smoothstep shape to
     * RegionSampleBranch's local noise, with the hash term order swapped so
     * the two fields never correlate at the same coordinates.
     */
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