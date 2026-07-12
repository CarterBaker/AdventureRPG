package application.bootstrap.weatherpipeline.weathermanager;

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
     * Owns the coherent weather noise field — a regional layer blended with
     * GlobalNoiseBranch's planet-rotation noise, both scrolling along the
     * world's longitudinal axis at the exact same rotation-driven rate — and
     * resolves it against a chance-weighted pool via resolveBand() and
     * resolveBandTowardHorizon(). These are the single canonical noise-to-
     * weather resolution path shared by WeatherManager, WeatherPatternManager,
     * and this class's own center-point atmosphere sample.
     */

    private GlobalNoiseBranch globalNoiseBranch;
    private WorldManager worldManager;

    private float noiseCellSize;

    private long referenceCoordinate;

    private double evolutionElapsedSeconds;
    private int evolutionTimeCell;
    private float evolutionTimeBlend;

    private final WeatherSampleStruct sample = new WeatherSampleStruct();
    private final WeatherSampleStruct targetSample = new WeatherSampleStruct();
    private boolean smoothingInitialized;

    private final WeatherBandStruct bandScratch = new WeatherBandStruct();

    @Override
    protected void create() {
        this.noiseCellSize = EngineSetting.WEATHER_NOISE_CELL_SIZE;
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

        advanceEvolution();

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

    // Evolution \\

    private void advanceEvolution() {

        evolutionElapsedSeconds += internal.getDeltaTime();
        evolutionElapsedSeconds %= EngineSetting.WEATHER_LOCAL_DRIFT_TIME_WRAP;

        double phase = evolutionElapsedSeconds / EngineSetting.WEATHER_LOCAL_EVOLUTION_PERIOD;
        double cell = Math.floor(phase);

        evolutionTimeCell = (int) cell;
        evolutionTimeBlend = smoothstep((float) (phase - cell));
    }

    // Resolution \\

    private float combinedNoiseAt(int chunkX, int chunkY) {

        float localNoise = sampleNoise(chunkX, chunkY);
        float globalIntensity = globalNoiseBranch.sampleGlobalIntensity(Coordinate2Long.pack(chunkX, chunkY));

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

    /*
     * Coherent 2D value noise over chunk coordinates, wrapped seamlessly at
     * the world edge. Scrolls along the world's longitudinal axis at the
     * exact same rotation-driven rate as GlobalNoiseBranch's own field, so
     * the two layers can never drift apart from each other. Each grid point
     * blends two time-decorrelated hash layers via evolvingHash() so the
     * field also morphs in place independently of that scroll.
     */
    private float sampleNoise(int chunkX, int chunkY) {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        double worldWidthChunks = activeWorld.getWorldScale().x / (double) EngineSetting.CHUNK_SIZE;
        double worldHeightChunks = activeWorld.getWorldScale().y / (double) EngineSetting.CHUNK_SIZE;

        int cellsX = (int) Math.max(1L, Math.round(worldWidthChunks / noiseCellSize));
        int cellsY = (int) Math.max(1L, Math.round(worldHeightChunks / noiseCellSize));

        double rotationProgress = globalNoiseBranch.getRotationAngleDegrees() / EngineSetting.DEGREES_PER_FULL_ROTATION;
        double u = wrap01(chunkX / worldWidthChunks + rotationProgress);
        double v = wrap01(chunkY / worldHeightChunks);

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

        float n00 = evolvingHash(wrappedX0, wrappedY0);
        float n10 = evolvingHash(wrappedX1, wrappedY0);
        float n01 = evolvingHash(wrappedX0, wrappedY1);
        float n11 = evolvingHash(wrappedX1, wrappedY1);

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

    private float evolvingHash(int x, int y) {

        float layerA = hash(x, y, evolutionTimeCell);
        float layerB = hash(x, y, evolutionTimeCell + 1);

        return lerp(layerA, layerB, evolutionTimeBlend);
    }

    private float hash(int x, int y, int timeLayer) {

        int h = x * 374761393 + y * 668265263 + timeLayer * 1013904223;
        h = (h ^ (h >>> 13)) * 1274126177;

        return ((h ^ (h >>> 16)) & 0x7fffffff) / (float) Integer.MAX_VALUE;
    }

    private float smoothstep(float t) {
        return t * t * (3f - 2f * t);
    }

    // Accessible \\

    WeatherSampleStruct getCenterSample() {
        return sample;
    }
}