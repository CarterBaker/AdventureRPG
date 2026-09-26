package application.bootstrap.worldpipeline.subchunk;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worlditem.WorldItemPaletteHandle;
import application.bootstrap.worldpipeline.worldrendermanager.RenderType;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderInstance;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate3Int;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

public class SubChunkInstance extends WorldRenderInstance {

    /*
     * One vertical slice of a chunk. Pure air and single-block regions stay
     * virtual; per-block palettes are only realized when an edit or a geometry
     * build needs them, and released again on reset(). Tracks the geometry
     * types its last build contained, and setSubBlocks() is the single write
     * path for sub-block cells. Writes happen under the chunk's lock.
     */

    // Internal
    private BlockPaletteHandle biomePaletteHandle;
    private BlockPaletteHandle blockPaletteHandle;
    private BlockPaletteHandle blockRotationPaletteHandle;
    private WorldItemPaletteHandle worldItemPaletteHandle;
    private BlockManager blockManager;

    // Storage — lazily realized
    private boolean populated;
    private short airBlockId;
    private short columnBiomeID;

    // Block Type Composition — tallied during geometry build
    private ReferenceOpenHashSet<DynamicGeometryType> containedBlockTypes;
    private int[] blockTypeCounts;

    // Liquid Flow
    private ShortOpenHashSet containedLiquidBlockIDs;
    private float liquidFlowAccumulator;

    // Empty Fast Path — set only by WorldGenerationManager when a subchunk
    // is proven to hold no blocks at all
    private boolean knownEmpty;

    // Uniform Fill Fast Path — set only by WorldGenerationManager when a
    // subchunk's entire volume is a single geometry type and block ID
    private boolean uniformFill;
    private DynamicGeometryType uniformGeometryType;
    private short uniformBlockID;

    // Opaque Interior — every cell a FULL block with no air or liquid, whatever the IDs
    private boolean opaqueInterior;

    // Internal \\

    @Override
    protected void create() {

        super.create();

        this.biomePaletteHandle = create(BlockPaletteHandle.class);
        this.blockPaletteHandle = create(BlockPaletteHandle.class);
        this.blockRotationPaletteHandle = create(BlockPaletteHandle.class);
        this.worldItemPaletteHandle = create(WorldItemPaletteHandle.class);
        this.worldItemPaletteHandle.constructor();

        this.populated = false;

        this.containedBlockTypes = new ReferenceOpenHashSet<>(DynamicGeometryType.LENGTH);
        this.blockTypeCounts = new int[DynamicGeometryType.LENGTH];

        this.containedLiquidBlockIDs = new ShortOpenHashSet();
        this.liquidFlowAccumulator = 0f;

        this.knownEmpty = false;
        this.uniformFill = false;
    }

    // Constructor \\

    public void constructor(
            WorldRenderManager worldRenderManager,
            WorldHandle worldHandle,
            long coordinate,
            VAOHandle vaoHandle,
            short airBlockId,
            BlockManager blockManager) {

        super.constructor(
                worldRenderManager,
                worldHandle,
                RenderType.INVALID,
                coordinate,
                vaoHandle);

        this.airBlockId = airBlockId;
        this.blockManager = blockManager;
        this.columnBiomeID = EngineSetting.REGISTRY_RESERVED_ID;
        this.populated = false;
        this.knownEmpty = false;
        this.uniformFill = false;
    }

    // Reset \\

    public void reset() {

        releaseStorageIfPopulated();

        worldItemPaletteHandle.clear();
        getDynamicPacket().clear();
        containedBlockTypes.clear();
        Arrays.fill(blockTypeCounts, 0);
        containedLiquidBlockIDs.clear();
        liquidFlowAccumulator = 0f;
        knownEmpty = false;
        uniformFill = false;
        opaqueInterior = false;
    }

    // Lazy Storage \\

    private void ensurePopulated() {

        if (populated)
            return;

        biomePaletteHandle.constructor(
                EngineSetting.CHUNK_SIZE / EngineSetting.BIOME_SIZE,
                EngineSetting.BLOCK_PALETTE_THRESHOLD / EngineSetting.BIOME_SIZE,
                EngineSetting.REGISTRY_RESERVED_ID);
        biomePaletteHandle.fill(columnBiomeID);

        blockPaletteHandle.constructor(
                EngineSetting.CHUNK_SIZE, EngineSetting.BLOCK_PALETTE_THRESHOLD, airBlockId, blockManager);
        blockRotationPaletteHandle.constructor(
                EngineSetting.CHUNK_SIZE, EngineSetting.BLOCK_PALETTE_THRESHOLD,
                EngineSetting.DEFAULT_BLOCK_ORIENTATION);

        if (uniformFill)
            blockPaletteHandle.fill(uniformBlockID);

        populated = true;
    }

