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
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class BiomeManager extends ManagerPackage {

    /*
     * Owns the biome palette and every runtime-queryable index derived from
     * it: name/handle and ID/handle registries, plus the map-color index
     * used to resolve a world PNG pixel to a biome. The color index is
     * handed to this manager by BiomeLoader right after get() wires the
     * loader to it — the color data itself is peeked from disk during
     * scan(), before any biome is fully parsed, so a coordinate can be
     * matched to a biome name and trigger that one biome's on-demand load
     * without requiring every biome on disk to already be loaded. Both
     * world generation and weather resolve through getBiome(world,
     * coordinate); there is no ID hop in that path — an ID is only ever
     * read back off an already-resolved handle, for the compact per-voxel
     * biome palette.
     */

    // Palette
    private Object2ObjectOpenHashMap<String, BiomeHandle> biomeName2BiomeHandle;
    private Short2ObjectOpenHashMap<BiomeHandle> biomeID2BiomeHandle;

    // Map Color Index — populated by BiomeLoader once it can reach this manager
    private IntArrayList registeredMapColors;
    private ObjectArrayList<String> registeredMapColorNames;

    // Base \\

    @Override
    protected void create() {

        this.biomeName2BiomeHandle = new Object2ObjectOpenHashMap<>();
        this.biomeID2BiomeHandle = new Short2ObjectOpenHashMap<>();

        this.registeredMapColors = new IntArrayList();
        this.registeredMapColorNames = new ObjectArrayList<>();

        create(BiomeLoader.class);
    }

    // Management \\

    synchronized void addBiome(BiomeHandle biomeHandle) {

        if (biomeHandle.getBiomeID() == EngineSetting.REGISTRY_RESERVED_ID)
            throwException("Biome \"" + biomeHandle.getBiomeName()
                    + "\" hashed to the reserved registry ID (" + EngineSetting.REGISTRY_RESERVED_ID
                    + "), which the biome palette uses as its \"not yet generated\" sentinel — "
                    + "rename this biome so its hashed ID no longer collides with the sentinel.");

        if (biomeID2BiomeHandle.containsKey(biomeHandle.getBiomeID())) {
            BiomeHandle existing = biomeID2BiomeHandle.get(biomeHandle.getBiomeID());
            if (RegistryUtility.isCollision(biomeHandle.getBiomeName(), existing.getBiomeName(),
                    biomeHandle.getBiomeID()))
                throwException("Biome ID collision: '"
                        + biomeHandle.getBiomeName() + "' collides with '"
                        + existing.getBiomeName() + "' (ID " + biomeHandle.getBiomeID()
                        + ") — rename one biome to resolve");
        }

        biomeName2BiomeHandle.put(biomeHandle.getBiomeName(), biomeHandle);
        biomeID2BiomeHandle.put(biomeHandle.getBiomeID(), biomeHandle);
    }

    synchronized void registerMapColor(String biomeName, int mapColor) {
        registeredMapColors.add(mapColor);
        registeredMapColorNames.add(biomeName);
    }

    // On-Demand \\

    public synchronized void request(String biomeName) {
        ((BiomeLoader) internalLoader).request(biomeName);
    }

    // World Map Resolution \\

    public synchronized BiomeHandle getBiome(WorldHandle worldHandle, long chunkCoordinate) {

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        BiomeHandle baseBiome = getBiomeHandleFromBiomeName(sampleMapBiomeName(worldHandle, chunkX, chunkZ));

        return applyProbableVariance(worldHandle, baseBiome, chunkX, chunkZ);
    }

    public synchronized short getBiomeIDFromChunkCoordinate(WorldHandle worldHandle, long chunkCoordinate) {
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

        if (registeredMapColors.isEmpty())
            throwException(
                    "No biomes define a \"map_color\" — world generation cannot resolve a biome from the world map.");

        int targetR = (color >> 16) & 0xFF;
        int targetG = (color >> 8) & 0xFF;
        int targetB = color & 0xFF;

        String nearestName = null;
        int nearestDistanceSq = Integer.MAX_VALUE;

        for (int i = 0; i < registeredMapColors.size(); i++) {

            int candidate = registeredMapColors.getInt(i);
            int dr = ((candidate >> 16) & 0xFF) - targetR;
            int dg = ((candidate >> 8) & 0xFF) - targetG;
            int db = (candidate & 0xFF) - targetB;
            int distanceSq = dr * dr + dg * dg + db * db;

            if (distanceSq < nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearestName = registeredMapColorNames.get(i);
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

    public synchronized boolean hasBiome(String biomeName) {
        return biomeName2BiomeHandle.containsKey(biomeName);
    }

    public synchronized BiomeHandle getBiomeHandleFromBiomeName(String biomeName) {

        if (!biomeName2BiomeHandle.containsKey(biomeName))
            request(biomeName);

        BiomeHandle handle = biomeName2BiomeHandle.get(biomeName);

        if (handle == null)
            throwException("Biome \"" + biomeName + "\" was not registered after its on-demand load completed — "
                    + "the loaded file must declare a different biome name than the one requested. "
                    + "Check for a resource-name/path mismatch between the biome directory and its declared name.");

        return handle;
    }

    public synchronized short getBiomeIDFromBiomeName(String biomeName) {
        return getBiomeHandleFromBiomeName(biomeName).getBiomeID();
    }

    public synchronized BiomeHandle getBiomeHandleFromBiomeID(short biomeID) {

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