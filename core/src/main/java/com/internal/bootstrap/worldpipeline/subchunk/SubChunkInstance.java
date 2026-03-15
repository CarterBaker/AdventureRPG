package com.internal.bootstrap.worldpipeline.subchunk;

import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.worldpipeline.block.BlockPaletteHandle;
import com.internal.bootstrap.worldpipeline.world.WorldHandle;
import com.internal.bootstrap.worldpipeline.worlditem.WorldItemPaletteHandle;
import com.internal.bootstrap.worldpipeline.worldrendermanager.RenderType;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderInstance;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.extras.Coordinate3Int;

public class SubChunkInstance extends WorldRenderInstance {

    // Internal
    private BlockPaletteHandle biomePaletteHandle;
    private BlockPaletteHandle blockPaletteHandle;
    private BlockPaletteHandle blockRotationPaletteHandle;
    private WorldItemPaletteHandle worldItemPaletteHandle;

    // Internal \\

    @Override
    protected void create() {
        super.create();
        this.biomePaletteHandle = create(BlockPaletteHandle.class);
        this.blockPaletteHandle = create(BlockPaletteHandle.class);
        this.blockRotationPaletteHandle = create(BlockPaletteHandle.class);
        this.worldItemPaletteHandle = create(WorldItemPaletteHandle.class);
        this.worldItemPaletteHandle.constructor();
    }

    public void constructor(
            WorldRenderManager worldRenderSystem,
            WorldHandle worldHandle,
            long coordinate,
            VAOHandle vaoHandle,
            short airBlockId,
            short defaultBiomeId) {
        super.constructor(
                worldRenderSystem,
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
        short defaultOrientation = (short) (EngineSetting.DEFAULT_BLOCK_DIRECTION * 4);
        this.blockRotationPaletteHandle.constructor(
                EngineSetting.CHUNK_SIZE,
                EngineSetting.BLOCK_PALETTE_THRESHOLD,
                defaultOrientation);
    }

    public void reset() {
        biomePaletteHandle.clear();
        blockPaletteHandle.clear();
        blockRotationPaletteHandle.clear();
        worldItemPaletteHandle.clear();
        dynamicPacketInstance.clear();
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