    private void releaseStorageIfPopulated() {

        if (!populated)
            return;

        biomePaletteHandle.releaseStorage();
        blockPaletteHandle.releaseStorage();
        blockRotationPaletteHandle.releaseStorage();

        populated = false;
    }

    public boolean isPopulated() {
        return populated;
    }

    // Generation \\

    public void beginGeneration(short columnBiomeID) {
        releaseStorageIfPopulated();
        this.columnBiomeID = columnBiomeID;
        this.knownEmpty = false;
        this.uniformFill = false;
        this.opaqueInterior = false;
    }

    public void dumpInteriorToAir() {

        if (!populated)
            return;

        blockPaletteHandle.dumpInteriorBlocks(airBlockId);
        clearUniformFill();
        opaqueInterior = false;
    }

    // Block Type Composition \\

    public void beginBlockTypeTally() {
        Arrays.fill(blockTypeCounts, 0);
        containedLiquidBlockIDs.clear();
    }

    public void tallyBlockType(DynamicGeometryType type) {
        blockTypeCounts[type.ordinal()]++;
    }

    public void tallyBlockType(DynamicGeometryType type, int count) {
        blockTypeCounts[type.ordinal()] += count;
    }

    public void tallyLiquidBlock(short blockID) {
        containedLiquidBlockIDs.add(blockID);
    }

    public void finalizeBlockTypeTally() {
        containedBlockTypes.clear();
        for (DynamicGeometryType type : DynamicGeometryType.VALUES)
            if (blockTypeCounts[type.ordinal()] > 0)
                containedBlockTypes.add(type);
    }

    public boolean hasBlockType(DynamicGeometryType type) {
        return blockTypeCounts[type.ordinal()] > 0;
    }

    public ReferenceOpenHashSet<DynamicGeometryType> getContainedBlockTypes() {
        return containedBlockTypes;
    }

    public ShortOpenHashSet getContainedLiquidBlockIDs() {
        return containedLiquidBlockIDs;
    }

    // Liquid Flow \\

    public void addLiquidFlowTime(float delta) {
        liquidFlowAccumulator += delta;
    }

    public float getLiquidFlowAccumulator() {
        return liquidFlowAccumulator;
    }

    public void resetLiquidFlowAccumulator() {
        liquidFlowAccumulator = 0f;
    }

    public boolean hasActiveLiquid() {
        return populated && blockPaletteHandle.getActiveLiquidCount() > 0;
    }

    public boolean isLiquidStable() {
        return !hasActiveLiquid();
    }

    // Empty Fast Path \\

    public void markKnownEmpty() {
        knownEmpty = true;
    }

    public boolean isKnownEmpty() {
        return knownEmpty;
    }

    // Uniform Fill Fast Path \\

    public void markUniformFill(DynamicGeometryType geometryType, short blockID) {
        this.uniformFill = true;
        this.uniformGeometryType = geometryType;
        this.uniformBlockID = blockID;
    }

    public void clearUniformFill() {
        this.uniformFill = false;
    }

    public boolean isUniformFill() {
        return uniformFill;
    }

    public DynamicGeometryType getUniformGeometryType() {
        return uniformGeometryType;
    }

    public short getUniformBlockID() {
        return uniformBlockID;
    }

    public void collapseGeneratedUniform(short resultBlockID, DynamicGeometryType resultGeometryType) {
        if (resultBlockID == airBlockId)
            markKnownEmpty();
        else
            markUniformFill(resultGeometryType, resultBlockID);
        releaseStorageIfPopulated();
    }

    // Opaque Interior Fast Path \\

    public void markOpaqueInterior() {
        this.opaqueInterior = true;
    }

    public boolean isOpaqueInterior() {
        return opaqueInterior;
    }

    // Block Writes \\

    public void setBlock(int x, int y, int z, short blockID) {
        setBlock(Coordinate3Int.pack(x, y, z), blockID);
    }

    public void setBlock(int packedXYZ, short blockID) {
        ensurePopulated();
        blockPaletteHandle.setBlock(packedXYZ, blockID);
        knownEmpty = false;
        uniformFill = false;
        opaqueInterior = false;
    }

