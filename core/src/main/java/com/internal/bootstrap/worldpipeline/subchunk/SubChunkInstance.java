package com.internal.bootstrap.worldpipeline.subchunk;

import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.bootstrap.worldpipeline.block.BlockPaletteHandle;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderInstance;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderSystem;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate3Int;
import com.internal.core.util.mathematics.Extras.Direction3Vector;

public class SubChunkInstance extends WorldRenderInstance {

    // Internal
    private BlockPaletteHandle biomePaletteHandle;
    private BlockPaletteHandle blockPaletteHandle;
    private BlockPaletteHandle blockRotationPaletteHandle;

    private short defaultDirection = (short) Direction3Vector.UP.index;

    // Internal \\

    @Override
    protected void create() {

        super.create();

        // Internal
        this.biomePaletteHandle = create(BlockPaletteHandle.class);
        this.blockPaletteHandle = create(BlockPaletteHandle.class);
        this.blockRotationPaletteHandle = create(BlockPaletteHandle.class);
    }

    public void constructor(
            WorldRenderSystem worldRenderSystem,
            WorldHandle worldHandle,
            long coordinate,
            VAOHandle vaoHandle,
            short airBlockId) {

        super.constructor(
                worldRenderSystem,
                worldHandle,
                coordinate,
                vaoHandle);

        this.biomePaletteHandle.constructor(
                EngineSetting.CHUNK_SIZE / EngineSetting.BIOME_SIZE,
                EngineSetting.BLOCK_PALETTE_THRESHOLD / EngineSetting.BIOME_SIZE,
                airBlockId);

        this.blockPaletteHandle.constructor(
                EngineSetting.CHUNK_SIZE,
                EngineSetting.BLOCK_PALETTE_THRESHOLD,
                airBlockId);

        this.blockRotationPaletteHandle.constructor(
                EngineSetting.CHUNK_SIZE,
                EngineSetting.BLOCK_PALETTE_THRESHOLD,
                defaultDirection);
    }

    // Accessible \\

    // Internal

    public BlockPaletteHandle getBiomePaletteHandle() {
        return biomePaletteHandle;
    }

    public BlockPaletteHandle getBlockPaletteHandle() {
        return blockPaletteHandle;
    }

    public BlockPaletteHandle getBlockRotationPaletteHandle() {
        return blockRotationPaletteHandle;
    }

    public short getBlock(int x, int y, int z) {
        return blockPaletteHandle.getBlock(Coordinate3Int.pack(x, y, z));
    }
}
