package application.bootstrap.worldpipeline.block;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.util.ChunkCoordinate3Int;
import engine.root.EngineSetting;
import engine.root.HandlePackage;
import engine.util.mathematics.extras.Coordinate3Int;
import it.unimi.dsi.fastutil.shorts.Short2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public final class BlockPaletteHandle extends HandlePackage {

    /*
     * Compressed block palette for a single sub-chunk region. Stores block IDs
     * using a bit-packed indirect palette that expands automatically as new
     * block types are introduced, backed by an O(1) reverse index so neither
     * writing a new value nor rebuilding the palette during a dump ever
     * degrades into a linear scan. Converts to a flat direct array once the
     * palette exceeds the configured threshold. releaseStorage() drops all
     * backing storage without forgetting the handle's own config, so a caller
     * that only needs this palette some of the time (see SubChunkInstance)
     * can construct it, release it, and construct it again cheaply. A block
     * palette constructed with a BlockManager is partitioned: it also owns one
     * child palette per geometry type, routes every write through them, and
     * answers type and liquid queries by dipping into the matching child.
     */

    // Palette Config
    private int chunkSize;
    private int blocksPerCell;
    private int paletteAxisSize;
    private int scaleBits;
    private int totalCells;
    private int maxPaletteSize;
    private short defaultBlockId;

    // Storage — packed palette mode
    private ShortArrayList palette;
    private Short2IntOpenHashMap paletteIndexLookup;
    private long[] packedData;
    private int bitsPerEntry;

    // Storage — direct mode (post-threshold)
    private short[] directData;

    // Children — realized only by a partitioned block palette
    private BlockManager blockManager;
    private BlockTypePaletteHandle fullPaletteHandle;
    private BlockTypePaletteHandle partialPaletteHandle;
    private BlockTypePaletteHandle complexPaletteHandle;
    private LiquidBlockPaletteHandle liquidPaletteHandle;
    private BlockTypePaletteHandle[] geometryType2TypePalette;

    // Construction \\

    public void constructor(int paletteAxisSize, int paletteThreshold, short defaultBlockId) {

        this.chunkSize = EngineSetting.CHUNK_SIZE;

        if (chunkSize % paletteAxisSize != 0)
            throwException("paletteAxisSize must evenly divide CHUNK_SIZE");

        this.blocksPerCell = chunkSize / paletteAxisSize;

        if ((blocksPerCell & (blocksPerCell - 1)) != 0)
            throwException("blocksPerCell must be power-of-two");

        this.paletteAxisSize = paletteAxisSize;
        this.scaleBits = Integer.numberOfTrailingZeros(blocksPerCell);
        this.totalCells = paletteAxisSize * paletteAxisSize * paletteAxisSize;
        this.maxPaletteSize = paletteThreshold;
        this.defaultBlockId = defaultBlockId;

        this.palette = new ShortArrayList();
        this.paletteIndexLookup = new Short2IntOpenHashMap();
        this.paletteIndexLookup.defaultReturnValue(-1);
        addToPalette(defaultBlockId);
        this.bitsPerEntry = 1;

        allocatePackedArray();
    }

    public void constructor(
            int paletteAxisSize,
            int paletteThreshold,
            short defaultBlockId,
            BlockManager blockManager) {

        constructor(paletteAxisSize, paletteThreshold, defaultBlockId);

        if (blocksPerCell != 1)
            throwException("A partitioned block palette must store exactly one block per cell");

        this.blockManager = blockManager;

        if (geometryType2TypePalette == null)
            createTypePalettes();

        for (BlockTypePaletteHandle typePalette : geometryType2TypePalette)
            if (typePalette != null)
                typePalette.constructor(totalCells);

        fillTypePalettes(defaultBlockId);
    }

    private void createTypePalettes() {

        this.fullPaletteHandle = create(BlockTypePaletteHandle.class);
        this.partialPaletteHandle = create(BlockTypePaletteHandle.class);
        this.complexPaletteHandle = create(BlockTypePaletteHandle.class);
        this.liquidPaletteHandle = create(LiquidBlockPaletteHandle.class);

        this.geometryType2TypePalette = new BlockTypePaletteHandle[DynamicGeometryType.LENGTH];
        this.geometryType2TypePalette[DynamicGeometryType.FULL.ordinal()] = fullPaletteHandle;
        this.geometryType2TypePalette[DynamicGeometryType.PARTIAL.ordinal()] = partialPaletteHandle;
        this.geometryType2TypePalette[DynamicGeometryType.COMPLEX.ordinal()] = complexPaletteHandle;
        this.geometryType2TypePalette[DynamicGeometryType.LIQUID.ordinal()] = liquidPaletteHandle;
    }

    public void clear() {
        fill(defaultBlockId);
    }

    /*
     * Resets every cell in this palette to a single value in O(1) — used
     * instead of looping setBlock() calls when an entire palette region is
     * known to share one value up front, such as a subchunk's biome
     * palette, which is always uniform across a single chunk column.
     */
    public void fill(short blockId) {

        palette.clear();
        paletteIndexLookup.clear();
        addToPalette(blockId);
        bitsPerEntry = 1;

        int longsNeeded = (totalCells + 63) >>> 6;
        if (packedData != null && packedData.length == longsNeeded)
            java.util.Arrays.fill(packedData, 0L);
        else
            packedData = new long[longsNeeded];

        directData = null;

        if (isPartitioned())
            fillTypePalettes(blockId);
    }

    /*
     * Drops all backing storage, leaving the handle's config (axis size,
     * threshold, default value) intact so constructor() can be called again
     * later to bring it back at full cost only when actually needed.
     */
    public void releaseStorage() {

        palette = null;
        paletteIndexLookup = null;
        packedData = null;
        directData = null;

        if (geometryType2TypePalette == null)
            return;

        for (BlockTypePaletteHandle typePalette : geometryType2TypePalette)
            if (typePalette != null)
                typePalette.releaseStorage();
    }

    // Internal \\

    private int addToPalette(short blockId) {
        int index = palette.size();
        palette.add(blockId);
        paletteIndexLookup.put(blockId, index);
        return index;
    }

    private void allocatePackedArray() {
        int longsNeeded = (totalCells * bitsPerEntry + 63) >>> 6;
        this.packedData = new long[longsNeeded];
    }

    private int calculateBitsNeeded(int paletteSize) {
        return Math.max(1, 32 - Integer.numberOfLeadingZeros(paletteSize - 1));
    }

    private int readPackedValue(int index) {

        int startBit = index * bitsPerEntry;
        int longIndex = startBit >>> 6;
        int bitOffset = startBit & 63;
        long mask = (1L << bitsPerEntry) - 1L;

        if (bitOffset + bitsPerEntry <= 64)
            return (int) ((packedData[longIndex] >>> bitOffset) & mask);

        int lowBits = 64 - bitOffset;
        long low = packedData[longIndex] >>> bitOffset;
        long high = packedData[longIndex + 1] & ((1L << (bitsPerEntry - lowBits)) - 1L);

        return (int) ((high << lowBits) | low);
    }

    private void writePackedValue(int index, int value) {

        int startBit = index * bitsPerEntry;
        int longIndex = startBit >>> 6;
        int bitOffset = startBit & 63;
        long mask = (1L << bitsPerEntry) - 1L;

        if (bitOffset + bitsPerEntry <= 64) {
            packedData[longIndex] = (packedData[longIndex] & ~(mask << bitOffset))
                    | ((long) value << bitOffset);
            return;
        }

        int lowBits = 64 - bitOffset;
        long lowMask = (1L << lowBits) - 1L;
        long highMask = (1L << (bitsPerEntry - lowBits)) - 1L;

        packedData[longIndex] = (packedData[longIndex] & ~(lowMask << bitOffset))
                | (((long) value & lowMask) << bitOffset);
        packedData[longIndex + 1] = (packedData[longIndex + 1] & ~highMask)
                | ((long) value >>> lowBits);
    }

    private void expandBits(int newBits) {

        long[] oldData = packedData;
        int oldBits = bitsPerEntry;

        bitsPerEntry = newBits;
        allocatePackedArray();

        for (int i = 0; i < totalCells; i++) {
            int value = readPackedValueFrom(oldData, oldBits, i);
            writePackedValue(i, value);
        }
    }

    private static int readPackedValueFrom(long[] data, int bits, int index) {

        int startBit = index * bits;
        int longIndex = startBit >>> 6;
        int bitOffset = startBit & 63;
        long mask = (1L << bits) - 1L;

        if (bitOffset + bits <= 64)
            return (int) ((data[longIndex] >>> bitOffset) & mask);

        int lowBits = 64 - bitOffset;
        long low = data[longIndex] >>> bitOffset;
        long high = data[longIndex + 1] & ((1L << (bits - lowBits)) - 1L);

        return (int) ((high << lowBits) | low);
    }

    private int getCellIndex(int packedXYZ) {
        int x = ((packedXYZ) & 0xF) >> scaleBits;
        int y = ((packedXYZ >> 20) & 0xF) >> scaleBits;
        int z = ((packedXYZ >> 10) & 0xF) >> scaleBits;
        return (y * paletteAxisSize + z) * paletteAxisSize + x;
    }

    private void convertToDirect() {

        directData = new short[totalCells];

        for (int i = 0; i < totalCells; i++)
            directData[i] = palette.getShort(readPackedValue(i));

        palette = null;
        paletteIndexLookup = null;
        packedData = null;
    }

    private void setBlockByIndex(int index, short blockId) {

        int paletteIndex = paletteIndexLookup.get(blockId);

        if (paletteIndex == -1) {
            paletteIndex = addToPalette(blockId);
            int neededBits = calculateBitsNeeded(palette.size());
            if (neededBits > bitsPerEntry)
                expandBits(neededBits);
        }

        writePackedValue(index, paletteIndex);
    }

    private short readBlock(int index) {
        return directData != null ? directData[index] : palette.getShort(readPackedValue(index));
    }

    private void writeBlock(int index, short blockId) {

        if (directData != null) {
            directData[index] = blockId;
            return;
        }

        int paletteIndex = paletteIndexLookup.get(blockId);

        if (paletteIndex == -1) {

            if (palette.size() >= maxPaletteSize) {
                convertToDirect();
                directData[index] = blockId;
                return;
            }

            paletteIndex = addToPalette(blockId);

            int neededBits = calculateBitsNeeded(palette.size());
            if (neededBits > bitsPerEntry)
                expandBits(neededBits);
        }

        writePackedValue(index, paletteIndex);
    }

    private void collapse() {

        ShortArrayList oldPalette = directData != null ? null : palette;
        long[] oldData = directData != null ? null : packedData;
        int oldBits = bitsPerEntry;
        short[] oldDirect = directData;

        palette = new ShortArrayList();
        paletteIndexLookup = new Short2IntOpenHashMap();
        paletteIndexLookup.defaultReturnValue(-1);
        addToPalette(defaultBlockId);
        bitsPerEntry = 1;
        allocatePackedArray();
        directData = null;

        for (int i = 0; i < totalCells; i++) {
            short block = oldDirect != null
                    ? oldDirect[i]
                    : oldPalette.getShort(readPackedValueFrom(oldData, oldBits, i));
            setBlockByIndex(i, block);
        }
    }

    // Management \\

    public void dumpInteriorBlocks(short airBlockId) {

        int[] interiorCoordinates = ChunkCoordinate3Int.getInteriorBlockCoordinates();

        if (isPartitioned())
            for (int packedXYZ : interiorCoordinates) {
                int index = getCellIndex(packedXYZ);
                routeTypePalettes(index, readBlock(index), airBlockId);
            }

        if (directData != null) {
            for (int packedXYZ : interiorCoordinates)
                directData[getCellIndex(packedXYZ)] = airBlockId;
        } else {

            int airPaletteIndex = paletteIndexLookup.get(airBlockId);

            if (airPaletteIndex == -1)
                airPaletteIndex = addToPalette(airBlockId);

            for (int packedXYZ : interiorCoordinates)
                writePackedValue(getCellIndex(packedXYZ), airPaletteIndex);
        }

        collapse();
    }

    // Type Palettes \\

    private boolean isPartitioned() {
        return blockManager != null;
    }

    private BlockTypePaletteHandle resolveTypePalette(short blockId) {
        return geometryType2TypePalette[blockManager.getGeometryFromBlockID(blockId).ordinal()];
    }

    private void fillTypePalettes(short blockId) {

        for (BlockTypePaletteHandle typePalette : geometryType2TypePalette)
            if (typePalette != null)
                typePalette.clear();

        BlockTypePaletteHandle typePalette = resolveTypePalette(blockId);

        if (typePalette != null)
            typePalette.fillAll();
    }

    private void routeTypePalettes(int index, short oldBlockId, short newBlockId) {

        if (oldBlockId == newBlockId)
            return;

        BlockTypePaletteHandle oldTypePalette = resolveTypePalette(oldBlockId);
        BlockTypePaletteHandle newTypePalette = resolveTypePalette(newBlockId);

        if (oldTypePalette != null)
            oldTypePalette.remove(index);

        if (newTypePalette != null)
            newTypePalette.add(index);
    }

    private BlockTypePaletteHandle requireTypePalette(DynamicGeometryType geometryType) {

        if (!isPartitioned())
            throwException("Block type queries require a palette constructed with a BlockManager");

        BlockTypePaletteHandle typePalette = geometryType2TypePalette[geometryType.ordinal()];

        if (typePalette == null)
            throwException("Geometry type " + geometryType + " has no child palette");

        return typePalette;
    }

    private LiquidBlockPaletteHandle requireLiquidPalette() {

        if (!isPartitioned())
            throwException("Liquid queries require a palette constructed with a BlockManager");

        return liquidPaletteHandle;
    }

    // Block Types \\

    public int getBlockTypeCount(DynamicGeometryType geometryType) {
        return requireTypePalette(geometryType).getCount();
    }

    public int nextBlockOfType(DynamicGeometryType geometryType, int fromIndex) {
        return requireTypePalette(geometryType).nextMember(fromIndex);
    }

    // Liquid \\

    public short getLiquidLevel(int packedXYZ) {
        return requireLiquidPalette().getLevel(getCellIndex(packedXYZ));
    }

    public void setLiquidLevel(int packedXYZ, short level) {
        requireLiquidPalette().setLevel(getCellIndex(packedXYZ), level);
    }

    public boolean isLiquidPermanent(int packedXYZ) {
        return requireLiquidPalette().isPermanent(getCellIndex(packedXYZ));
    }

    public void setLiquidPermanent(int packedXYZ, boolean permanent) {
        requireLiquidPalette().setPermanent(getCellIndex(packedXYZ), permanent);
    }

    public void activateLiquid(int packedXYZ) {
        requireLiquidPalette().activate(getCellIndex(packedXYZ));
    }

    public void deactivateLiquid(int packedXYZ) {
        requireLiquidPalette().deactivate(getCellIndex(packedXYZ));
    }

    public int getActiveLiquidCount() {
        return requireLiquidPalette().getActiveCount();
    }

    public int collectActiveLiquid(int[] target) {
        return requireLiquidPalette().collectActive(target);
    }

    // Accessible \\

    public short getBlock(int packedXYZ) {
        return readBlock(getCellIndex(packedXYZ));
    }

    public void setBlock(int packedXYZ, short blockId) {

        int index = getCellIndex(packedXYZ);

        if (isPartitioned())
            routeTypePalettes(index, readBlock(index), blockId);

        writeBlock(index, blockId);
    }

    public short getBlock(int x, int y, int z) {
        return getBlock(Coordinate3Int.pack(x, y, z));
    }

    public void setBlock(int x, int y, int z, short blockId) {
        setBlock(Coordinate3Int.pack(x, y, z), blockId);
    }
}