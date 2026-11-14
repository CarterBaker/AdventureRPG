package application.bootstrap.shaderpipeline.texturemanager;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import application.bootstrap.shaderpipeline.texture.TextureMapType;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgUtility;
import engine.util.memory.BufferUtility;

class TexturePoolBranch extends BranchPackage {

    /*
     * Owns the GPU side of tile streaming: one block-compressed array per tier
     * and map, sized once from the texture quality setting and never grown,
     * the fallback arrays holding every tile at the fallback size, the small
     * extra set, and the integer lookup a shader turns a tile ID into its
     * layers with. Slot N of a tier holds the same tile in every map's array,
     * and layer 0 of every array is the sentinel no tile takes. Tile data is
     * copied into a staging buffer and uploaded from it in one batch per
     * flush, so the CPU never waits on the GPU. Startup checks the context can
     * hold every pool and fails with the limit it cannot meet.
     */

    // GL
    private int[][] tierArrays;
    private int[] fallbackArrays;
    private int extraArray;
    private int lookupTexture;
    private int[] stagingBuffers;
    private int stagingIndex;

    // Quality
    private int quality;
    private float anisotropyMax;
    private float anisotropy;

    // Slots
    private int[] tierSlotCounts;
    private int[][] slotTiles;
    private int[][] freeSlots;
    private int[] freeSlotCounts;

    // Tiles
    private int[][] tileSlots;
    private int[] tileFallbackLayers;
    private int[] tileExtraLayers;
    private int fallbackLayerCount;
    private int extraLayerCount;

    // Lookup
    private int lookupHeight;
    private IntBuffer lookupWords;
    private int dirtyRowLow;
    private int dirtyRowHigh;

    // Staging
    private ByteBuffer staging;
    private int[] uploadArrays;
    private int[] uploadFormats;
    private int[] uploadSizes;
    private int[] uploadLayers;
    private int[] uploadOffsets;
    private int uploadCount;

    // Base \\

    @Override
    protected void create() {

        int tileCount = EngineSetting.TEXTURE_TILE_CAPACITY + EngineSetting.TEXTURE_SENTINEL_LAYERS;
        int tierCount = EngineSetting.TEXTURE_TIER_COUNT;

        // Slots
        this.tierSlotCounts = new int[tierCount];
        this.slotTiles = new int[tierCount][];
        this.freeSlots = new int[tierCount][];
        this.freeSlotCounts = new int[tierCount];

        for (int tier = 0; tier < tierCount; tier++) {
            int layers = EngineSetting.TEXTURE_TIER_SLOTS[tier] + EngineSetting.TEXTURE_SENTINEL_LAYERS;
            this.slotTiles[tier] = new int[layers];
            this.freeSlots[tier] = new int[layers];
        }

        // Tiles
        this.tileSlots = new int[tierCount][tileCount];
        this.tileFallbackLayers = new int[tileCount];
        this.tileExtraLayers = new int[tileCount];
        this.fallbackLayerCount = EngineSetting.TEXTURE_SENTINEL_LAYERS;
        this.extraLayerCount = EngineSetting.TEXTURE_SENTINEL_LAYERS;

        // Lookup
        this.lookupHeight = (tileCount + EngineSetting.TEXTURE_LOOKUP_WIDTH - 1) / EngineSetting.TEXTURE_LOOKUP_WIDTH;
        this.lookupWords = BufferUtility.newIntBuffer(
                lookupHeight * EngineSetting.TEXTURE_LOOKUP_WIDTH * EngineSetting.TEXTURE_LOOKUP_WORDS);
        this.dirtyRowLow = Integer.MAX_VALUE;
        this.dirtyRowHigh = EngineSetting.INDEX_NOT_FOUND;

        // Staging
        int uploads = EngineSetting.TEXTURE_STREAM_MAX_UPLOADS_PER_FLUSH;
        this.staging = BufferUtility.newByteBuffer(EngineSetting.TEXTURE_STREAM_UPLOAD_BYTES_PER_FRAME);
        this.uploadArrays = new int[uploads];
        this.uploadFormats = new int[uploads];
        this.uploadSizes = new int[uploads];
        this.uploadLayers = new int[uploads];
        this.uploadOffsets = new int[uploads];

        verifyContext();
        createArrays();
    }

