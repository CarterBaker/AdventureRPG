package com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.dynamicgeometry;

import java.util.BitSet;

import com.internal.bootstrap.worldpipeline.biome.BiomeHandle;
import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.core.engine.BranchPackage;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class LiquidGeometryBranch extends BranchPackage {

    public FloatArrayList assembleQuads(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            short xyz,
            BitSet batchedBlocks) {

        FloatArrayList quads = new FloatArrayList();

        // build geometry into quads

        return quads;
    }
}
