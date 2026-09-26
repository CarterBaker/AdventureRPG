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
     * One vertical slice of a chunk covering CHUNK_SIZE^3 blocks. A subchunk
     * carries real per-block palette storage (biome, block, rotation) only
     * once something genuinely needs cell-by-cell data — pure air
     * (knownEmpty) and single-block-type regions (uniformFill) are tracked as
     * scalars and never allocate a palette at all. Storage is realized on
     * first real need: a block edit, or a geometry build that finds the
     * subchunk not fully enclosed by identical neighbors and so must walk it
     * cell by cell. Since most subchunks in a tall world are either open sky
     * or buried deep underground, this keeps memory and generation cost
     * proportional to the terrain surface rather than to total world volume,
     * and releases storage back to virtual on reset() so a pooled chunk
     * reused at a new location starts free again. containedBlockTypes is the
     * set of DynamicGeometryTypes this subchunk's most recent geometry build
     * (or uniform-fill classification) actually contains, tallied so systems
     * like liquid ticking can skip subchunks that never need them. Tally
     * writes only ever happen while the owning chunk's ChunkDataSyncContainer
     * is held; readers off the build thread must acquire that lock first.
     * Liquid state lives per cell in the block palette's liquid child, so a
     * subchunk is liquid-stable exactly when none of its liquid cells are
     * active, and value reads answer for a virtual subchunk without ever
     * realizing storage. A cell subdivided into sub-blocks keeps its material
     * as its block and its octants in the block palette's partial child, and
     * setSubBlocks() is the single write path that subdivides, collapses, or
     * clears one.
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

    // Opaque Interior Fast Path — set only by WorldGenerationManager when a
    // subchunk's entire volume resolved to solid FULL-geometry blocks with
    // zero air or liquid, even though the exact block IDs are not uniform
    // (e.g. a stone subchunk shot through with ore, or a buried dirt/stone
    // transition band that never breaks the surface anywhere in its
    // footprint). Two adjacent FULL blocks never expose a face regardless of
    // their exact IDs, so GeometryBuildManager treats this exactly like
    // isUniformFill() when deciding whether a fully-enclosed subchunk can
    // skip geometry entirely — without releasing the real per-block storage
    // those varying IDs still require for mining and other block queries.
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

    /*
     * Realizes real per-block storage on first genuine need, backfilling it
     * to whatever this subchunk currently virtually represents (air, or a
     * single uniform block) so the caller that triggered this doesn't see a
     * spurious reset. Idempotent.
     */
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

    /*
     * Called once by WorldGenerationManager before each generation pass.
     * Drops any previously realized storage — this instance may be pooled
     * and reused for a different location — and records the column's biome
     * as a scalar; a palette is only ever built once something needs one.
     */
    public void beginGeneration(short columnBiomeID) {
        releaseStorageIfPopulated();
        this.columnBiomeID = columnBiomeID;
        this.knownEmpty = false;
        this.uniformFill = false;
        this.opaqueInterior = false;
    }

    /*
     * Hollows the interior of a subchunk that has real storage, dropping it
     * toward a shell. A subchunk that never realized storage is already
     * maximally compact — nothing to hollow — so its uniformFill
     * classification is left intact and it stays free.
     */
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

    /*
     * A subchunk uniformly filled with one liquid is a single contiguous body
     * spanning its entire volume, so it reads as permanent and settled
     * without ever realizing storage — deep open water never costs a tick of
     * simulation unless a direct edit breaks its uniformity. Only ocean
     * generation ever produces one, so it also reads as tidal.
     */
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

    /*
     * Called by world generation immediately after the real per-block loop
     * finishes, only when every cell it just wrote (or left at default air)
     * turned out to share one block ID — the common outcome for a subchunk
     * whose whole-chunk-footprint bounds were too rough to prove it air or
     * uniform fill in advance, but which is still buried entirely under one
     * material once actually resolved. Drops straight back to whichever
     * scalar fast path matches and frees the palette storage that was only
     * ever needed to discover this, so the subchunk gets identical treatment
     * — skipped by geometry building, no lingering memory — to one that was
     * classified uniform before a single block was ever written.
     */
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

    /*
     * Every write realizes storage first, so an edit to a single cell of a
     * uniform or empty subchunk correctly backfills the other 4095 cells to
     * their prior value before applying.
     */

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

    /*
     * The single write path for a cell's sub-blocks. The mask names which
     * octants of blockID are present: every octant writes the whole block,
     * none writes air, and anything between subdivides the cell.
     */
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

    /*
     * The single write path for ocean water. Places the liquid if the cell
     * does not already hold it and marks it permanent and tidal at the given
     * level, without waking anything — the tide owns this cell's level, not
     * the flow simulation, which only takes it over once an edit wakes it.
     */
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

    /*
     * Value-only reads never realize storage — a virtual subchunk answers
     * directly from its scalar state, exactly as if a real palette had been
     * filled uniformly.
     */
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