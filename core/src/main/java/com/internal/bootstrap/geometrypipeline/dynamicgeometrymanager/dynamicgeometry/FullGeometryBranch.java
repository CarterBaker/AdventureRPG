package com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.dynamicgeometry;

import java.util.BitSet;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometry;
import com.internal.bootstrap.worldpipeline.biome.BiomeHandle;
import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkNeighborStruct;
import com.internal.bootstrap.worldpipeline.subchunk.BlockPaletteHandle;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate3Short;
import com.internal.core.util.mathematics.Extras.Direction2Vector;
import com.internal.core.util.mathematics.Extras.Direction3Vector;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class FullGeometryBranch extends BranchPackage {

    // Internal
    private BlockManager blockManager;

    // Data
    private static final SubChunkInstance ERROR = new SubChunkInstance();

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.blockManager = get(BlockManager.class);
    }

    // Full Geometry Builder \\

    public Boolean assembleQuads(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            short xyz,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            FloatArrayList quads,
            BitSet batchReturn) {

        if (!blockHasFace(
                chunkInstance,
                subChunkInstance,
                xyz,
                direction3Vector,
                blockHandle))
            return false;

        return false; // TODO: Temporary
    }

    private boolean blockHasFace(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            short xyz,
            Direction3Vector direction3Vector,
            BlockHandle blockHandle) {

        SubChunkInstance comparativeSubChunkCoordinate = getComparativeSubChunkInstance(
                chunkInstance,
                subChunkInstance,
                xyz,
                direction3Vector);
        if (comparativeSubChunkCoordinate == ERROR)
            return false;

        short comparativeXYZ = Coordinate3Short.getNeighbor(xyz, direction3Vector);

        BlockPaletteHandle comparativeBlockPaletteHandle = comparativeSubChunkCoordinate.getBlockPaletteHandle();
        short comparativeBlockID = comparativeBlockPaletteHandle.getBlock(comparativeXYZ);
        BlockHandle comparativeBlockHandle = blockManager.getBlockFromBlockID(comparativeBlockID);

        if (comparativeBlockHandle.getGeometry() == DynamicGeometry.FULL)
            return false;

        return true;
    }

    private SubChunkInstance getComparativeSubChunkInstance(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            short xyz,
            Direction3Vector direction3Vector) {

        // If the coordinate is within the sa,e
        if (!Coordinate3Short.isAtEdge(xyz))
            return subChunkInstance;

        byte subChunkCoordinate = subChunkInstance.getSubChunkCoordinate();

        // Handle vertical chunk comparison
        if (direction3Vector == Direction3Vector.UP ||
                direction3Vector == Direction3Vector.DOWN) {

            // Add the `subChunkCoordinate` and the `direction3Vector.y`
            byte comparativeSubChunkCoordinate = (byte) (subChunkCoordinate + direction3Vector.y);

            // If `comparativeSubChunkCoordinate` if in range `getSubChunk` and return
            if (comparativeSubChunkCoordinate >= 0 && comparativeSubChunkCoordinate < EngineSetting.WORLD_HEIGHT)
                return chunkInstance.getSubChunk(comparativeSubChunkCoordinate);

            else // Else return null
                return null;
        }

        // Flatten the `direction3Vector`
        Direction2Vector direction2Vector = direction3Vector.to2D();

        // Get the neighbor `ChunkInstance` reference
        ChunkNeighborStruct chunkNeighborStruct = chunkInstance.getChunkNeighbors();
        ChunkInstance neighborChunkInstance = chunkNeighborStruct.getNeighborChunk(direction2Vector.index);

        // get the `comparativeSubChunkInstance` reference
        SubChunkInstance comparativeSubChunkInstance = neighborChunkInstance.getSubChunk(subChunkCoordinate);

        // If `comparativeSubChunkInstance` is null return `ERROR`
        if (comparativeSubChunkInstance == null)
            return ERROR;

        return comparativeSubChunkInstance;
    }

    // Face Assembly \\
}
