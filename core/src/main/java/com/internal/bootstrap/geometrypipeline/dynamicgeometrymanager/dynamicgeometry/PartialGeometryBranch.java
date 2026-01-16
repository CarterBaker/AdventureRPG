package com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.dynamicgeometry;

import java.util.BitSet;

import com.internal.bootstrap.worldpipeline.biome.BiomeHandle;
import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.core.engine.BranchPackage;
import com.internal.core.util.mathematics.Extras.Direction3Vector;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class PartialGeometryBranch extends BranchPackage {

    public Boolean assembleQuads(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            short xyz,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            FloatArrayList quads,
            BitSet batchReturn) {

        // build geometry into quads

        return false;
    }
}
