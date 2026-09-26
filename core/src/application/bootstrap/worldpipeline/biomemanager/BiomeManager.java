package application.bootstrap.worldpipeline.biomemanager;

import java.util.Arrays;
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
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class BiomeManager extends ManagerPackage {

    /*
     * Owns the biome palette and the biome field, the continuous function
     * giving each biome's share at any world position. The world map is
     * reconstructed through a warped kernel so painted borders blend, probable
     * variants appear as soft patches, and land meets ocean through its
     * declared beach. Reads are lock-free: registries are copy-on-write
     * snapshots and each worker memoizes map colors in its own scratch.
     */

    // Palette
    private final ConcurrentHashMap<String, BiomeHandle> biomeName2BiomeHandle = new ConcurrentHashMap<>();
    private volatile Short2ObjectOpenHashMap<BiomeHandle> biomeID2BiomeHandle = new Short2ObjectOpenHashMap<>();
    private final ConcurrentHashMap<String, String> variantName2ParentName = new ConcurrentHashMap<>();

    // Map Color Index
    private volatile ColorIndex colorIndex = ColorIndex.EMPTY;

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

        Short2ObjectOpenHashMap<BiomeHandle> nextID2BiomeHandle = new Short2ObjectOpenHashMap<>(biomeID2BiomeHandle);
        nextID2BiomeHandle.put(biomeHandle.getBiomeID(), biomeHandle);

        biomeName2BiomeHandle.put(biomeHandle.getBiomeName(), biomeHandle);
        biomeID2BiomeHandle = nextID2BiomeHandle;

        for (String variantName : biomeHandle.getProbableBiomeNames())
            linkVariant(variantName, biomeHandle.getBiomeName());
    }

    private void linkVariant(String variantName, String parentName) {

        String existingParentName = variantName2ParentName.putIfAbsent(variantName, parentName);

        if (existingParentName != null && !existingParentName.equals(parentName))
            throwException("Biome \"" + variantName + "\" is listed in \"probable_biomes\" by both \""
                    + existingParentName + "\" and \"" + parentName
                    + "\" — every variant belongs to exactly one parent biome.");
    }

    synchronized void registerMapColor(String biomeName, int mapColor) {

        ColorIndex current = colorIndex;
        int size = current.colors.length;

        int[] colors = Arrays.copyOf(current.colors, size + 1);
        String[] names = Arrays.copyOf(current.names, size + 1);

        colors[size] = mapColor;
        names[size] = biomeName;

        colorIndex = new ColorIndex(colors, names);
    }

    // On-Demand \\

    public void request(String biomeName) {
        ((BiomeLoader) internalLoader).request(biomeName);
    }

    // Biome Field \\

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
                    scratch,
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
        resolveShoreBuffers(outBlend);
    }

    private void resolveShoreBuffers(BiomeBlendStruct blend) {

        float oceanWeight = blend.getOceanWeight();

        if (oceanWeight <= 0f || oceanWeight >= 1f)
            return;

        float fraction = BiomeFieldUtility.computeShoreBufferFraction(oceanWeight);
        int count = blend.getCount();

        for (int i = 0; i < count; i++) {

            BiomeHandle biome = blend.getBiome(i);

            if (biome.hasOceanWater() || !biome.hasBeachBiome())
                continue;

            blend.convertToBuffer(i, getBiomeHandleFromBiomeName(biome.getBeachBiomeName()), fraction);
        }

        blend.normalize();
    }

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

    private BiomeHandle getBiomeHandleForColor(BiomeFieldAsyncContainer scratch, int color) {

        ColorIndex index = colorIndex;

        if (scratch.colorIndexStamp != index) {
            scratch.color2BiomeHandle.clear();
            scratch.colorIndexStamp = index;
        }

        BiomeHandle handle = scratch.color2BiomeHandle.get(color);

        if (handle == null) {
            handle = getBiomeHandleFromBiomeName(getNearestBiomeNameForColor(index, color));
            scratch.color2BiomeHandle.put(color, handle);
        }

        return handle;
    }

    private String getNearestBiomeNameForColor(ColorIndex index, int color) {

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

    public String getDisplayName(BiomeHandle biomeHandle) {

        if (biomeHandle.hasDisplayName())
            return biomeHandle.getDisplayName();

        String parentName = variantName2ParentName.get(biomeHandle.getBiomeName());

        if (parentName == null)
            throwException("Biome \"" + biomeHandle.getBiomeName() + "\" declares no \"display_name\" and no "
                    + "registered biome lists it in \"probable_biomes\" — "
                    + "an unnamed biome must be a linked variant.");

        return getDisplayName(getBiomeHandleFromBiomeName(parentName));
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