package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.weatherband.WeatherBandInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RegionSampleSystem extends SystemPackage {

    /*
     * Resolves the regional weather noise field against a chance-weighted
     * pool, either at an exact chunk coordinate or blended toward a
     * horizon-direction sample. Pure resolution logic — every caller owns
     * its own reference coordinate and any state/smoothing on top of
     * whatever this returns. Pools are supplied as parallel fastutil
     * lists — handles alongside their chance weights.
     */

    private static final long NOISE_SEED = 0x51A5F00DCAFEBEEFL;

    private GlobalNoiseSystem globalNoiseSystem;
    private WorldManager worldManager;

    @Override
    protected void get() {
        this.globalNoiseSystem = get(GlobalNoiseSystem.class);
        this.worldManager = get(WorldManager.class);
    }

    /*
     * Fixed, terrain-independent ranges — the sky/weather map is resolved
     * from UBO data and a ray/plane intersection, never from loaded chunk
     * geometry, so it must never be clamped against the terrain streaming
     * radius (settings.maxRenderDistance). These are the sole authority for
     * how far the CPU weather simulation and the skybox's distant cloud
     * sampling reach.
     */

    float getEffectiveOuterRangeChunks() {
        return EngineSetting.WEATHER_OUTER_RANGE_CHUNKS;
    }

    float getEffectiveNearRangeChunks() {
        return Math.min(getEffectiveOuterRangeChunks(), (float) EngineSetting.WEATHER_NEAR_RANGE_CHUNKS);
    }

    private float combinedNoiseAt(int chunkX, int chunkZ) {

        float localNoise = sampleNoise(chunkX, chunkZ);
        float globalIntensity = globalNoiseSystem.sampleGlobalIntensity(Coordinate2Long.pack(chunkX, chunkZ));

        return lerp(localNoise, globalIntensity, globalNoiseSystem.getGlobalInfluence());
    }

    void resolveBand(
            WeatherBandInstance out,
            int chunkX, int chunkZ,
            ObjectArrayList<WeatherHandle> poolHandles,
            FloatArrayList poolChances) {
        bandFromPool(out, poolHandles, poolChances, combinedNoiseAt(chunkX, chunkZ));
    }

    void resolveBandTowardHorizon(
            WeatherBandInstance out,
            int homeChunkX,
            int homeChunkZ,
            int referenceChunkX,
            int referenceChunkZ,
            ObjectArrayList<WeatherHandle> poolHandles,
            FloatArrayList poolChances) {

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
            bandFromPool(out, poolHandles, poolChances, nearNoise);
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

        bandFromPool(out, poolHandles, poolChances, blendedNoise);
    }

    private void bandFromPool(
            WeatherBandInstance out,
            ObjectArrayList<WeatherHandle> poolHandles,
            FloatArrayList poolChances,
            float noise) {

        if (poolHandles.size() == 1) {
            WeatherHandle only = poolHandles.get(0);
            out.assign(only, only, 0f);
            return;
        }

        float total = 0f;

        for (int i = 0; i < poolChances.size(); i++)
            total += Math.max(0f, poolChances.getFloat(i));

        if (total <= 0f) {
            WeatherHandle first = poolHandles.get(0);
            out.assign(first, first, 0f);
            return;
        }

        float target = clamp01(noise) * total;
        float cumulative = 0f;

        for (int i = 0; i < poolHandles.size(); i++) {

            float chance = Math.max(0f, poolChances.getFloat(i));
            float bandEnd = cumulative + chance;
            boolean isLast = i == poolHandles.size() - 1;

            if (target <= bandEnd || isLast) {

                WeatherHandle low = poolHandles.get(i);
                int nextIndex = isLast ? i : i + 1;
                WeatherHandle high = poolHandles.get(nextIndex);

                float bandWidth = Math.max(bandEnd - cumulative, 0.0001f);
                float t = clamp01((target - cumulative) / bandWidth);

                out.assign(low, high, t);
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