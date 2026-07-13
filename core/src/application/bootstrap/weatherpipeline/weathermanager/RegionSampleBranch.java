package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.random.WeightedChanceUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Owns the coherent regional weather noise field — a continuous 2D,
 * X-wrapped layer (see ToroidalNoiseUtility) blended with GlobalNoiseBranch's
 * broad current, both scrolling with the world's rotation at the same rate
 * — and resolves it against a chance-weighted pool via resolveBand() and
 * resolveBandTowardHorizon(). These are the canonical noise-to-weather
 * resolution paths shared by WeatherManager, WeatherPatternManager, and
 * this class's own center-point atmosphere sample.
 */
class RegionSampleBranch extends BranchPackage {

    private static final long NOISE_SEED = 0x51A5F00DCAFEBEEFL;
    private static final double WAVELENGTH_CHUNKS = 128.0;
    private static final double DRIFT_SPEED_X = 0.006;
    private static final double DRIFT_SPEED_Z = 0.15;
    private static final double DRIFT_Z_WRAP = 1.0E7;

    private GlobalNoiseBranch globalNoiseBranch;
    private WorldManager worldManager;

    private long referenceCoordinate;

    private double driftPhaseX;
    private double driftChunksZ;

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

    float getEffectiveNearRangeChunks() {
        return Math.min(settings.maxRenderDistance, (float) EngineSetting.WEATHER_NEAR_RANGE_CHUNKS);
    }

    // Sampling \\

    void sampleRegions(ObjectArrayList<WeatherPoolEntryStruct> pool) {

        advanceDrift();

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

    // Drift \\

    private void advanceDrift() {
        float deltaTime = internal.getDeltaTime();
        driftPhaseX += DRIFT_SPEED_X * deltaTime;
        driftPhaseX %= (Math.PI * 2.0);
        driftChunksZ += DRIFT_SPEED_Z * deltaTime;
        driftChunksZ %= DRIFT_Z_WRAP;
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

    void resolveBandTowardHorizon(
            WeatherBandStruct out,
            int homeChunkX,
            int homeChunkZ,
            int referenceChunkX,
            int referenceChunkZ,
            ObjectArrayList<WeatherPoolEntryStruct> pool) {

        double dx = homeChunkX - referenceChunkX;
        double dz = homeChunkZ - referenceChunkZ;
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

        double rotationPhase = (globalNoiseBranch.getRotationAngleDegrees() / EngineSetting.DEGREES_PER_FULL_ROTATION)
                * (Math.PI * 2.0);

        return WeatherNoiseUtility.sample(
                NOISE_SEED,
                chunkX, chunkZ,
                worldWidthChunks,
                WAVELENGTH_CHUNKS,
                rotationPhase + driftPhaseX,
                driftChunksZ);
    }

    // Accessible \\

    WeatherSampleStruct getCenterSample() {
        return sample;
    }
}