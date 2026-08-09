package application.bootstrap.worldpipeline.biomemanager;

import java.util.concurrent.ConcurrentHashMap;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.assets.image.Pixmap;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.NoiseUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BiomeManager extends ManagerPackage {

    /*
     * Owns the biome palette and every runtime-queryable index derived from
     * it: name/handle and ID/handle registries, plus the map-color index used
     * to resolve a world PNG pixel to a biome. World generation resolves a
     * biome for every new chunk from whichever worker thread is generating
     * it, so every read path here — getBiome() and everything it calls — is
     * lock-free: the two registries are ConcurrentHashMaps, and the map-color
     * index is an immutable snapshot published through a volatile reference.
     * Only the rare mutation paths (registering a newly loaded biome or a
     * newly discovered map color) take a lock, and that lock never blocks a
     * concurrent reader.
     */

    // Palette
    private final ConcurrentHashMap<String, BiomeHandle> biomeName2BiomeHandle = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Short, BiomeHandle> biomeID2BiomeHandle = new ConcurrentHashMap<>();

    // Map Color Index — an immutable snapshot swapped in on every
    // registration, so getNearestBiomeNameForColor() never locks against it.
    private volatile ColorIndex colorIndex = ColorIndex.EMPTY;

    private static final class ColorIndex {

        static final ColorIndex EMPTY = new ColorIndex(new int[0], new String[0]);

        final int[] colors;
        final String[] names;

        ColorIndex(int[] colors, String[] names) {
            this.colors = colors;
            this.names = names;
        }
    }

    // Base \\

    @Override
    protected void create() {
        create(BiomeLoader.class);
    }

    // Management \\

    synchronized void addBiome(BiomeHandle biomeHandle) {

        if (biomeHandle.getBiomeID() == EngineSetting.REGISTRY_RESERVED_ID)
            throwException("Biome \"" + biomeHandle.getBiomeName()
                    + "\" hashed to the reserved registry ID (" + EngineSetting.REGISTRY_RESERVED_ID
                    + "), which the biome palette uses as its \"not yet generated\" sentinel — "
                    + "rename this biome so its hashed ID no longer collides with the sentinel.");

        BiomeHandle existing = biomeID2BiomeHandle.get(biomeHandle.getBiomeID());

        if (existing != null && RegistryUtility.isCollision(biomeHandle.getBiomeName(), existing.getBiomeName(),
                biomeHandle.getBiomeID()))
            throwException("Biome ID collision: '"
                    + biomeHandle.getBiomeName() + "' collides with '"
                    + existing.getBiomeName() + "' (ID " + biomeHandle.getBiomeID()
                    + ") — rename one biome to resolve");

        biomeName2BiomeHandle.put(biomeHandle.getBiomeName(), biomeHandle);
        biomeID2BiomeHandle.put(biomeHandle.getBiomeID(), biomeHandle);
    }

    synchronized void registerMapColor(String biomeName, int mapColor) {

        ColorIndex current = colorIndex;
        int size = current.colors.length;

        int[] colors = java.util.Arrays.copyOf(current.colors, size + 1);
        String[] names = java.util.Arrays.copyOf(current.names, size + 1);

        colors[size] = mapColor;
        names[size] = biomeName;

        colorIndex = new ColorIndex(colors, names);
    }

    // On-Demand \\

    public void request(String biomeName) {
        ((BiomeLoader) internalLoader).request(biomeName);
    }

    // World Map Resolution \\

    public BiomeHandle getBiome(WorldHandle worldHandle, long chunkCoordinate) {

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        BiomeHandle baseBiome = getBiomeHandleFromBiomeName(sampleMapBiomeName(worldHandle, chunkX, chunkZ));

        return applyProbableVariance(worldHandle, baseBiome, chunkX, chunkZ);
    }

    public short getBiomeIDFromChunkCoordinate(WorldHandle worldHandle, long chunkCoordinate) {
        return getBiome(worldHandle, chunkCoordinate).getBiomeID();
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

        int color = map.getPixelRGB(sampleX, sampleZ);

        return getNearestBiomeNameForColor(color);
    }

    private String getNearestBiomeNameForColor(int color) {

        ColorIndex index = colorIndex;

        if (index.colors.length == 0)
            throwException(
                    "No biomes define a \"map_color\" — world generation cannot resolve a biome from the world map.");

        int targetR = (color >> 16) & 0xFF;
        int targetG = (color >> 8) & 0xFF;
        int targetB = color & 0xFF;

        String nearestName = null;
        int nearestDistanceSq = Integer.MAX_VALUE;

        for (int i = 0; i < index.colors.length; i++) {

            int candidate = index.colors[i];
            int dr = ((candidate >> 16) & 0xFF) - targetR;
            int dg = ((candidate >> 8) & 0xFF) - targetG;
            int db = (candidate & 0xFF) - targetB;
            int distanceSq = dr * dr + dg * dg + db * db;

            if (distanceSq < nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearestName = index.names[i];
            }
        }

        return nearestName;
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

    public boolean hasBiome(String biomeName) {
        return biomeName2BiomeHandle.containsKey(biomeName);
    }

    public BiomeHandle getBiomeHandleFromBiomeName(String biomeName) {

        BiomeHandle handle = biomeName2BiomeHandle.get(biomeName);

        if (handle == null) {
            request(biomeName);
            handle = biomeName2BiomeHandle.get(biomeName);
        }

        if (handle == null)
            throwException("Biome \"" + biomeName + "\" was not registered after its on-demand load completed — "
                    + "the loaded file must declare a different biome name than the one requested. "
                    + "Check for a resource-name/path mismatch between the biome directory and its declared name.");

        return handle;
    }

    public short getBiomeIDFromBiomeName(String biomeName) {
        return getBiomeHandleFromBiomeName(biomeName).getBiomeID();
    }

    public BiomeHandle getBiomeHandleFromBiomeID(short biomeID) {

        if (biomeID == EngineSetting.REGISTRY_RESERVED_ID)
            throwException("Biome ID 0 was queried — this is the reserved \"not yet generated\" sentinel a "
                    + "subchunk's biome palette holds until world generation actually runs on it. The caller "
                    + "read biome data before ChunkData.GENERATION_DATA was set for this chunk.");

        BiomeHandle handle = biomeID2BiomeHandle.get(biomeID);

        if (handle == null)
            throwException("No handle registered for biome ID: " + biomeID);

        return handle;
    }
}