    @Override
    protected void dispose() {

        for (int tier = 0; tier < EngineSetting.TEXTURE_TIER_COUNT; tier++)
            for (int m = 0; m < TextureMapType.TIERED_COUNT; m++)
                TextureGLSLUtility.deleteTextureArray(tierArrays[tier][m]);

        for (int m = 0; m < TextureMapType.TIERED_COUNT; m++)
            TextureGLSLUtility.deleteTextureArray(fallbackArrays[m]);

        TextureGLSLUtility.deleteTextureArray(extraArray);
        TextureGLSLUtility.deleteTexture2D(lookupTexture);

        for (int i = 0; i < stagingBuffers.length; i++)
            TextureGLSLUtility.deleteStagingBuffer(stagingBuffers[i]);
    }

    // Context \\

    // Every pool must fit the context: compressed formats, layer counts and the fragment stage's texture units
    private void verifyContext() {

        int major = TextureGLSLUtility.getInteger(EngineSetting.GL_MAJOR_VERSION);
        int minor = TextureGLSLUtility.getInteger(EngineSetting.GL_MINOR_VERSION);
        boolean bptcCore = major > EngineSetting.TEXTURE_BPTC_CORE_MAJOR
                || (major == EngineSetting.TEXTURE_BPTC_CORE_MAJOR && minor >= EngineSetting.TEXTURE_BPTC_CORE_MINOR);

        if (!bptcCore && !TextureGLSLUtility.hasExtension(EngineSetting.GL_EXTENSION_TEXTURE_COMPRESSION_BPTC))
            throwException("The graphics driver supports neither OpenGL " + EngineSetting.TEXTURE_BPTC_CORE_MAJOR
                    + "." + EngineSetting.TEXTURE_BPTC_CORE_MINOR + " nor "
                    + EngineSetting.GL_EXTENSION_TEXTURE_COMPRESSION_BPTC
                    + ", which the BC7 textures need. Update the graphics driver.");

        int layersNeeded = Math.max(EngineSetting.TEXTURE_TILE_CAPACITY, EngineSetting.TEXTURE_EXTRA_SLOTS);

        for (int tier = 0; tier < EngineSetting.TEXTURE_TIER_COUNT; tier++)
            layersNeeded = Math.max(layersNeeded, EngineSetting.TEXTURE_TIER_SLOTS[tier]);

        layersNeeded += EngineSetting.TEXTURE_SENTINEL_LAYERS;
        int layersAllowed = TextureGLSLUtility.getInteger(EngineSetting.GL_MAX_ARRAY_TEXTURE_LAYERS);

        if (layersAllowed < layersNeeded)
            throwException("The texture pools need " + layersNeeded + " array layers, but the graphics driver allows "
                    + layersAllowed + " (GL_MAX_ARRAY_TEXTURE_LAYERS).");

        int unitsNeeded = EngineSetting.TEXTURE_SAMPLER_COUNT + EngineSetting.TEXTURE_SHADER_SAMPLER_HEADROOM;
        int unitsAllowed = TextureGLSLUtility.getInteger(EngineSetting.GL_MAX_TEXTURE_IMAGE_UNITS);

        if (unitsAllowed < unitsNeeded)
            throwException("The texture pools need " + unitsNeeded + " fragment texture units, but the graphics "
                    + "driver allows " + unitsAllowed + " (GL_MAX_TEXTURE_IMAGE_UNITS).");

        boolean anisotropic = TextureGLSLUtility.hasExtension(EngineSetting.GL_EXTENSION_TEXTURE_FILTER_ANISOTROPIC)
                || TextureGLSLUtility.hasExtension(EngineSetting.GL_EXTENSION_TEXTURE_FILTER_ANISOTROPIC_ARB);

        this.anisotropyMax = anisotropic
                ? TextureGLSLUtility.getFloat(EngineSetting.GL_MAX_TEXTURE_MAX_ANISOTROPY)
                : 1f;
    }

