package application.bootstrap.weatherpipeline.weathermanager;

import java.util.Arrays;

import application.bootstrap.weatherpipeline.util.WeatherNoiseUtility;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RegionSampleSystem extends SystemPackage {

    /*
     * Reads the static weather image and turns a pixel of it into exactly one
     * weather. Raw noise clusters around its midpoint, so every read is first
     * converted into a percentile against the active world's own measured
     * noise distribution — a weather authored at 20% chance then genuinely
     * owns 20% of the map. The percentile walks a biome pool that
     * WeatherManager has ordered from calmest to most severe, so neighbouring
     * weathers in the image grade into one another the way a front does.
     */

    // Internal
    private WorldManager worldManager;

    // Distribution
    private WorldHandle distributionWorld;
    private float[] noiseDistribution;

    // Base \\

    @Override
    protected void create() {
        int sampleCount = EngineSetting.WEATHER_NOISE_DISTRIBUTION_SAMPLES_X
                * EngineSetting.WEATHER_NOISE_DISTRIBUTION_SAMPLES_Z;

        this.noiseDistribution = new float[sampleCount];
    }

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
    }

    // Sample \\

    float samplePercentile(double noiseChunkX, double noiseChunkZ) {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        if (activeWorld != distributionWorld)
            buildDistribution(activeWorld);

        return resolvePercentile(sampleNoise(activeWorld, noiseChunkX, noiseChunkZ));
    }

    private float sampleNoise(WorldHandle activeWorld, double noiseChunkX, double noiseChunkZ) {
        return WeatherNoiseUtility.sample(
                EngineSetting.WEATHER_NOISE_SEED,
                noiseChunkX,
                noiseChunkZ,
                activeWorld.getWorldScale().x / (double) EngineSetting.CHUNK_SIZE,
                activeWorld.getWorldScale().y / (double) EngineSetting.CHUNK_SIZE,
                EngineSetting.WEATHER_NOISE_CELL_SIZE);
    }

    // Distribution \\

    private void buildDistribution(WorldHandle activeWorld) {

        int samplesX = EngineSetting.WEATHER_NOISE_DISTRIBUTION_SAMPLES_X;
        int samplesZ = EngineSetting.WEATHER_NOISE_DISTRIBUTION_SAMPLES_Z;
        double stepX = activeWorld.getWorldScale().x / (double) EngineSetting.CHUNK_SIZE / samplesX;
        double stepZ = activeWorld.getWorldScale().y / (double) EngineSetting.CHUNK_SIZE / samplesZ;

        for (int z = 0; z < samplesZ; z++)
            for (int x = 0; x < samplesX; x++)
                noiseDistribution[z * samplesX + x] = sampleNoise(
                        activeWorld, (x + 0.5) * stepX, (z + 0.5) * stepZ);

        Arrays.sort(noiseDistribution);
        this.distributionWorld = activeWorld;
    }

    private float resolvePercentile(float noise) {

        int index = Arrays.binarySearch(noiseDistribution, noise);
        int insertion = index >= 0 ? index : -index - 1;

        return clamp01(insertion / (float) noiseDistribution.length);
    }

    // Pool \\

    WeatherHandle pickFromPool(
            ObjectArrayList<WeatherHandle> poolHandles,
            FloatArrayList poolChances,
            float percentile) {

        if (poolHandles.size() == 1)
            return poolHandles.get(0);

        float total = 0f;

        for (int i = 0; i < poolChances.size(); i++)
            total += Math.max(0f, poolChances.getFloat(i));

        if (total <= 0f)
            return poolHandles.get(0);

        float target = clamp01(percentile) * total;
        float cumulative = 0f;

        for (int i = 0; i < poolHandles.size(); i++) {

            cumulative += Math.max(0f, poolChances.getFloat(i));

            if (target <= cumulative)
                return poolHandles.get(i);
        }

        return poolHandles.get(poolHandles.size() - 1);
    }

    // Utility \\

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
