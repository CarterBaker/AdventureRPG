// RegionSampleBranch.java
package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.weatherpipeline.weather.CloudChanceStruct;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.windmanager.WindManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Direction2Vector;
import engine.util.mathematics.vectors.Vector3;
import engine.util.random.WeightedChanceUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RegionSampleBranch extends BranchPackage {

    /*
     * Continuously samples a coherent noise field over world-space chunk
     * coordinates at the reference coordinate and its eight surrounding
     * directions (Direction2Vector's own order: N, NE, E, SE, S, SW, W, NW).
     * Each direction's local noise is blended with GlobalNoiseBranch's
     * planet-rotation-and-tilt-driven noise
     * (EngineSetting.GLOBAL_WEATHER_INFLUENCE)
     * before resolving against the supplied chance-weighted pool via
     * resolveBand() — the single canonical noise-to-weather resolution path,
     * also reachable cross-package through WeatherManager.resolveWeatherBand().
     *
     * resolveBand() only reports which two pool entries a coordinate sits
     * between and how far across that blend band it is (WeatherBandStruct).
     * It remembers nothing between calls — a caller wanting a persistent,
     * non-reblending identity (an overhead cell) should read
     * WeatherBandStruct.getPrimary() once and hold it. This class's own
     * sampleDirection() deliberately reblends every call since these 9
     * samples only ever drive smoothly fading fog/cloud UBO values.
     *
     * Diagonal samples are placed at the same Euclidean distance from the
     * reference as the cardinal ones — a diagonal direction has both axes
     * at full magnitude (dir.x, dir.y both ±1), which without correction
     * would put it sqrt(2) times farther out. DIAGONAL_AXIS_SCALE (1/sqrt(2))
     * corrects that per-axis before the offset is applied.
     *
     * Wind-driven drift — the actual "storms move with the wind" mechanic
     * -------------------------------------------------------------------
     * The local noise field's sampling position is displaced every frame by
     * the SAME live WindHandle local wind vector every other wind-aware
     * system reads (see LocalWindBranch) — not a fixed elapsedTime*constant
     * scroll. advanceWindDrift() integrates velocity into position
     * incrementally (drift += direction * speed * scale * dt) rather than
     * recomputing from elapsedTime * currentWind, which would retroactively
     * apply this instant's wind to the whole session's history the moment
     * wind changes — a visible pop. Both drift accumulators wrap modulo the
     * world's own chunk-space width/height every frame, exactly like
     * GlobalNoiseBranch wraps its rotation angle. Because LocalWindBranch
     * itself reads WeatherManager.getWindSpeedScale()/getWindTurbulenceScale()
     * (this class's own center sample), wind and weather form one closed
     * feedback loop: wind pushes the storm pattern, the storm's own
     * windSpeedScale/windTurbulenceScale in turn shape the wind blowing
     * through it — see LocalWindBranch's own doc comment for that side.
     *
     * Local noise samples in normalized, wrapped world-UV space — chunk
     * coordinates (plus the drifted offset) convert to a UV fraction of the
     * world's own chunk-space width/height in double precision, wrapped
     * into [0, 1), scaled to a whole number of noise cells, with the hash
     * lookup wrapping cell indices via floorMod — seamless at the world
     * edge instead of two unrelated hash values on either side of the seam.
     */

    // Scales a diagonal direction's per-axis offset so its Euclidean
    // distance from the reference matches the four cardinal samples exactly.
    private static final float DIAGONAL_AXIS_SCALE = 0.70710678f;

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

    // Samples — index 0 is the centre; indices 1-8 mirror Direction2Vector's
    // own ordinal order (NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH,
    // SOUTHWEST, WEST, NORTHWEST), so a direction's ordinal and its sample
    // index are always exactly one apart, with no separate lookup table.
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

        // Samples — centre + all 8 Direction2Vector entries
        this.samples = new WeatherSampleStruct[1 + Direction2Vector.LENGTH];
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

        for (int i = 0; i < Direction2Vector.LENGTH; i++) {

            Direction2Vector dir = Direction2Vector.VALUES[i];
            boolean isDiagonal = dir.x != 0 && dir.y != 0;
            float axisScale = isDiagonal ? DIAGONAL_AXIS_SCALE : 1.0f;

            int sampleX = originX + Math.round(dir.x * sampleDistance * axisScale);
            int sampleY = originY + Math.round(dir.y * sampleDistance * axisScale);

            sampleDirection(samples[i + 1], sampleX, sampleY, pool);
        }
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
     * once per frame, before any of the 9 direction samples.
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
     * uses. windSpeedScale/windTurbulenceScale are blended identically to
     * every other atmosphere field — see LocalWindBranch for how the
     * centre sample's copy of these two feeds back into wind itself.
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
        sample.setWindTurbulenceScale(lerp(low.getWindTurbulenceScale(), high.getWindTurbulenceScale(), t));
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

    WeatherSampleStruct getSampleForDirection(Direction2Vector direction) {
        return samples[direction.index + 1];
    }

    WeatherSampleStruct getNorthSample() {
        return samples[Direction2Vector.NORTH.index + 1];
    }

    WeatherSampleStruct getNortheastSample() {
        return samples[Direction2Vector.NORTHEAST.index + 1];
    }

    WeatherSampleStruct getEastSample() {
        return samples[Direction2Vector.EAST.index + 1];
    }

    WeatherSampleStruct getSoutheastSample() {
        return samples[Direction2Vector.SOUTHEAST.index + 1];
    }

    WeatherSampleStruct getSouthSample() {
        return samples[Direction2Vector.SOUTH.index + 1];
    }

    WeatherSampleStruct getSouthwestSample() {
        return samples[Direction2Vector.SOUTHWEST.index + 1];
    }

    WeatherSampleStruct getWestSample() {
        return samples[Direction2Vector.WEST.index + 1];
    }

    WeatherSampleStruct getNorthwestSample() {
        return samples[Direction2Vector.NORTHWEST.index + 1];
    }
}