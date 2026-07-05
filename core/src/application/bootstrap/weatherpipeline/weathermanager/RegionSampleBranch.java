package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.weatherpipeline.weather.CloudChanceStruct;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.random.WeightedChanceUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RegionSampleBranch extends BranchPackage {

    /*
     * Continuously samples a coherent, drifting noise field over world-space
     * chunk coordinates at the reference coordinate and its four cardinal
     * neighbors. Each direction's local drift noise is blended with
     * GlobalNoiseBranch's planet-rotation-driven noise (see
     * EngineSetting.GLOBAL_WEATHER_INFLUENCE for the blend weight) before
     * resolving against the chance-weighted pool handed to it, via
     * resolveBand() — the single canonical noise-to-weather resolution
     * path, also reachable cross-package through
     * WeatherManager.resolveWeatherBand() so a future overhead cloud grid
     * resolves weather with the exact same algorithm this class already
     * uses for its own 5-point region sampling, rather than a second,
     * possibly-drifting copy of the same logic.
     *
     * resolveBand() only ever reports which two pool entries a coordinate's
     * noise currently sits between and how far across that blend band it
     * is — see WeatherBandStruct. It does not remember anything between
     * calls. A caller that wants a stable, non-reblending weather identity
     * (an overhead cell, eventually) is expected to read
     * WeatherBandStruct.getPrimary() once and hold onto it itself,
     * re-resolving only when it chooses to transition — this class's own
     * sampleDirection() deliberately does the opposite, reblending every
     * call, since the 5 region samples exist purely to drive smoothly
     * fading fog/cloud UBO values, not a persistent identity.
     *
     * Noise position maps to a cumulative chance band per weather (see
     * bandFromPool), so a weather with a larger relative chance occupies a
     * proportionally wider band of the combined noise field and appears
     * more often — never a single evenly-spaced slot. Blending happens
     * across the boundary between adjacent bands so weather never pops as
     * the noise field drifts — it fades.
     *
     * Local noise samples in normalized, wrapped world-UV space rather than
     * raw chunk coordinates — chunk coordinates are converted to a UV
     * fraction of the world's own chunk-space width/height in double
     * precision, wrapped into [0, 1), scaled to a whole number of noise
     * cells, and the hash lookup wraps cell indices with floorMod. This
     * keeps the field seamless at the world edge instead of sampling two
     * unrelated hash values on either side of the seam. elapsedTime is
     * wrapped modulo a large but bounded period for the same reason
     * GlobalNoiseBranch wraps its own rotation angle.
     */

    // Internal
    private GlobalNoiseBranch globalNoiseBranch;
    private WorldManager worldManager;

    // Settings
    private int sampleDistance;
    private float noiseCellSize;
    private float windDriftSpeed;

    // Reference
    private long referenceCoordinate;

    // Drift
    private float elapsedTime;

    // Samples — CENTER, NORTH, EAST, SOUTH, WEST
    private WeatherSampleStruct[] samples;

    // Scratch — reused every sample call, never reallocated
    private final WeatherBandStruct bandScratch = new WeatherBandStruct();

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.sampleDistance = EngineSetting.WEATHER_REGION_SAMPLE_DISTANCE;
        this.noiseCellSize = EngineSetting.WEATHER_NOISE_CELL_SIZE;
        this.windDriftSpeed = EngineSetting.WEATHER_WIND_DRIFT_SPEED;

        // Reference
        this.referenceCoordinate = Coordinate2Long.pack(0, 0);

        // Samples
        this.samples = new WeatherSampleStruct[5];
        for (int i = 0; i < samples.length; i++)
            samples[i] = new WeatherSampleStruct();
    }

    @Override
    protected void get() {
        this.globalNoiseBranch = get(GlobalNoiseBranch.class);
        this.worldManager = get(WorldManager.class);
    }

    // Reference \\

    void setReferenceCoordinate(long chunkCoordinate) {
        this.referenceCoordinate = chunkCoordinate;
    }

    // Sampling \\

    void sampleRegions(ObjectArrayList<WeatherPoolEntryStruct> pool) {

        elapsedTime += internal.getDeltaTime();
        elapsedTime %= EngineSetting.WEATHER_LOCAL_DRIFT_TIME_WRAP;

        int originX = Coordinate2Long.unpackX(referenceCoordinate);
        int originY = Coordinate2Long.unpackY(referenceCoordinate);

        sampleDirection(samples[0], originX, originY, pool);
        sampleDirection(samples[1], originX, originY - sampleDistance, pool);
        sampleDirection(samples[2], originX + sampleDistance, originY, pool);
        sampleDirection(samples[3], originX, originY + sampleDistance, pool);
        sampleDirection(samples[4], originX - sampleDistance, originY, pool);
    }

    private void sampleDirection(
            WeatherSampleStruct sample,
            int chunkX,
            int chunkY,
            ObjectArrayList<WeatherPoolEntryStruct> pool) {

        resolveBand(bandScratch, chunkX, chunkY, pool);
        writeSample(sample, bandScratch.getLow(), bandScratch.getHigh(), bandScratch.getBlendFactor());
    }

    // Resolution \\

    /*
     * Combines this coordinate's local drift noise with the global
     * rotation-driven noise, then resolves the blend against the supplied
     * chance-weighted pool. Writes into the caller-supplied struct rather
     * than allocating — this is the method WeatherManager.resolveWeatherBand()
     * calls on behalf of any cross-package caller.
     */
    void resolveBand(WeatherBandStruct out, int chunkX, int chunkY, ObjectArrayList<WeatherPoolEntryStruct> pool) {

        float localNoise = sampleNoise(chunkX, chunkY);
        float globalIntensity = globalNoiseBranch.sampleGlobalIntensity(Coordinate2Long.pack(chunkX, chunkY));
        float combinedNoise = lerp(localNoise, globalIntensity, globalNoiseBranch.getGlobalInfluence());

        bandFromPool(out, pool, combinedNoise);
    }

    /*
     * Maps noise01 onto a cumulative chance-weighted band across the pool,
     * in JSON declaration order.
     */
    private void bandFromPool(WeatherBandStruct out, ObjectArrayList<WeatherPoolEntryStruct> pool, float noise) {

        if (pool.size() == 1) {
            WeatherHandle only = pool.get(0).getWeatherHandle();
            out.set(only, only, 0f);
            return;
        }

        float total = WeightedChanceUtility.totalChance(pool);

        if (total <= 0f) {
            WeatherHandle first = pool.get(0).getWeatherHandle();
            out.set(first, first, 0f);
            return;
        }

        float target = clamp01(noise) * total;
        float cumulative = 0f;

        for (int i = 0; i < pool.size(); i++) {

            float chance = Math.max(0f, pool.get(i).getChance());
            float bandEnd = cumulative + chance;
            boolean isLast = i == pool.size() - 1;

            if (target <= bandEnd || isLast) {

                WeatherHandle low = pool.get(i).getWeatherHandle();
                int nextIndex = isLast ? i : i + 1;
                WeatherHandle high = pool.get(nextIndex).getWeatherHandle();

                float bandWidth = Math.max(bandEnd - cumulative, 0.0001f);
                float t = clamp01((target - cumulative) / bandWidth);

                out.set(low, high, t);
                return;
            }

            cumulative = bandEnd;
        }
    }

    // Visual Blend \\

    /*
     * Converts a resolved band into flattened, continuously-blended visual
     * values for the region-sampling UBO path — a genuine reblend every
     * call, not the identity-preserving path a persistent overhead cell
     * will use.
     */
    private void writeSample(
            WeatherSampleStruct sample,
            WeatherHandle low,
            WeatherHandle high,
            float t) {

        CloudChanceStruct lowCloud = low.getPrimaryCloud();
        CloudChanceStruct highCloud = high.getPrimaryCloud();

        sample.setCloudCoverage(lerp(low.getCloudCoverage(), high.getCloudCoverage(), t));
        sample.setPrecipitationIntensity(lerp(low.getPrecipitationIntensity(), high.getPrecipitationIntensity(), t));
        sample.setWindSpeedScale(lerp(low.getWindSpeedScale(), high.getWindSpeedScale(), t));
        sample.setFogDensityScale(lerp(low.getFogDensityScale(), high.getFogDensityScale(), t));
        sample.setCloudAltitude(lerp(lowCloud.getEffectiveAltitude(), highCloud.getEffectiveAltitude(), t));

        sample.setCloudColor(
                lerp(lowCloud.getCloudHandle().getCloudColor().x, highCloud.getCloudHandle().getCloudColor().x, t),
                lerp(lowCloud.getCloudHandle().getCloudColor().y, highCloud.getCloudHandle().getCloudColor().y, t),
                lerp(lowCloud.getCloudHandle().getCloudColor().z, highCloud.getCloudHandle().getCloudColor().z, t));
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    // Noise \\

    /*
     * Coherent 2D value noise over chunk coordinates, wrapped seamlessly at
     * the world edge and drifted over real time so weather fronts visibly
     * move across the world independently of the planet's rotation.
     */
    private float sampleNoise(int chunkX, int chunkY) {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        double worldWidthChunks = activeWorld.getWorldScale().x / (double) EngineSetting.CHUNK_SIZE;
        double worldHeightChunks = activeWorld.getWorldScale().y / (double) EngineSetting.CHUNK_SIZE;

        int cellsX = (int) Math.max(1L, Math.round(worldWidthChunks / noiseCellSize));
        int cellsY = (int) Math.max(1L, Math.round(worldHeightChunks / noiseCellSize));

        double u = wrap01(chunkX / worldWidthChunks);
        double v = wrap01(chunkY / worldHeightChunks);

        float drift = elapsedTime * windDriftSpeed;

        double sampleX = u * cellsX + drift;
        double sampleY = v * cellsY;

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

    private double wrap01(double value) {
        double wrapped = value % 1.0;
        return wrapped < 0 ? wrapped + 1.0 : wrapped;
    }

    private float hash(int x, int y) {

        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;

        return ((h ^ (h >>> 16)) & 0x7fffffff) / (float) Integer.MAX_VALUE;
    }

    private float smoothstep(float t) {
        return t * t * (3f - 2f * t);
    }

    // Accessible \\

    WeatherSampleStruct getCenterSample() {
        return samples[0];
    }

    WeatherSampleStruct getNorthSample() {
        return samples[1];
    }

    WeatherSampleStruct getEastSample() {
        return samples[2];
    }

    WeatherSampleStruct getSouthSample() {
        return samples[3];
    }

    WeatherSampleStruct getWestSample() {
        return samples[4];
    }
}