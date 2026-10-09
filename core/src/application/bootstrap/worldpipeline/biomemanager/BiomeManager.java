package application.bootstrap.worldpipeline.biomemanager;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.ProbableBiomeStruct;
import application.bootstrap.worldpipeline.biome.ProbablePatchStruct;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.assets.image.Pixmap;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BiomeManager extends ManagerPackage {

    /*
     * Owns the biome palette and the biome field, the continuous function
     * giving each biome's share at any world position. The world map is
     * reconstructed through a warped kernel so painted borders blend, probable
     * biomes scatter as shaped patches or sit at the center of their parent's
     * patches, chaining their own probable biomes to any depth, and land
     * meets ocean through its declared beach. Biome IDs are assigned in
     * registration order. Reads are lock-free: registries are copy-on-write
     * snapshots and each worker memoizes map colors in its own scratch. Biomes
     * can be rebuilt or retired live from an edited ARPG tree; the revision
     * moves on every such change, so whatever derives from the field knows to
     * refresh.
     */

    // Palette
    private final ConcurrentHashMap<String, BiomeHandle> biomeName2BiomeHandle = new ConcurrentHashMap<>();
    private final Object2IntOpenHashMap<String> biomeName2BiomeID = RegistryUtility.createNameIndex();
    private volatile ObjectArrayList<BiomeHandle> biomeID2BiomeHandle = RegistryUtility.createPalette();
    private final ConcurrentHashMap<String, String> variantName2ParentName = new ConcurrentHashMap<>();

    // Map Color Index
    private volatile ColorIndex colorIndex = ColorIndex.EMPTY;

    // Internal
    private BiomeFieldAsyncContainer fieldContainer;
    private BiomeRebuildBranch biomeRebuildBranch;

    // Revision
    private volatile int revision;

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
        this.biomeRebuildBranch = create(BiomeRebuildBranch.class);
    }

    // Management \\

    // A name keeps its ID for the engine lifetime, so a rebuilt biome replaces the handle under the same ID
    synchronized short registerBiomeName(String biomeName) {

        int biomeID = biomeName2BiomeID.getInt(biomeName);

        if (biomeID != RegistryUtility.ID_NONE)
            return (short) biomeID;

        ObjectArrayList<BiomeHandle> nextID2BiomeHandle = new ObjectArrayList<>(biomeID2BiomeHandle);
        biomeID = RegistryUtility.registerID(
                biomeName2BiomeID, nextID2BiomeHandle, biomeName, EngineSetting.REGISTRY_SHORT_ID_COUNT);
        biomeID2BiomeHandle = nextID2BiomeHandle;

        return (short) biomeID;
    }

    synchronized void addBiome(BiomeHandle biomeHandle) {

        ObjectArrayList<BiomeHandle> nextID2BiomeHandle = new ObjectArrayList<>(biomeID2BiomeHandle);
        nextID2BiomeHandle.set(biomeHandle.getBiomeID(), biomeHandle);

        biomeName2BiomeHandle.put(biomeHandle.getBiomeName(), biomeHandle);
        biomeID2BiomeHandle = nextID2BiomeHandle;

        for (ProbableBiomeStruct probableBiome : biomeHandle.getProbableBiomes())
            linkVariant(probableBiome.getBiomeName(), biomeHandle.getBiomeName());
    }

    private void linkVariant(String variantName, String parentName) {

        if (isChainedBeneath(parentName, variantName))
            throwException("Biome \"" + parentName + "\" lists \"" + variantName + "\" in \"probable_biomes\", "
                    + "but \"" + parentName + "\" is already chained beneath \"" + variantName
                    + "\" — a probable biome chain cannot loop back on itself.");

        String existingParentName = variantName2ParentName.putIfAbsent(variantName, parentName);

        if (existingParentName != null && !existingParentName.equals(parentName))
            throwException("Biome \"" + variantName + "\" is listed in \"probable_biomes\" by both \""
                    + existingParentName + "\" and \"" + parentName
                    + "\" — every variant belongs to exactly one parent biome.");
    }

    // Replaces the color a biome already paints with, or appends it
    synchronized void registerMapColor(String biomeName, int mapColor) {

        ColorIndex current = colorIndex;
        int index = indexOfMapColor(current, biomeName);

        if (index != EngineSetting.INDEX_NOT_FOUND) {

            int[] colors = current.colors.clone();
            colors[index] = mapColor;
            colorIndex = new ColorIndex(colors, current.names);
            return;
        }

        int size = current.colors.length;
        int[] colors = Arrays.copyOf(current.colors, size + 1);
        String[] names = Arrays.copyOf(current.names, size + 1);

        colors[size] = mapColor;
        names[size] = biomeName;

        colorIndex = new ColorIndex(colors, names);
    }

    private synchronized void unregisterMapColor(String biomeName) {

        ColorIndex current = colorIndex;
        int index = indexOfMapColor(current, biomeName);

        if (index == EngineSetting.INDEX_NOT_FOUND)
            return;

        int size = current.colors.length - 1;
        int[] colors = new int[size];
        String[] names = new String[size];

        System.arraycopy(current.colors, 0, colors, 0, index);
        System.arraycopy(current.names, 0, names, 0, index);
        System.arraycopy(current.colors, index + 1, colors, index, size - index);
        System.arraycopy(current.names, index + 1, names, index, size - index);

        colorIndex = new ColorIndex(colors, names);
    }

    private int indexOfMapColor(ColorIndex index, String biomeName) {

        for (int i = 0; i < index.names.length; i++)
            if (index.names[i].equals(biomeName))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Live Edit \

    // Throws a catchable InternalException, leaving the biome as it was, when the tree cannot go live
    public synchronized void rebuildBiome(String biomeName, ArpgObjectStruct biomeArpg) {

        BiomeHandle biomeHandle = biomeRebuildBranch.build(biomeName, biomeArpg);
        BiomeHandle previous = biomeName2BiomeHandle.get(biomeName);

        if (previous != null)
            for (ProbableBiomeStruct probableBiome : previous.getProbableBiomes())
                variantName2ParentName.remove(probableBiome.getBiomeName(), biomeName);

        addBiome(biomeHandle);

        if (biomeHandle.hasMapColor())
            registerMapColor(biomeName, biomeHandle.getMapColor());
        else
            unregisterMapColor(biomeName);

        revision++;
    }

    // A retired biome stays registered for what was generated with it, but no longer paints the world map
    public synchronized void retireBiome(String biomeName) {

        if (indexOfMapColor(colorIndex, biomeName) == EngineSetting.INDEX_NOT_FOUND)
            return;

        if (colorIndex.names.length == 1)
            throw new InternalException("Biome \"" + biomeName
                    + "\" is the last biome painted on the world map and cannot be retired.");

        unregisterMapColor(biomeName);
        revision++;
    }

    boolean isLastPaintedBiome(String biomeName) {

        ColorIndex index = colorIndex;

        return index.names.length == 1 && index.names[0].equals(biomeName);
    }

    String getVariantParentName(String variantName) {
        return variantName2ParentName.get(variantName);
    }

    // True when the biome is the descendant itself or anywhere up its chain of parents
    boolean isChainedBeneath(String descendantName, String biomeName) {

        String currentName = descendantName;

        while (currentName != null) {

            if (currentName.equals(biomeName))
                return true;

            currentName = variantName2ParentName.get(currentName);
        }

        return false;
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

        int mapBiomeCount = collectMapBiomes(scratch, map);

        scratch.probableSeed = seed ^ EngineSetting.BIOME_PROBABLE_SEED;
        scratch.probablePixelX = pixelX;
        scratch.probablePixelZ = pixelZ;
        scratch.mapWidth = map.getWidth();
        scratch.mapHeight = map.getHeight();

        for (int i = 0; i < mapBiomeCount; i++)
            accumulateChain(
                    scratch, scratch.mapBiomes[i], scratch.mapBiomeWeights[i],
                    0L, ProbablePatchStruct.NO_PATCH_SHAPE, 0f, outBlend);

        outBlend.normalize();
        resolveShoreBuffers(outBlend);
    }

    // Merges map samples that resolve to the same biome, so each chain is walked once per position
    private int collectMapBiomes(BiomeFieldAsyncContainer scratch, Pixmap map) {

        int count = 0;

        for (int i = 0; i < BiomeFieldUtility.MAP_SAMPLE_COUNT; i++) {

            float mapWeight = scratch.mapWeights[i];

            if (mapWeight <= 0f)
                continue;

            BiomeHandle mapBiome = getBiomeHandleForColor(
                    scratch,
                    map.getPixelRGB(scratch.mapPixelX[i], scratch.mapPixelZ[i]));

            int index = 0;

            while (index < count && scratch.mapBiomes[index] != mapBiome)
                index++;

            if (index == count) {
                scratch.mapBiomes[count] = mapBiome;
                scratch.mapBiomeWeights[count] = 0f;
                count++;
            }

            scratch.mapBiomeWeights[index] += mapWeight;
        }

        return count;
    }

    // Each probable biome claims what it covers of the share earlier entries left, then walks its own chain
    private void accumulateChain(
            BiomeFieldAsyncContainer scratch,
            BiomeHandle biome,
            float weight,
            long hostHash,
            float hostShape,
            float hostNoise,
            BiomeBlendStruct outBlend) {

        ObjectArrayList<ProbableBiomeStruct> probableBiomes = biome.getProbableBiomes();
        ProbablePatchStruct patch = scratch.probablePatch;
        float remaining = 1f;

        for (int i = 0; i < probableBiomes.size() && remaining > 0f; i++) {

            ProbableBiomeStruct probableBiome = probableBiomes.get(i);

            if (probableBiome.isCentered())
                BiomeFieldUtility.computeCenterCoverage(probableBiome, hostHash, hostShape, hostNoise, patch);
            else
                BiomeFieldUtility.computeScatterCoverage(
                        scratch.probableSeed, probableBiome,
                        scratch.probablePixelX, scratch.probablePixelZ,
                        scratch.mapWidth, scratch.mapHeight, patch);

            float coverage = patch.getCoverage();

            if (coverage <= 0f)
                continue;

            float share = remaining * coverage;
            remaining -= share;

            accumulateChain(
                    scratch, getBiomeHandleFromBiomeName(probableBiome.getBiomeName()), weight * share,
                    patch.getPatchHash(), patch.getPatchShape(), patch.getPatchNoise(), outBlend);
        }

        outBlend.accumulate(biome, weight * remaining);
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

    public int getRevision() {
        return revision;
    }

    public void collectPaintedBiomes(ObjectArrayList<String> outNames, IntArrayList outColors) {

        ColorIndex index = colorIndex;

        outNames.clear();
        outColors.clear();

        for (int i = 0; i < index.names.length; i++) {
            outNames.add(index.names[i]);
            outColors.add(index.colors[i]);
        }
    }

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

    public int getMapColor(BiomeHandle biomeHandle) {

        if (biomeHandle.hasMapColor())
            return biomeHandle.getMapColor();

        String parentName = variantName2ParentName.get(biomeHandle.getBiomeName());

        if (parentName == null)
            return EngineSetting.BIOME_MAP_COLOR_UNDEFINED;

        return getMapColor(getBiomeHandleFromBiomeName(parentName));
    }

    public short getBiomeIDFromBiomeName(String biomeName) {
        return getBiomeHandleFromBiomeName(biomeName).getBiomeID();
    }

    public BiomeHandle getBiomeHandleFromBiomeID(short biomeID) {

        if (biomeID == EngineSetting.REGISTRY_RESERVED_ID)
            throwException("Biome ID 0 was queried — this is the reserved \"not yet generated\" sentinel a "
                    + "subchunk's biome palette holds until world generation actually runs on it. The caller "
                    + "read biome data before ChunkData.GENERATION_DATA was set for this chunk.");

        BiomeHandle handle = RegistryUtility.getHandle(biomeID2BiomeHandle, biomeID);

        if (handle == null)
            throwException("No handle registered for biome ID: " + biomeID);

        return handle;
    }
}