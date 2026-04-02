package program.bootstrap.geometrypipeline.dynamicgeometrymanager;

import java.util.BitSet;
import program.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import program.bootstrap.worldpipeline.biome.BiomeHandle;
import program.bootstrap.worldpipeline.block.BlockHandle;
import program.bootstrap.worldpipeline.block.BlockPaletteHandle;
import program.bootstrap.worldpipeline.chunk.ChunkInstance;
import program.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import program.core.engine.BranchPackage;
import program.core.util.mathematics.extras.Color;
import program.core.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

class LiquidGeometryBranch extends BranchPackage {

    /*
     * Geometry branch for liquid blocks. Handles surface-only face assembly
     * with liquid-specific blending rules. Stub implementation, to be filled out.
     */

    // Build \\

    boolean assembleQuads(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            BlockPaletteHandle rotationPaletteHandle,
            DynamicPacketInstance dynamicPacketInstance,
            int xyz,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            Int2ObjectOpenHashMap<FloatArrayList> verts,
            BitSet accumulatedBatch,
            BitSet batchReturn,
            Color[] vertColors) {
        return false;
    }
}