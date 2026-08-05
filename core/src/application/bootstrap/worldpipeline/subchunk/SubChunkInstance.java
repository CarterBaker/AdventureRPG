package application.bootstrap.worldpipeline.subchunk;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
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
     * One vertical slice of a chunk covering CHUNK_SIZE^3 blocks. Owns block,
     * biome, rotation, and liquid-level palettes plus a world item palette.
     * Permanently owned by its parent ChunkInstance — never pooled or
     * transferred independently. Dirty-region geometry rebuilds operate at
     * this granularity. Also tracks containedBlockTypes — the set of
     * DynamicGeometryTypes this subchunk's most recently built geometry
     * actually contains blocks of, tallied by GeometryBuildManager during the
     * same walk that already visits every block to build the mesh — so
     * systems that only care about one geometry type (liquid tides, liquid
     * flow ticking) can skip subchunks that never contain it without a
     * separate scan. Tally writes only ever happen while the owning chunk's
     * ChunkDataSyncContainer is held (see BuildBranch); readers off the
     * build thread must acquire that same lock first. liquidLevelPaletteHandle
     * mirrors blockPaletteHandle's resolution one-for-one and holds each
     * liquid cell's fill level (0..EngineSetting.LIQUID_LEVEL_MAX) — see
     * LiquidSimulationSystem, the sole writer of partial fill state.
     * liquidStable marks a subchunk whose liquid produced no movement on its
     * last flow step — LiquidTickBranch skips the palette scan entirely while
     * it's set, and clears it the instant anything writes new liquid state
     * into this subchunk from outside.
     */

    // Internal
    private BlockPaletteHandle biomePaletteHandle;
    private BlockPaletteHandle blockPaletteHandle;
    private BlockPaletteHandle blockRotationPaletteHandle;
    private BlockPaletteHandle liquidLevelPaletteHandle;
    private WorldItemPaletteHandle worldItemPaletteHandle;

    // Block Type Composition — tallied during geometry build
    private ReferenceOpenHashSet<DynamicGeometryType> containedBlockTypes;
    private int[] blockTypeCounts;

    // Liquid Flow — the exact liquid block IDs tallied into this subchunk on
    // its last geometry build (a subset of containedBlockTypes' LIQUID case),
    // how many real seconds have accumulated since its liquid geometry last
    // redrew, and whether the last flow step moved anything at all.
    private ShortOpenHashSet containedLiquidBlockIDs;
    private float liquidFlowAccumulator;
    private boolean liquidStable;

    // Internal \\

    @Override
    protected void create() {

        super.create();

        // Internal
        this.biomePaletteHandle = create(BlockPaletteHandle.class);
        this.blockPaletteHandle = create(BlockPaletteHandle.class);
        this.blockRotationPaletteHandle = create(BlockPaletteHandle.class);
        this.liquidLevelPaletteHandle = create(BlockPaletteHandle.class);
        this.worldItemPaletteHandle = create(WorldItemPaletteHandle.class);
        this.worldItemPaletteHandle.constructor();

        // Block Type Composition
        this.containedBlockTypes = new ReferenceOpenHashSet<>(DynamicGeometryType.LENGTH);
        this.blockTypeCounts = new int[DynamicGeometryType.LENGTH];

        // Liquid Flow
        this.containedLiquidBlockIDs = new ShortOpenHashSet();
        this.liquidFlowAccumulator = 0f;
        this.liquidStable = false;
    }

    // Constructor \\

    public void constructor(
            WorldRenderManager worldRenderManager,
            WorldHandle worldHandle,
            long coordinate,
            VAOHandle vaoHandle,
            short airBlockId,
            short defaultBiomeId) {

        super.constructor(
                worldRenderManager,
                worldHandle,
                RenderType.INVALID,
                coordinate,
                vaoHandle);

        this.biomePaletteHandle.constructor(
                EngineSetting.CHUNK_SIZE / EngineSetting.BIOME_SIZE,
                EngineSetting.BLOCK_PALETTE_THRESHOLD / EngineSetting.BIOME_SIZE,
                defaultBiomeId);

        this.blockPaletteHandle.constructor(
                EngineSetting.CHUNK_SIZE,
                EngineSetting.BLOCK_PALETTE_THRESHOLD,
                airBlockId);

        this.blockRotationPaletteHandle.constructor(
                EngineSetting.CHUNK_SIZE,
                EngineSetting.BLOCK_PALETTE_THRESHOLD,
                EngineSetting.DEFAULT_BLOCK_ORIENTATION);

        this.liquidLevelPaletteHandle.constructor(
                EngineSetting.CHUNK_SIZE,
                EngineSetting.BLOCK_PALETTE_THRESHOLD,
                EngineSetting.LIQUID_LEVEL_EMPTY);
    }

    // Reset \\

    public void reset() {
        biomePaletteHandle.clear();
        blockPaletteHandle.clear();
        blockRotationPaletteHandle.clear();
        liquidLevelPaletteHandle.clear();
        worldItemPaletteHandle.clear();
        getDynamicPacket().clear();
        containedBlockTypes.clear();
        Arrays.fill(blockTypeCounts, 0);
        containedLiquidBlockIDs.clear();
        liquidFlowAccumulator = 0f;
        liquidStable = false;
    }

    // Block Type Composition \\

    public void beginBlockTypeTally() {
        Arrays.fill(blockTypeCounts, 0);
        containedLiquidBlockIDs.clear();
    }

    public void tallyBlockType(DynamicGeometryType type) {
        blockTypeCounts[type.ordinal()]++;
    }

    public void tallyLiquidBlock(short blockID) {
        containedLiquidBlockIDs.add(blockID);
    }

    /*
     * Folds the tally into the exposed set. The set instance is never
     * reallocated — only cleared and repopulated — so repeated rebuilds
     * create no garbage.
     */
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

    public boolean isLiquidStable() {
        return liquidStable;
    }

    public void setLiquidStable(boolean liquidStable) {
        this.liquidStable = liquidStable;
    }

    // Block Writes \\

    /*
     * Every write to this subchunk's block or liquid-level palette must pass
     * through here instead of touching BlockPaletteHandle directly, so
     * liquidStable is always invalidated the instant something actually
     * changes — no external system, debug tooling included, is ever
     * responsible for remembering to clear it itself. invalidateLiquid() is
     * also called directly by systems that alter a NEIGHBORING subchunk in a
     * way that could open or close this subchunk's own flow paths (see
     * BlockBranch.rebuildSubChunk()).
     */

    public void invalidateLiquid() {
        liquidStable = false;
    }

    public void setBlock(int x, int y, int z, short blockID) {
        blockPaletteHandle.setBlock(x, y, z, blockID);
        invalidateLiquid();
    }

    public void setBlock(int packedXYZ, short blockID) {
        blockPaletteHandle.setBlock(packedXYZ, blockID);
        invalidateLiquid();
    }

    public void setLiquidLevel(int x, int y, int z, short level) {
        liquidLevelPaletteHandle.setBlock(x, y, z, level);
        invalidateLiquid();
    }

    public void setLiquidLevel(int packedXYZ, short level) {
        liquidLevelPaletteHandle.setBlock(packedXYZ, level);
        invalidateLiquid();
    }

    // Accessible \\

    public BlockPaletteHandle getBiomePaletteHandle() {
        return biomePaletteHandle;
    }

    public BlockPaletteHandle getBlockPaletteHandle() {
        return blockPaletteHandle;
    }

    public BlockPaletteHandle getBlockRotationPaletteHandle() {
        return blockRotationPaletteHandle;
    }

    public BlockPaletteHandle getLiquidLevelPaletteHandle() {
        return liquidLevelPaletteHandle;
    }

    public WorldItemPaletteHandle getWorldItemPaletteHandle() {
        return worldItemPaletteHandle;
    }

    public short getBlock(int x, int y, int z) {
        return blockPaletteHandle.getBlock(Coordinate3Int.pack(x, y, z));
    }
}