    private void createArrays() {

        this.quality = clampQuality(settings.textureQuality);
        this.anisotropy = clampAnisotropy(settings.textureAnisotropy);
        this.tierArrays = new int[EngineSetting.TEXTURE_TIER_COUNT][TextureMapType.TIERED_COUNT];
        this.fallbackArrays = new int[TextureMapType.TIERED_COUNT];

        for (int tier = 0; tier < EngineSetting.TEXTURE_TIER_COUNT; tier++)
            for (int m = 0; m < TextureMapType.TIERED_COUNT; m++)
                tierArrays[tier][m] = TextureGLSLUtility.generateTexture();

        for (int m = 0; m < TextureMapType.TIERED_COUNT; m++) {
            fallbackArrays[m] = TextureGLSLUtility.generateTexture();
            TextureGLSLUtility.allocateCompressedArray(fallbackArrays[m], TextureMapType.VALUES[m].getGlFormat(),
                    EngineSetting.TEXTURE_FALLBACK_SIZE,
                    EngineSetting.TEXTURE_TILE_CAPACITY + EngineSetting.TEXTURE_SENTINEL_LAYERS, anisotropy);
        }

        this.extraArray = TextureGLSLUtility.generateTexture();
        TextureGLSLUtility.allocateCompressedArray(extraArray, TextureMapType.EXTRA.getGlFormat(),
                EngineSetting.TEXTURE_EXTRA_SIZE,
                EngineSetting.TEXTURE_EXTRA_SLOTS + EngineSetting.TEXTURE_SENTINEL_LAYERS, anisotropy);

        this.lookupTexture = TextureGLSLUtility.createLookupTexture(
                EngineSetting.TEXTURE_LOOKUP_WIDTH, lookupHeight, lookupWords);

        this.stagingBuffers = new int[EngineSetting.TEXTURE_STREAM_STAGING_BUFFERS];

        for (int i = 0; i < stagingBuffers.length; i++)
            stagingBuffers[i] = TextureGLSLUtility.generateStagingBuffer();

        allocateTiers();
    }

    // Every tier's arrays sized for the quality: a tier it leaves out keeps only its sentinel layer
    private void allocateTiers() {

        for (int tier = 0; tier < EngineSetting.TEXTURE_TIER_COUNT; tier++) {

            int slots = tier <= quality ? EngineSetting.TEXTURE_TIER_SLOTS[tier] : 0;
            tierSlotCounts[tier] = slots;
            freeSlotCounts[tier] = slots;

            for (int s = 0; s < slots; s++)
                freeSlots[tier][s] = slots - s;

            for (int m = 0; m < TextureMapType.TIERED_COUNT; m++)
                TextureGLSLUtility.allocateCompressedArray(tierArrays[tier][m],
                        TextureMapType.VALUES[m].getGlFormat(), EngineSetting.TEXTURE_TIER_SIZES[tier],
                        slots + EngineSetting.TEXTURE_SENTINEL_LAYERS, anisotropy);
        }
    }

    private int clampQuality(int value) {
        return Math.max(EngineSetting.TEXTURE_QUALITY_LOW, Math.min(EngineSetting.TEXTURE_QUALITY_HIGH, value));
    }

    private float clampAnisotropy(int value) {
        return Math.max(1f, Math.min(anisotropyMax, value));
    }

    // Quality \\

    // Reallocates every tier for a new quality; every tile loses its tier slots and streams in again
    void applyQuality(int value) {

        uploadCount = 0;
        staging.clear();
        this.quality = clampQuality(value);

        for (int tier = 0; tier < EngineSetting.TEXTURE_TIER_COUNT; tier++) {

            for (int s = 0; s < slotTiles[tier].length; s++) {

                int tileID = slotTiles[tier][s];

                if (tileID != 0) {
                    tileSlots[tier][tileID] = 0;
                    writeLookup(tileID);
                }

                slotTiles[tier][s] = 0;
            }
        }

        allocateTiers();
    }

