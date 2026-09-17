package application.bootstrap.worldpipeline.biomemanager;

import java.util.concurrent.ConcurrentHashMap;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.assets.image.Pixmap;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BiomeManager extends ManagerPackage {

    /*
     * Owns the biome palette, the indexes that resolve a name, ID, or world
     * PNG pixel color to a biome, and the biome field itself — the
     * continuous, position-addressed function that answers "how much of each
     * biome is present here" for any world position. The field is what world
     * generation shapes terrain against: it treats the PNG as a suggestion
     * reconstructed through a warped, band-limited kernel rather than as a
     * per-chunk lookup, so a border between two painted regions resolves as
     * a gradient of both biomes' authored values instead of a hard switch,
     * and a biome's probable variants appear as soft-edged patches inside
     * it. Every read path is lock-free: both registries and the color
     * resolution memo are ConcurrentHashMaps, and the map-color index is an
     * immutable snapshot published through a volatile reference. Only the
     * rare mutation paths take a lock, and that lock never blocks a reader.
     */

    // Palette
    private final ConcurrentHashMap<String, BiomeHandle> biomeName2BiomeHandle = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Short, BiomeHandle> biomeID2BiomeHandle = new ConcurrentHashMap<>();

    // Map Color Index — an immutable snapshot swapped in on every
    // registration, so getNearestBiomeNameForColor() never locks against it.
    private volatile ColorIndex colorIndex = ColorIndex.EMPTY;

    // Memo of the nearest-color search, which the field would otherwise
    // repeat for every map sample of every column of every chunk.
    private final ConcurrentHashMap<Integer, BiomeHandle> color2BiomeHandle = new ConcurrentHashMap<>();

    // Internal
    private BiomeFieldAsyncContainer fieldContainer;

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
        this.fieldContainer = create(BiomeFieldAsyncContainer.class);
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

        color2BiomeHandle.clear();
    }

    // On-Demand \\

    public void request(String biomeName) {
        ((BiomeLoader) internalLoader).request(biomeName);
    }

    // Biome Field \\

    /*
     * Resolves the biome influence present at one world position into the
     * caller's blend, normalized to sum to 1.0. The position is converted to
     * continuous map-pixel space, domain-warped so painted borders bend and
     * roughen, reconstructed against the four surrounding pixels with a
     * smooth band-limited kernel, and — for any contributing biome that
     * declares probable variants — split across the patch cells reaching
     * that position. Nothing in the path is aligned to the chunk grid, so
     * two chunks evaluating a shared position produce identical weights and
     * terrain crosses a chunk boundary without a seam.
     */
    public void sampleBiomeField(WorldHandle worldHandle, double worldX, double worldZ, BiomeBlendStruct outBlend) {

        outBlend.reset();

        BiomeFieldAsyncContainer scratch = fieldContainer.getInstance();

        Pixmap map = worldHandle.getWorld();
        long seed = worldHandle.getSeed();

        double blocksPerPixel = EngineSetting.CHUNKS_PER_PIXEL * (double) EngineSetting.CHUNK_SIZE;
        double pixelX = worldX / blocksPerPixel;
        double pixelZ = worldZ / blocksPerPixel;

        double warpedPixelX = pixelX + BiomeFieldUtility.computeBorderWarpOffset(
                seed ^ EngineSetting.BIOME_BORDER_WARP_SEED, pixelX, pixelZ);

        double warpedPixelZ = pixelZ + BiomeFieldUtility.computeBorderWarpOffset(
                seed ^ EngineSetting.BIOME_BORDER_WARP_SEED ^ EngineSetting.HASH_FINALIZER_MULTIPLIER_1,
                pixelX, pixelZ);

        BiomeFieldUtility.computeMapSamples(
                warpedPixelX, warpedPixelZ, map.getWidth(), map.getHeight(),
                scratch.mapPixelX, scratch.mapPixelZ, scratch.mapWeights);

        int patchCount = BiomeFieldUtility.computePatchSamples(
                seed ^ EngineSetting.BIOME_PATCH_SEED, warpedPixelX, warpedPixelZ,
                map.getWidth(), map.getHeight(), scratch.patchCellHash, scratch.patchWeights);

        for (int i = 0; i < BiomeFieldUtility.MAP_SAMPLE_COUNT; i++) {

            float mapWeight = scratch.mapWeights[i];

            if (mapWeight <= 0f)
                continue;

            BiomeHandle mapBiome = getBiomeHandleForColor(
                    map.getPixelRGB(scratch.mapPixelX[i], scratch.mapPixelZ[i]));

            if (patchCount == 0 || mapBiome.getProbableBiomeNames().isEmpty()) {
                outBlend.accumulate(mapBiome, mapWeight);
                continue;
            }

            for (int patch = 0; patch < patchCount; patch++)
                outBlend.accumulate(
                        resolveProbableBiome(mapBiome, scratch.patchCellHash[patch]),
                        mapWeight * scratch.patchWeights[patch]);
        }

        outBlend.normalize();
    }

    /*
     * Which variant a single patch cell rolled for this base biome. The roll
     * is hashed against the base biome's own ID as well as the cell, so two
     * biomes sharing the patch lattice do not place their variants in
     * lockstep. Chances are validated at load time to sum to no more than
     * 1.0, and whatever remains is the base biome keeping the cell.
     */
    private BiomeHandle resolveProbableBiome(BiomeHandle baseBiome, long cellHash) {

        ObjectArrayList<String> probableNames = baseBiome.getProbableBiomeNames();
        FloatArrayList probableChances = baseBiome.getProbableBiomeChances();

        float roll = BiomeFieldUtility.hash01(
                cellHash ^ (baseBiome.getBiomeID() * EngineSetting.HASH_FINALIZER_MULTIPLIER_2));

        float cumulative = 0f;

        for (int i = 0; i < probableNames.size(); i++) {

            cumulative += probableChances.getFloat(i);

            if (roll < cumulative)
                return getBiomeHandleFromBiomeName(probableNames.get(i));
        }

        return baseBiome;
    }

    // World Map Resolution \\

    /*
     * Chunk-granularity biome identity, for consumers that key off one biome
     * per chunk rather than shaping terrain — weather, primarily. This is the
     * dominant biome of the field sampled at the chunk's center block, so it
     * always agrees with the field the terrain under it was built from.
     */
    public BiomeHandle getBiome(WorldHandle worldHandle, long chunkCoordinate) {

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        BiomeFieldAsyncContainer scratch = fieldContainer.getInstance();

        double centerWorldX = (double) chunkX * EngineSetting.CHUNK_SIZE + EngineSetting.CHUNK_SIZE * 0.5;
        double centerWorldZ = (double) chunkZ * EngineSetting.CHUNK_SIZE + EngineSetting.CHUNK_SIZE * 0.5;

        sampleBiomeField(worldHandle, centerWorldX, centerWorldZ, scratch.queryBlend);

        return scratch.queryBlend.getDominantBiome();
    }

    public short getBiomeIDFromChunkCoordinate(WorldHandle worldHandle, long chunkCoordinate) {
        return getBiome(worldHandle, chunkCoordinate).getBiomeID();
    }

    private BiomeHandle getBiomeHandleForColor(int color) {
        return color2BiomeHandle.computeIfAbsent(
                color, key -> getBiomeHandleFromBiomeName(getNearestBiomeNameForColor(key)));
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