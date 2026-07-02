package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;

class RegionSampleBranch extends BranchPackage {

    /*
     * Continuously samples a coherent, drifting noise field over world-space
     * chunk coordinates at the reference coordinate and its four cardinal
     * neighbors. Each sample resolves against the pool handed to it by
     * WeatherManager (already sorted by ascending cloud coverage) and blends
     * between the two nearest entries so weather never pops as the noise
     * field drifts — it fades.
     */

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

        if (pool.length == 1) {
            writeSample(sample, pool[0], pool[0], 0f);
            return;
        }

        float scaled = noise * (pool.length - 1);
        int lowIndex = (int) scaled;
        int highIndex = Math.min(lowIndex + 1, pool.length - 1);
        float t = scaled - lowIndex;

        writeSample(sample, pool[lowIndex], pool[highIndex], t);
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
     * Coherent 2D value noise over chunk coordinates, drifted over real time
     * so weather fronts visibly move across the world instead of sitting
     * static on the grid. Mirrors the bilinear hash-noise approach used by
     * Clouds.glsl, kept CPU-side and self-contained here.
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