    void applyAnisotropy(int value) {

        this.anisotropy = clampAnisotropy(value);

        for (int tier = 0; tier < EngineSetting.TEXTURE_TIER_COUNT; tier++)
            for (int m = 0; m < TextureMapType.TIERED_COUNT; m++)
                TextureGLSLUtility.setAnisotropy(tierArrays[tier][m], anisotropy);

        for (int m = 0; m < TextureMapType.TIERED_COUNT; m++)
            TextureGLSLUtility.setAnisotropy(fallbackArrays[m], anisotropy);

        TextureGLSLUtility.setAnisotropy(extraArray, anisotropy);
    }

    int getQuality() {
        return quality;
    }

    float getAnisotropy() {
        return anisotropy;
    }

    // Slots \\

    int getSlotCount(int tier) {
        return tierSlotCounts[tier];
    }

    int getFreeSlotCount(int tier) {
        return freeSlotCounts[tier];
    }

    int getSlotTile(int tier, int slot) {
        return slotTiles[tier][slot];
    }

    int getTileSlot(int tier, int tileID) {
        return tileSlots[tier][tileID];
    }

    // A free slot of the tier taken for the tile, 0 when none is free
    int placeTile(int tier, int tileID) {

        if (freeSlotCounts[tier] == 0)
            return 0;

        int slot = freeSlots[tier][--freeSlotCounts[tier]];
        slotTiles[tier][slot] = tileID;
        tileSlots[tier][tileID] = slot;
        writeLookup(tileID);

        return slot;
    }

    void removeTile(int tier, int tileID) {

        int slot = tileSlots[tier][tileID];

        if (slot == 0)
            return;

        slotTiles[tier][slot] = 0;
        tileSlots[tier][tileID] = 0;
        freeSlots[tier][freeSlotCounts[tier]++] = slot;
        writeLookup(tileID);
    }

    // Fallback & Extra \\

    int assignFallbackLayer(int tileID) {

        if (fallbackLayerCount > EngineSetting.TEXTURE_TILE_CAPACITY)
            throwException("More than " + EngineSetting.TEXTURE_TILE_CAPACITY
                    + " texture tiles are defined; the fallback pool holds no more.");

        int layer = fallbackLayerCount++;
        tileFallbackLayers[tileID] = layer;
        writeLookup(tileID);

        return layer;
    }

    int assignExtraLayer(int tileID) {

        if (extraLayerCount > EngineSetting.TEXTURE_EXTRA_SLOTS)
            throwException("More than " + EngineSetting.TEXTURE_EXTRA_SLOTS
                    + " texture tiles carry growth, roughness, tint or emissive maps; the extra set holds no more.");

        int layer = extraLayerCount++;
        tileExtraLayers[tileID] = layer;
        writeLookup(tileID);

        return layer;
    }

    int getFallbackLayer(int tileID) {
        return tileFallbackLayers[tileID];
    }

    int getExtraLayer(int tileID) {
        return tileExtraLayers[tileID];
    }

    // Staging \\

    void stageTier(int tier, int slot, byte[][] maps) {
        for (int m = 0; m < TextureMapType.TIERED_COUNT; m++)
            stageChain(tierArrays[tier][m], TextureMapType.VALUES[m], EngineSetting.TEXTURE_TIER_SIZES[tier], slot,
                    maps[m]);
    }

    void stageFallback(int layer, byte[][] maps) {
        for (int m = 0; m < TextureMapType.TIERED_COUNT; m++)
            stageChain(fallbackArrays[m], TextureMapType.VALUES[m], EngineSetting.TEXTURE_FALLBACK_SIZE, layer,
                    maps[m]);
    }

    void stageExtra(int layer, byte[] chain) {
        stageChain(extraArray, TextureMapType.EXTRA, EngineSetting.TEXTURE_EXTRA_SIZE, layer, chain);
    }

