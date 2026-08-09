package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.weatherpipeline.util.WeatherNoiseUtility;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
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
     * Resolves the regional weather noise field into exactly one weather
     * handle from a chance-weighted pool — no blending between candidates.
     * Callers own their own reference coordinate and any state/smoothing on
     * top of whatever this returns.
     */

    private GlobalNoiseSystem globalNoiseSystem;
    private WorldManager worldManager;

    @Override
    protected void get() {
        this.globalNoiseSystem = get(GlobalNoiseSystem.class);
        this.worldManager = get(WorldManager.class);
    }

    /*
     * Fixed, terrain-independent range — the sky/weather map is resolved
     * from UBO data and a ray/plane intersection, never from loaded chunk
     * geometry, so it must never be clamped against the terrain streaming
     * radius (settings.maxRenderDistance). This is the sole authority for
     * how far the CPU weather simulation and the skybox's distant cloud
     * sampling reach — the same single range is used to stream patterns in,
     * cull the weather map, and retire patterns that drift out of it.
     */

    float getEffectiveRangeChunks() {
        return EngineSetting.WEATHER_RANGE_CHUNKS;
    }

    private float combinedNoiseAt(int chunkX, int chunkZ) {

        float localNoise = sampleNoise(chunkX, chunkZ);
        float globalIntensity = globalNoiseSystem.sampleGlobalIntensity(Coordinate2Long.pack(chunkX, chunkZ));

        return lerp(localNoise, globalIntensity, globalNoiseSystem.getGlobalInfluence());
    }

    WeatherHandle resolveWeather(
            int chunkX, int chunkZ,
            ObjectArrayList<WeatherHandle> poolHandles,
            FloatArrayList poolChances) {
        return pickFromPool(poolHandles, poolChances, combinedNoiseAt(chunkX, chunkZ));
    }

    WeatherHandle resolveWeatherTowardHorizon(
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

        double effectiveRangeChunks = getEffectiveRangeChunks();
        double clampedDistance = Math.min(distanceChunks, effectiveRangeChunks);
        float distanceT = effectiveRangeChunks > 0.0
                ? (float) (clampedDistance / effectiveRangeChunks)
                : 1f;

        float nearNoise = combinedNoiseAt(homeChunkX, homeChunkZ);

        if (distanceT <= 0.0001f)
            return pickFromPool(poolHandles, poolChances, nearNoise);

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

        return pickFromPool(poolHandles, poolChances, blendedNoise);
    }

    /*
     * Noise picks exactly one weather from the pool — whichever chance-
     * weighted band it falls into. No blending between candidates; a
     * weather either owns this sample or it doesn't.
     */
    private WeatherHandle pickFromPool(
            ObjectArrayList<WeatherHandle> poolHandles,
            FloatArrayList poolChances,
            float noise) {

        if (poolHandles.size() == 1)
            return poolHandles.get(0);

        float total = 0f;

        for (int i = 0; i < poolChances.size(); i++)
            total += Math.max(0f, poolChances.getFloat(i));

        if (total <= 0f)
            return poolHandles.get(0);

        float target = clamp01(noise) * total;
        float cumulative = 0f;

        for (int i = 0; i < poolHandles.size(); i++) {

            float chance = Math.max(0f, poolChances.getFloat(i));
            cumulative += chance;

            boolean isLast = i == poolHandles.size() - 1;

            if (target <= cumulative || isLast)
                return poolHandles.get(i);
        }

        return poolHandles.get(poolHandles.size() - 1);
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
                EngineSetting.WEATHER_NOISE_SEED,
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