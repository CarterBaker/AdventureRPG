package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.weatherpipeline.weather.CloudChanceStruct;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
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
     * resolving against the chance-weighted pool handed to it by
     * WeatherManager: noise position maps to a cumulative chance band per
     * weather (see blendPool), so a weather with a larger relative chance
     * occupies a proportionally wider band of the combined noise field and
     * appears more often — never a single evenly-spaced slot. Blending
     * happens across the boundary between adjacent bands so weather never
     * pops as the noise field drifts — it fades.
     */

    // Internal
    private GlobalNoiseBranch globalNoiseBranch;

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
    }

    // Reference \\

    void setReferenceCoordinate(long chunkCoordinate) {
        this.referenceCoordinate = chunkCoordinate;
    }

    // Sampling \\

    void sampleRegions(ObjectArrayList<WeatherPoolEntryStruct> pool) {

        elapsedTime += internal.getDeltaTime();

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

        float localNoise = sampleNoise(chunkX, chunkY);
        float globalIntensity = globalNoiseBranch.sampleGlobalIntensity(Coordinate2Long.pack(chunkX, chunkY));
        float combinedNoise = lerp(localNoise, globalIntensity, globalNoiseBranch.getGlobalInfluence());

        blendPool(sample, pool, combinedNoise);
    }

    // Blend \\

    /*
     * Maps noise01 onto a cumulative chance-weighted band across the pool,
     * in JSON declaration order. Within the band a sample lands in, it
     * blends toward the NEXT pool entry as noise approaches the top of that
     * band — the same "fade across the seam" behavior the old uniform-slot
     * version had, just with band width driven by relative chance instead
     * of array position.
     */
    private void blendPool(WeatherSampleStruct sample, ObjectArrayList<WeatherPoolEntryStruct> pool, float noise) {

        if (pool.size() == 1) {
            WeatherHandle only = pool.get(0).getWeatherHandle();
            writeSample(sample, only, only, 0f);
            return;
        }

        float total = WeightedChanceUtility.totalChance(pool);

        if (total <= 0f) {
            WeatherHandle first = pool.get(0).getWeatherHandle();
            writeSample(sample, first, first, 0f);
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

                writeSample(sample, low, high, t);
                return;
            }

            cumulative = bandEnd;
        }
    }

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
     * Coherent 2D value noise over chunk coordinates, drifted over real time
     * so weather fronts visibly move across the world independently of the
     * planet's rotation. Mirrors the bilinear hash-noise approach used by
     * Clouds.glsl, kept CPU-side and self-contained here. Not yet wrapped at
     * world edges — that arrives with the horizon sampling step, where this
     * same function grows a second long-distance call site.
     */
    private float sampleNoise(int chunkX, int chunkY) {

        float drift = elapsedTime * windDriftSpeed;

        float sampleX = chunkX / noiseCellSize + drift;
        float sampleY = chunkY / noiseCellSize;

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