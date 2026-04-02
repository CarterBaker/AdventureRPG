package program.bootstrap.worldpipeline.subchunk;

import program.bootstrap.geometrypipeline.vao.VAOHandle;
import program.bootstrap.worldpipeline.block.BlockPaletteHandle;
import program.bootstrap.worldpipeline.world.WorldHandle;
import program.bootstrap.worldpipeline.worlditem.WorldItemPaletteHandle;
import program.bootstrap.worldpipeline.worldrendermanager.RenderType;
import program.bootstrap.worldpipeline.worldrendermanager.WorldRenderInstance;
import program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import program.core.settings.EngineSetting;
import program.core.util.mathematics.extras.Coordinate3Int;

public class SubChunkInstance extends WorldRenderInstance {

    /*
     * One vertical slice of a chunk covering CHUNK_SIZE^3 blocks. Owns block,
     * biome, and rotation palettes plus a world item palette. Permanently owned
     * by its parent ChunkInstance — never pooled or transferred independently.
     * Dirty-region geometry rebuilds operate at this granularity.
     */

    // Internal
    private BlockPaletteHandle biomePaletteHandle;
    private BlockPaletteHandle blockPaletteHandle;
    private BlockPaletteHandle blockRotationPaletteHandle;
    private WorldItemPaletteHandle worldItemPaletteHandle;

    // Internal \\

    @Override
    protected void create() {

        super.create();

        // Internal
        this.biomePaletteHandle = create(BlockPaletteHandle.class);
        this.blockPaletteHandle = create(BlockPaletteHandle.class);
        this.blockRotationPaletteHandle = create(BlockPaletteHandle.class);
        this.worldItemPaletteHandle = create(WorldItemPaletteHandle.class);
        this.worldItemPaletteHandle.constructor();
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
    }

    // Reset \\

    public void reset() {
        biomePaletteHandle.clear();
        blockPaletteHandle.clear();
        blockRotationPaletteHandle.clear();
        worldItemPaletteHandle.clear();
        getDynamicPacket().clear();
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

    public WorldItemPaletteHandle getWorldItemPaletteHandle() {
        return worldItemPaletteHandle;
    }

    public short getBlock(int x, int y, int z) {
        return blockPaletteHandle.getBlock(Coordinate3Int.pack(x, y, z));
    }
}