    public void setSubBlocks(int packedXYZ, short blockID, int mask) {

        if (mask == SubBlockUtility.MASK_EMPTY) {
            setBlock(packedXYZ, airBlockId);
            return;
        }

        if (mask == SubBlockUtility.MASK_FULL) {
            setBlock(packedXYZ, blockID);
            return;
        }

        ensurePopulated();

        if (blockPaletteHandle.getBlock(packedXYZ) != blockID)
            blockPaletteHandle.setBlock(packedXYZ, blockID);

        blockPaletteHandle.setSubBlockMask(packedXYZ, mask);
        knownEmpty = false;
        uniformFill = false;
        opaqueInterior = false;
    }

    public void setLiquidLevel(int x, int y, int z, short level) {
        setLiquidLevel(Coordinate3Int.pack(x, y, z), level);
    }

    public void setLiquidLevel(int packedXYZ, short level) {
        ensurePopulated();
        blockPaletteHandle.setLiquidLevel(packedXYZ, level);
    }

    public void setLiquidPermanent(int packedXYZ, boolean permanent) {
        ensurePopulated();
        blockPaletteHandle.setLiquidPermanent(packedXYZ, permanent);
    }

    public void writeTidalLiquid(int packedXYZ, short liquidBlockID, short level) {

        ensurePopulated();

        if (blockPaletteHandle.getBlock(packedXYZ) != liquidBlockID)
            blockPaletteHandle.setBlock(packedXYZ, liquidBlockID);

        knownEmpty = false;
        uniformFill = false;
        opaqueInterior = false;

        blockPaletteHandle.setLiquidLevel(packedXYZ, level);
        blockPaletteHandle.setLiquidPermanent(packedXYZ, true);
        blockPaletteHandle.setLiquidTidal(packedXYZ, true);
    }

    // Liquid Activity \\

    public void activateLiquid(int packedXYZ) {

        if (!isLiquid(getBlock(packedXYZ)))
            return;

        ensurePopulated();
        blockPaletteHandle.activateLiquid(packedXYZ);
    }

    public void deactivateLiquid(int packedXYZ) {

        if (!populated)
            return;

        blockPaletteHandle.deactivateLiquid(packedXYZ);
    }

    public int collectActiveLiquid(int[] target) {

        if (!populated)
            return 0;

        return blockPaletteHandle.collectActiveLiquid(target);
    }

    private boolean isLiquid(short blockID) {
        return blockManager.getGeometryFromBlockID(blockID) == DynamicGeometryType.LIQUID;
    }

    // Accessible \\

    public BlockPaletteHandle getBiomePaletteHandle() {
        ensurePopulated();
        return biomePaletteHandle;
    }

    public BlockPaletteHandle getBlockPaletteHandle() {
        ensurePopulated();
        return blockPaletteHandle;
    }

    public BlockPaletteHandle getBlockRotationPaletteHandle() {
        ensurePopulated();
        return blockRotationPaletteHandle;
    }

    public WorldItemPaletteHandle getWorldItemPaletteHandle() {
        return worldItemPaletteHandle;
    }

    public short getBlock(int x, int y, int z) {
        return getBlock(Coordinate3Int.pack(x, y, z));
    }

    public short getBlock(int packedXYZ) {
        if (!populated)
            return uniformFill ? uniformBlockID : airBlockId;
        return blockPaletteHandle.getBlock(packedXYZ);
    }

    public int getSubBlockMask(int x, int y, int z) {
        return getSubBlockMask(Coordinate3Int.pack(x, y, z));
    }

    public int getSubBlockMask(int packedXYZ) {
        if (!populated)
            return SubBlockUtility.MASK_FULL;
        return blockPaletteHandle.getSubBlockMask(packedXYZ);
    }

    public short getLiquidLevel(int x, int y, int z) {
        return getLiquidLevel(Coordinate3Int.pack(x, y, z));
    }

    public short getLiquidLevel(int packedXYZ) {
        if (!populated)
            return isUniformLiquid()
                    ? EngineSetting.LIQUID_LEVEL_MAX
                    : EngineSetting.LIQUID_LEVEL_EMPTY;
        return blockPaletteHandle.getLiquidLevel(packedXYZ);
    }

    public boolean isLiquidPermanent(int packedXYZ) {
        if (!populated)
            return isUniformLiquid();
        return blockPaletteHandle.isLiquidPermanent(packedXYZ);
    }

    public boolean isLiquidTidal(int packedXYZ) {
        if (!populated)
            return isUniformLiquid();
        return blockPaletteHandle.isLiquidTidal(packedXYZ);
    }

    private boolean isUniformLiquid() {
        return uniformFill && uniformGeometryType == DynamicGeometryType.LIQUID;
    }
}