    private void stageChain(int array, TextureMapType mapType, int size, int layer, byte[] chain) {

        if (chain.length != TextureCompressionUtility.getChainBytes(size))
            throwException("A baked " + ArpgUtility.toEnumName(mapType) + " chain holds " + chain.length
                    + " bytes where a " + size + " chain needs " + TextureCompressionUtility.getChainBytes(size));

        if (staging.remaining() < chain.length || uploadCount == uploadArrays.length)
            flush();

        uploadArrays[uploadCount] = array;
        uploadFormats[uploadCount] = mapType.getGlFormat();
        uploadSizes[uploadCount] = size;
        uploadLayers[uploadCount] = layer;
        uploadOffsets[uploadCount] = staging.position();
        uploadCount++;
        staging.put(chain);
    }

    // Uploads everything staged from the next staging buffer in turn, returning the bytes sent
    int flush() {

        if (uploadCount == 0)
            return 0;

        int bytes = staging.position();
        staging.flip();
        TextureGLSLUtility.bindStaged(stagingBuffers[stagingIndex], staging.capacity(), staging);

        for (int i = 0; i < uploadCount; i++)
            TextureGLSLUtility.uploadCompressedLayer(uploadArrays[i], uploadFormats[i], uploadSizes[i],
                    uploadLayers[i], uploadOffsets[i]);

        TextureGLSLUtility.unbindStaged();
        staging.clear();
        uploadCount = 0;
        stagingIndex = (stagingIndex + 1) % stagingBuffers.length;

        return bytes;
    }

    // Lookup \\

    // The tile's two lookup words: base and middle tier slots, then top tier slot, extra layer and fallback layer
    private void writeLookup(int tileID) {

        int first = tileSlots[0][tileID] | (tileSlots[1][tileID] << EngineSetting.TEXTURE_LOOKUP_WIDE_BITS);
        int second = tileSlots[2][tileID]
                | (tileExtraLayers[tileID] << EngineSetting.TEXTURE_LOOKUP_NARROW_BITS)
                | (tileFallbackLayers[tileID] << EngineSetting.TEXTURE_LOOKUP_WIDE_BITS);
        int index = tileID * EngineSetting.TEXTURE_LOOKUP_WORDS;
        int row = tileID / EngineSetting.TEXTURE_LOOKUP_WIDTH;

        lookupWords.put(index, first);
        lookupWords.put(index + 1, second);
        dirtyRowLow = Math.min(dirtyRowLow, row);
        dirtyRowHigh = Math.max(dirtyRowHigh, row);
    }

    // Sends every lookup row changed since the last push
    void pushLookup() {

        if (dirtyRowHigh < dirtyRowLow)
            return;

        int rowWords = EngineSetting.TEXTURE_LOOKUP_WIDTH * EngineSetting.TEXTURE_LOOKUP_WORDS;
        lookupWords.limit((dirtyRowHigh + 1) * rowWords);
        lookupWords.position(dirtyRowLow * rowWords);
        TextureGLSLUtility.updateLookupRows(lookupTexture, dirtyRowLow, dirtyRowHigh - dirtyRowLow + 1,
                EngineSetting.TEXTURE_LOOKUP_WIDTH, lookupWords);
        lookupWords.clear();

        dirtyRowLow = Integer.MAX_VALUE;
        dirtyRowHigh = EngineSetting.INDEX_NOT_FOUND;
    }

    // Samplers \\

    // The GL texture a tile sampler of the given name reads, GL_HANDLE_NONE for any other name
    int getSamplerHandle(String uniformName) {

        if (uniformName.equals(EngineSetting.UNIFORM_TILE_SLOTS))
            return lookupTexture;

        if (uniformName.equals(EngineSetting.UNIFORM_TILE_EXTRA))
            return extraArray;

        for (int m = 0; m < TextureMapType.TIERED_COUNT; m++) {

            if (uniformName.equals(EngineSetting.TEXTURE_FALLBACK_SAMPLER_NAMES[m]))
                return fallbackArrays[m];

            for (int tier = 0; tier < EngineSetting.TEXTURE_TIER_COUNT; tier++)
                if (uniformName.equals(EngineSetting.TEXTURE_TIER_SAMPLER_NAMES[tier][m]))
                    return tierArrays[tier][m];
        }

        return EngineSetting.GL_HANDLE_NONE;
    }
}
