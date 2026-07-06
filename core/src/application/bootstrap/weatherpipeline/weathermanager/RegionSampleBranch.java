package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.weatherpipeline.weather.CloudChanceStruct;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.windmanager.WindManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;
import engine.util.random.WeightedChanceUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RegionSampleBranch extends BranchPackage {

    /*
     * Continuously samples a coherent noise field over world-space chunk
     * coordinates at the reference coordinate and its four cardinal
     * neighbors. Each direction's local noise is blended with
     * GlobalNoiseBranch's planet-rotation-and-tilt-driven noise (see
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
     * (an overhead cell) is expected to read WeatherBandStruct.getPrimary()
     * once and hold onto it itself, re-resolving only when it chooses to
     * transition — this class's own sampleDirection() deliberately does
     * the opposite, reblending every call, since the 5 region samples
     * exist purely to drive smoothly fading fog/cloud UBO values, not a
     * persistent identity.
     *
     * Noise position maps to a cumulative chance band per weather (see
     * bandFromPool), so a weather with a larger relative chance occupies a
     * proportionally wider band of the combined noise field and appears
     * more often — never a single evenly-spaced slot. Blending happens
     * across the boundary between adjacent bands so weather never pops as
     * the noise field drifts — it fades.
     *
     * Wind-driven drift — the actual "storms move with the wind" mechanic
     * -------------------------------------------------------------------
     * The local noise field's sampling position is displaced every frame by
     * the SAME live WindHandle.getLocalWindDirection()/getLocalWindSpeed()
     * every other wind-aware system reads (see LocalWindBranch) — not by a
     * fixed elapsedTime * constant scroll the way this used to work. Wind
     * x/z map onto chunk-space x/y, the same horizontal-plane convention
     * OverheadManager already uses for its own cosmetic cloud-sprite drift,
     * so the underlying storm PATTERN drifts in visual lockstep with the
     * decorative clouds sitting on top of it rather than the two silently
     * disagreeing.
     *
     * advanceWindDrift() integrates velocity into position incrementally,
     * once per frame (driftChunksX/Y += direction * speed * scale * dt),
     * rather than recomputing drift from elapsedTime * currentWind every
     * call. The latter would retroactively apply this instant's wind to
     * the entire session's accumulated history the moment wind direction
     * or speed changes — a visible pop the moment a gust or season turn
     * changes the wind. Both accumulators are wrapped modulo the active
     * world's own chunk-space width/height every frame, exactly like
     * GlobalNoiseBranch wraps its own rotation angle, so they never grow
     * large enough to lose float precision over a long session — and,
     * because LocalWindBranch itself reads WeatherManager.getWindSpeedScale()
     * (this class's own center sample), wind and weather form one closed
     * feedback loop: wind pushes the storm pattern, the storm's own
     * windSpeedScale in turn shapes the wind blowing through it.
     *
     * Local noise samples in normalized, wrapped world-UV space rather than
     * raw chunk coordinates — chunk coordinates (including the drifted
     * offset) are converted to a UV fraction of the world's own chunk-space
     * width/height in double precision, wrapped into [0, 1), scaled to a
     * whole number of noise cells, and the hash lookup wraps cell indices
     * with floorMod. This keeps the field seamless at the world edge
     * instead of sampling two unrelated hash values on either side of the
     * seam.
     */

    // Internal
    private GlobalNoiseBranch globalNoiseBranch;
    private WorldManager worldManager;
    private WindManager windManager;

    // Settings
    private int sampleDistance;
    private float noiseCellSize;
    private float windDriftScale;

    // Reference
    private long referenceCoordinate;

    // Drift — accumulated chunk-space displacement of the local weather
    // noise field, driven every frame by the live local wind vector.
    private double driftChunksX;
    private double driftChunksY;

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
        this.windDriftScale = EngineSetting.WEATHER_WIND_DRIFT_SCALE;

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
        this.windManager = get(WindManager.class);
    }

    // Reference \\

    void setReferenceCoordinate(long chunkCoordinate) {
        this.referenceCoordinate = chunkCoordinate;
    }

    long getReferenceCoordinate() {
        return referenceCoordinate;
    }

    // Sampling \\

    void sampleRegions(ObjectArrayList<WeatherPoolEntryStruct> pool) {

        advanceWindDrift();

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

    // Wind Drift \\

    /*
     * Advances the local weather noise field's drift from the live local
     * wind vector — see the class comment for the full rationale. Called
     * once per frame, before any of the 5 direction samples.
     */
    private void advanceWindDrift() {

        Vector3 windDirection = windManager.getWindHandle().getLocalWindDirection();
        float windSpeed = windManager.getWindHandle().getLocalWindSpeed();
        float deltaTime = internal.getDeltaTime();

        driftChunksX += windDirection.x * windSpeed * windDriftScale * deltaTime;
        driftChunksY += windDirection.z * windSpeed * windDriftScale * deltaTime;

        WorldHandle activeWorld = worldManager.getActiveWorld();
        double worldWidthChunks = activeWorld.getWorldScale().x / (double) EngineSetting.CHUNK_SIZE;
        double worldHeightChunks = activeWorld.getWorldScale().y / (double) EngineSetting.CHUNK_SIZE;

        driftChunksX = wrapPeriod(driftChunksX, worldWidthChunks);
        driftChunksY = wrapPeriod(driftChunksY, worldHeightChunks);
    }

    private double wrapPeriod(double value, double period) {

        if (period <= 0)
            return 0.0;

        double wrapped = value % period;

        return wrapped < 0 ? wrapped + period : wrapped;
    }

    // Resolution \\

    /*
     * Combines this coordinate's wind-drifted local noise with the global
     * rotation-and-tilt-driven noise, then resolves the blend against the
     * supplied chance-weighted pool. Writes into the caller-supplied struct
     * rather than allocating — this is the method
     * WeatherManager.resolveWeatherBand() calls on behalf of any
     * cross-package caller.
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
     * uses.
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
     * the world edge and displaced by the wind-driven drift accumulators
     * from advanceWindDrift() so weather fronts visibly move across the
     * world with the wind, independently of the planet's rotation/tilt.
     */
    private float sampleNoise(int chunkX, int chunkY) {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        double worldWidthChunks = activeWorld.getWorldScale().x / (double) EngineSetting.CHUNK_SIZE;
        double worldHeightChunks = activeWorld.getWorldScale().y / (double) EngineSetting.CHUNK_SIZE;

        int cellsX = (int) Math.max(1L, Math.round(worldWidthChunks / noiseCellSize));
        int cellsY = (int) Math.max(1L, Math.round(worldHeightChunks / noiseCellSize));

        double u = wrap01((chunkX + driftChunksX) / worldWidthChunks);
        double v = wrap01((chunkY + driftChunksY) / worldHeightChunks);

        double sampleX = u * cellsX;
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