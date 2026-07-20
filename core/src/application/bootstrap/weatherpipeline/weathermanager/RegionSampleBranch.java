// RegionSampleBranch.java
package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.random.WeightedChanceUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Owns the coherent regional weather noise field and resolves it against a
 * chance-weighted pool, both for the player's own position and for any
 * arbitrary home coordinate blended toward its own horizon-direction sample.
 */
class RegionSampleBranch extends BranchPackage {

    private static final long NOISE_SEED = 0x51A5F00DCAFEBEEFL;

    private GlobalNoiseBranch globalNoiseBranch;
    private WorldManager worldManager;

    private long referenceCoordinate;

    private final WeatherSampleStruct sample = new WeatherSampleStruct();
    private final WeatherSampleStruct targetSample = new WeatherSampleStruct();
    private boolean smoothingInitialized;

    private final WeatherBandStruct bandScratch = new WeatherBandStruct();

    @Override
    protected void create() {
        this.referenceCoordinate = Coordinate2Long.pack(0, 0);
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

    long getReferenceCoordinate() {
        return referenceCoordinate;
    }

    // Effective Range \\

    /*
     * The single shared boundary between the overhead system's outer edge
     * and the sky dome's near horizon ring. WeatherPatternManager and
     * SkyWeatherPatternBranch both read this method rather than recomputing
     * their own radius, so the two visual layers always hand off at the
     * same distance instead of drifting apart. The cap against
     * WEATHER_NEAR_RANGE_CHUNKS is a sanity ceiling only — under normal
     * settings the boundary sits at exactly settings.maxRenderDistance,
     * never half of it.
     */
    float getEffectiveNearRangeChunks() {
        return Math.min(settings.maxRenderDistance, (float) EngineSetting.WEATHER_NEAR_RANGE_CHUNKS);
    }

    // Sampling \\

    void sampleRegions(ObjectArrayList<WeatherPoolEntryStruct> pool) {

        int originX = Coordinate2Long.unpackX(referenceCoordinate);
        int originY = Coordinate2Long.unpackY(referenceCoordinate);

        resolveBand(bandScratch, originX, originY, pool);
        writeSample(targetSample, bandScratch.getLow(), bandScratch.getHigh(), bandScratch.getBlendFactor());

        advanceSmoothing();
    }

    // Smoothing \\

    private void advanceSmoothing() {

        if (!smoothingInitialized) {
            sample.copyFrom(targetSample);
            smoothingInitialized = true;
            return;
        }

        float deltaTime = internal.getDeltaTime();
        float alpha = 1f - (float) Math.exp(-deltaTime / EngineSetting.WEATHER_SAMPLE_SMOOTHING_TIME_SECONDS);

        sample.lerpToward(targetSample, alpha);
    }

    // Resolution \\

    private float combinedNoiseAt(int chunkX, int chunkZ) {

        float localNoise = sampleNoise(chunkX, chunkZ);
        float globalIntensity = globalNoiseBranch.sampleGlobalIntensity(Coordinate2Long.pack(chunkX, chunkZ));

        return lerp(localNoise, globalIntensity, globalNoiseBranch.getGlobalInfluence());
    }

    void resolveBand(WeatherBandStruct out, int chunkX, int chunkY, ObjectArrayList<WeatherPoolEntryStruct> pool) {
        bandFromPool(out, pool, combinedNoiseAt(chunkX, chunkY));
    }

    /*
     * Home and reference coordinates can legally sit on opposite sides of
     * the world's wrap seam while still being physically close together —
     * both the distance and the direction toward the horizon sample must
     * go through wrap-aware deltas, the same as every other distance check
     * in this pipeline, or a pattern near the seam resolves against a
     * meaningless direction and effectively random weather.
     */
    void resolveBandTowardHorizon(
            WeatherBandStruct out,
            int homeChunkX,
            int homeChunkZ,
            int referenceChunkX,
            int referenceChunkZ,
            ObjectArrayList<WeatherPoolEntryStruct> pool) {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        double dx = WorldWrapUtility.wrappedDeltaX(activeWorld, homeChunkX, referenceChunkX);
        double dz = WorldWrapUtility.wrappedDeltaZ(activeWorld, homeChunkZ, referenceChunkZ);
        double distanceChunks = Math.sqrt(dx * dx + dz * dz);

        double effectiveNearRangeChunks = getEffectiveNearRangeChunks();
        double clampedDistance = Math.min(distanceChunks, effectiveNearRangeChunks);
        float distanceT = effectiveNearRangeChunks > 0.0
                ? (float) (clampedDistance / effectiveNearRangeChunks)
                : 1f;

        float nearNoise = combinedNoiseAt(homeChunkX, homeChunkZ);

        if (distanceT <= 0.0001f) {
            bandFromPool(out, pool, nearNoise);
            return;
        }

        int farChunkX = homeChunkX;
        int farChunkZ = homeChunkZ;

        if (distanceChunks > 0.0001) {
            double dirX = dx / distanceChunks;
            double dirZ = dz / distanceChunks;
            farChunkX = referenceChunkX + (int) Math.round(dirX * EngineSetting.WEATHER_FAR_RANGE_CHUNKS);
            farChunkZ = referenceChunkZ + (int) Math.round(dirZ * EngineSetting.WEATHER_FAR_RANGE_CHUNKS);
        }

        float farNoise = combinedNoiseAt(farChunkX, farChunkZ);
        float blendedNoise = lerp(nearNoise, farNoise, distanceT);

        bandFromPool(out, pool, blendedNoise);
    }

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

    // Atmosphere Blend \\

    private void writeSample(WeatherSampleStruct sampleOut, WeatherHandle low, WeatherHandle high, float t) {

        sampleOut.setPrecipitationIntensity(lerp(low.getPrecipitationIntensity(), high.getPrecipitationIntensity(), t));
        sampleOut.setWindSpeedScale(lerp(low.getWindSpeedScale(), high.getWindSpeedScale(), t));
        sampleOut.setWindTurbulenceScale(lerp(low.getWindTurbulenceScale(), high.getWindTurbulenceScale(), t));
        sampleOut.setFogDensityScale(lerp(low.getFogDensityScale(), high.getFogDensityScale(), t));
        sampleOut.setHumidity(lerp(low.getHumidity(), high.getHumidity(), t));
        sampleOut.setVisibility(lerp(low.getVisibility(), high.getVisibility(), t));
        sampleOut.setTemperatureModifier(lerp(low.getTemperatureModifier(), high.getTemperatureModifier(), t));
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    // Noise \\

    private float sampleNoise(int chunkX, int chunkZ) {

        WorldHandle activeWorld = worldManager.getActiveWorld();
        double worldWidthChunks = activeWorld.getWorldScale().x / (double) EngineSetting.CHUNK_SIZE;
        double wavelengthChunks = EngineSetting.WEATHER_NOISE_CELL_SIZE;

        double rotationPhase = (globalNoiseBranch.getRotationAngleDegrees() / EngineSetting.DEGREES_PER_FULL_ROTATION)
                * (Math.PI * 2.0);

        double meanderAmplitudeChunks = EngineSetting.GLOBAL_WEATHER_MEANDER_INFLUENCE * wavelengthChunks;

        return WeatherNoiseUtility.sample(
                NOISE_SEED,
                chunkX, chunkZ,
                worldWidthChunks,
                wavelengthChunks,
                rotationPhase,
                globalNoiseBranch.getSeasonalDriftZChunks(),
                globalNoiseBranch.getMeanderWaveNumber(),
                meanderAmplitudeChunks,
                globalNoiseBranch.getMeanderPhase());
    }

    // Accessible \\

    WeatherSampleStruct getCenterSample() {
        return sample;
    }
}