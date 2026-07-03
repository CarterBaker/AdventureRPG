package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.wind.WindHandle;
import application.bootstrap.weatherpipeline.windmanager.WindManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;

class RegionSampleBranch extends BranchPackage {

    /*
     * Continuously samples a coherent, drifting noise field over world-space
     * chunk coordinates at the reference coordinate and its four cardinal
     * neighbors. Each sample resolves against the pool handed to it by
     * WeatherManager (already sorted by ascending cloud coverage) and blends
     * between the two nearest entries — weighted by each entry's own
     * 'chance' from its JSON definition, see rebuildWeightedPositions() —
     * so weather never pops as the noise field drifts, and higher-chance
     * weathers show up more often than low-chance ones. Noise drift follows
     * WindManager's local wind direction instead of a fixed axis.
     */

    // Settings
    private int sampleDistance;
    private float noiseCellSize;

    // Internal
    private WindManager windManager;

    // Reference
    private long referenceCoordinate;

    // Drift
    private float elapsedTime;

    // Samples — CENTER, NORTH, EAST, SOUTH, WEST
    private WeatherSampleStruct[] samples;

    // Weighted Pool
    private WeatherHandle[] cachedPool;
    private float[] cachedPositions;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.sampleDistance = EngineSetting.WEATHER_REGION_SAMPLE_DISTANCE;
        this.noiseCellSize = EngineSetting.WEATHER_NOISE_CELL_SIZE;

        // Reference
        this.referenceCoordinate = Coordinate2Long.pack(0, 0);

        // Samples
        this.samples = new WeatherSampleStruct[5];
        for (int i = 0; i < samples.length; i++)
            samples[i] = new WeatherSampleStruct();
    }

    @Override
    protected void get() {

        // Internal
        this.windManager = get(WindManager.class);
    }

    // Reference \\

    void setReferenceCoordinate(long chunkCoordinate) {
        this.referenceCoordinate = chunkCoordinate;
    }

    // Sampling \\

    void sampleRegions(WeatherHandle[] pool) {

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
            WeatherHandle[] pool) {

        float noise = sampleNoise(chunkX, chunkY);
        blendPool(sample, pool, noise);
    }

    // Blend \\

    private void blendPool(WeatherSampleStruct sample, WeatherHandle[] pool, float noise) {

        if (pool != cachedPool)
            rebuildWeightedPositions(pool);

        if (pool.length == 1) {
            writeSample(sample, pool[0], pool[0], 0f);
            return;
        }

        int lowIndex = 0;

        while (lowIndex < pool.length - 2 && noise >= cachedPositions[lowIndex + 1])
            lowIndex++;

        int highIndex = lowIndex + 1;
        float segmentStart = cachedPositions[lowIndex];
        float segmentEnd = cachedPositions[highIndex];
        float t = segmentEnd > segmentStart ? (noise - segmentStart) / (segmentEnd - segmentStart) : 0f;

        writeSample(sample, pool[lowIndex], pool[highIndex], t);
    }

    /*
     * Rebuilds the weighted blend breakpoints whenever WeatherManager hands
     * over a new pool reference — only happens on season change, see
     * WeatherManager.update(). Entry i's breakpoint spacing is proportional
     * to the combined chance of entries i and i+1, so a higher-chance entry
     * claims a wider share of the noise domain and gets blended toward more
     * often as the noise field drifts. Reduces to the old even 1/(N-1)
     * spacing exactly when every entry's chance is equal.
     */
    private void rebuildWeightedPositions(WeatherHandle[] pool) {

        cachedPool = pool;

        if (pool.length == 1)
            return;

        if (cachedPositions == null || cachedPositions.length != pool.length)
            cachedPositions = new float[pool.length];

        float totalGap = 0f;

        for (int i = 0; i < pool.length - 1; i++)
            totalGap += pool[i].getChance() + pool[i + 1].getChance();

        if (totalGap <= 0f)
            throwException("Weather pool has zero total chance — every entry's 'chance' is 0 or unset");

        cachedPositions[0] = 0f;

        float runningGap = 0f;

        for (int i = 1; i < pool.length; i++) {
            runningGap += pool[i - 1].getChance() + pool[i].getChance();
            cachedPositions[i] = runningGap / totalGap;
        }
    }

    private void writeSample(
            WeatherSampleStruct sample,
            WeatherHandle low,
            WeatherHandle high,
            float t) {

        sample.setCloudCoverage(lerp(low.getCloudCoverage(), high.getCloudCoverage(), t));
        sample.setPrecipitationIntensity(lerp(low.getPrecipitationIntensity(), high.getPrecipitationIntensity(), t));
        sample.setWindSpeedScale(lerp(low.getWindSpeedScale(), high.getWindSpeedScale(), t));
        sample.setFogDensityScale(lerp(low.getFogDensityScale(), high.getFogDensityScale(), t));
        sample.setCloudType(lerp(low.getCloudType().ordinal(), high.getCloudType().ordinal(), t));

        sample.setCloudColor(
                lerp(low.getCloudColor().x, high.getCloudColor().x, t),
                lerp(low.getCloudColor().y, high.getCloudColor().y, t),
                lerp(low.getCloudColor().z, high.getCloudColor().z, t));
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    // Noise \\

    /*
     * Coherent 2D value noise over chunk coordinates, drifted by the current
     * local wind (WindManager) instead of a fixed +X axis — direction comes
     * straight from WindHandle.getLocalWindDirection(), already blended by
     * season and active weather, so weather fronts visibly move with
     * whichever way the wind is actually blowing. Mirrors the bilinear
     * hash-noise approach used by Clouds.glsl, kept CPU-side and
     * self-contained here.
     *
     * NOT YET WRAPPED — chunkX/chunkY should route through WorldWrapUtility
     * the same way GridInstance does, so sampling is consistent across the
     * world seam. Blocked on that utility's API — see chat.
     */
    private float sampleNoise(int chunkX, int chunkY) {

        WindHandle windHandle = windManager.getWindHandle();
        Vector3 windDirection = windHandle.getLocalWindDirection();
        float windSpeed = windHandle.getLocalWindSpeed();
        float driftDistance = elapsedTime * windSpeed * EngineSetting.WEATHER_WIND_DRIFT_SCALE;

        float sampleX = chunkX / noiseCellSize + windDirection.x * driftDistance;
        float sampleY = chunkY / noiseCellSize + windDirection.z * driftDistance;

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