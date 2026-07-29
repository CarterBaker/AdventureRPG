// RegionSampleSystem.java
package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.random.WeightedChanceUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RegionSampleSystem extends SystemPackage {

    /*
     * Resolves the regional weather noise field against a chance-weighted
     * pool, either at an exact chunk coordinate or blended toward a
     * horizon-direction sample. Pure resolution logic — every caller owns
     * its own reference coordinate and any state/smoothing on top of
     * whatever this returns.
     */

    private static final long NOISE_SEED = 0x51A5F00DCAFEBEEFL;

    private GlobalNoiseSystem globalNoiseSystem;
    private WorldManager worldManager;

    @Override
    protected void get() {
        this.globalNoiseSystem = get(GlobalNoiseSystem.class);
        this.worldManager = get(WorldManager.class);
    }

    float getEffectiveOuterRangeChunks() {
        return Math.min(settings.maxRenderDistance, (float) EngineSetting.WEATHER_OUTER_RANGE_CHUNKS);
    }

    float getEffectiveNearRangeChunks() {
        return Math.min(getEffectiveOuterRangeChunks(), (float) EngineSetting.WEATHER_NEAR_RANGE_CHUNKS);
    }

    private float combinedNoiseAt(int chunkX, int chunkZ) {

        float localNoise = sampleNoise(chunkX, chunkZ);
        float globalIntensity = globalNoiseSystem.sampleGlobalIntensity(Coordinate2Long.pack(chunkX, chunkZ));

        return lerp(localNoise, globalIntensity, globalNoiseSystem.getGlobalInfluence());
    }

    void resolveBand(WeatherBandStruct out, int chunkX, int chunkZ, ObjectArrayList<WeatherPoolEntryStruct> pool) {
        bandFromPool(out, pool, combinedNoiseAt(chunkX, chunkZ));
    }

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

        double effectiveOuterRangeChunks = getEffectiveOuterRangeChunks();
        double clampedDistance = Math.min(distanceChunks, effectiveOuterRangeChunks);
        float distanceT = effectiveOuterRangeChunks > 0.0
                ? (float) (clampedDistance / effectiveOuterRangeChunks)
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

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private float sampleNoise(int chunkX, int chunkZ) {

        WorldHandle activeWorld = worldManager.getActiveWorld();
        double worldWidthChunks = activeWorld.getWorldScale().x / (double) EngineSetting.CHUNK_SIZE;
        double worldHeightChunks = activeWorld.getWorldScale().y / (double) EngineSetting.CHUNK_SIZE;
        double wavelengthChunks = EngineSetting.WEATHER_NOISE_CELL_SIZE;

        double rotationPhase = (globalNoiseSystem.getRotationAngleDegrees() / EngineSetting.DEGREES_PER_FULL_ROTATION)
                * (Math.PI * 2.0);

        double meanderAmplitudeChunks = EngineSetting.GLOBAL_WEATHER_MEANDER_INFLUENCE * wavelengthChunks;

        return WeatherNoiseUtility.sample(
                NOISE_SEED,
                chunkX, chunkZ,
                worldWidthChunks,
                worldHeightChunks,
                wavelengthChunks,
                rotationPhase,
                globalNoiseSystem.getSeasonalDriftZChunks(),
                globalNoiseSystem.getMeanderWaveNumber(),
                meanderAmplitudeChunks,
                globalNoiseSystem.getMeanderPhase());
    }
}