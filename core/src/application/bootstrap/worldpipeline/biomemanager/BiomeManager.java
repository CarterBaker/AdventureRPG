package application.bootstrap.worldpipeline.biomemanager;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.assets.image.Pixmap;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.NoiseUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class BiomeManager extends ManagerPackage {

    /*
     * Owns the biome palette for the engine lifetime and resolves which
     * biome applies at any chunk coordinate for a given world. Resolution
     * samples that world's map PNG through BiomeLoader's pre-registered
     * map-color table (so it never waits on a specific biome's full JSON
     * load), warps the sample position with low-frequency noise so borders
     * read as organic coastlines rather than the PNG's raw pixel grid, then
     * rolls the matched biome's own "probable_biomes" pool so hand-authored
     * variety can appear within or instead of the PNG's base pick. Reachable
     * from the world-generation thread as well as the main thread, so every
     * entry point that can trigger or observe a biome load is synchronized.
     */

    // Palette
    private Object2ShortOpenHashMap<String> biomeName2BiomeID;
    private Short2ObjectOpenHashMap<BiomeHandle> biomeID2BiomeHandle;

    // Base \\

    @Override
    protected void create() {

        this.biomeName2BiomeID = new Object2ShortOpenHashMap<>();
        this.biomeID2BiomeHandle = new Short2ObjectOpenHashMap<>();

        create(BiomeLoader.class);
    }

    // Management \\

    synchronized void addBiome(BiomeHandle biomeHandle) {

        if (biomeID2BiomeHandle.containsKey(biomeHandle.getBiomeID())) {
            BiomeHandle existing = biomeID2BiomeHandle.get(biomeHandle.getBiomeID());
            if (RegistryUtility.isCollision(biomeHandle.getBiomeName(), existing.getBiomeName(),
                    biomeHandle.getBiomeID()))
                throwException("Biome ID collision: '"
                        + biomeHandle.getBiomeName() + "' collides with '"
                        + existing.getBiomeName() + "' (ID " + biomeHandle.getBiomeID()
                        + ") — rename one biome to resolve");
        }

        biomeName2BiomeID.put(biomeHandle.getBiomeName(), biomeHandle.getBiomeID());
        biomeID2BiomeHandle.put(biomeHandle.getBiomeID(), biomeHandle);
    }

    // On-Demand \\

    public synchronized void request(String biomeName) {
        ((BiomeLoader) internalLoader).request(biomeName);
    }

    // World Map Resolution \\

    public synchronized BiomeHandle getBiomeHandleFromChunkCoordinate(WorldHandle worldHandle, long chunkCoordinate) {

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        BiomeHandle baseBiome = getBiomeHandleFromBiomeName(sampleMapBiomeName(worldHandle, chunkX, chunkZ));

        return applyProbableVariance(worldHandle, baseBiome, chunkX, chunkZ);
    }

    public synchronized short getBiomeIDFromChunkCoordinate(WorldHandle worldHandle, long chunkCoordinate) {
        return getBiomeHandleFromChunkCoordinate(worldHandle, chunkCoordinate).getBiomeID();
    }

    private String sampleMapBiomeName(WorldHandle worldHandle, int chunkX, int chunkZ) {

        Pixmap map = worldHandle.getWorld();
        long seed = worldHandle.getSeed();

        double pixelX = chunkX / (double) EngineSetting.CHUNKS_PER_PIXEL;
        double pixelZ = chunkZ / (double) EngineSetting.CHUNKS_PER_PIXEL;

        float warpX = NoiseUtility.noise2(
                seed ^ EngineSetting.BIOME_BORDER_WARP_SEED,
                pixelX * EngineSetting.BIOME_BORDER_WARP_FREQUENCY,
                pixelZ * EngineSetting.BIOME_BORDER_WARP_FREQUENCY);
        float warpZ = NoiseUtility.noise2(
                seed ^ EngineSetting.BIOME_BORDER_WARP_SEED ^ EngineSetting.HASH_FINALIZER_MULTIPLIER_1,
                pixelX * EngineSetting.BIOME_BORDER_WARP_FREQUENCY,
                pixelZ * EngineSetting.BIOME_BORDER_WARP_FREQUENCY);

        int sampleX = wrapPixelIndex(
                (int) Math.floor(pixelX + warpX * EngineSetting.BIOME_BORDER_WARP_STRENGTH_PIXELS), map.getWidth());
        int sampleZ = wrapPixelIndex(
                (int) Math.floor(pixelZ + warpZ * EngineSetting.BIOME_BORDER_WARP_STRENGTH_PIXELS), map.getHeight());

        int color = map.getPixel(sampleX, sampleZ);

        return ((BiomeLoader) internalLoader).getNearestBiomeNameForColor(color);
    }

    private BiomeHandle applyProbableVariance(WorldHandle worldHandle, BiomeHandle baseBiome, int chunkX, int chunkZ) {

        ObjectArrayList<String> probableNames = baseBiome.getProbableBiomeNames();

        if (probableNames.isEmpty())
            return baseBiome;

        FloatArrayList probableChances = baseBiome.getProbableBiomeChances();

        float roll = NoiseUtility.noise2(
                worldHandle.getSeed() ^ EngineSetting.BIOME_VARIANCE_SEED,
                chunkX * EngineSetting.BIOME_VARIANCE_NOISE_FREQUENCY,
                chunkZ * EngineSetting.BIOME_VARIANCE_NOISE_FREQUENCY) * 0.5f + 0.5f;

        float cumulative = 0f;

        for (int i = 0; i < probableNames.size(); i++) {

            cumulative += probableChances.getFloat(i);

            if (roll < cumulative)
                return getBiomeHandleFromBiomeName(probableNames.get(i));
        }

        return baseBiome;
    }

    private static int wrapPixelIndex(int value, int range) {
        int wrapped = value % range;
        if (wrapped < 0)
            wrapped += range;
        return wrapped;
    }

    // Accessible \\

    public synchronized boolean hasBiome(String biomeName) {
        return biomeName2BiomeID.containsKey(biomeName);
    }

    public synchronized short getBiomeIDFromBiomeName(String biomeName) {

        if (!biomeName2BiomeID.containsKey(biomeName))
            request(biomeName);

        return biomeName2BiomeID.getShort(biomeName);
    }

    public synchronized BiomeHandle getBiomeHandleFromBiomeID(short biomeID) {

        BiomeHandle handle = biomeID2BiomeHandle.get(biomeID);

        if (handle == null)
            throwException("No handle registered for biome ID: " + biomeID);

        return handle;
    }

    public synchronized BiomeHandle getBiomeHandleFromBiomeName(String biomeName) {
        return getBiomeHandleFromBiomeID(getBiomeIDFromBiomeName(biomeName));
    }
}