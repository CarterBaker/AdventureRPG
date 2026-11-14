package application.bootstrap.worldpipeline.structuremanager;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.AsyncContainerPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class StructurePlacementAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for placing structures into one chunk: the chunk
     * being generated, the placement cells that can reach it, and the blend
     * used to judge an anchor's biome.
     */

    // Chunk
    WorldHandle worldHandle;
    long chunkCoordinate;
    long chunkOriginX;
    long chunkOriginZ;
    SubChunkInstance[] subChunks;

    // Placement Cells
    IntArrayList cellsX;
    IntArrayList cellsZ;

    // Rules
    BiomeBlendStruct anchorBlend;

    @Override
    protected void create() {

        this.cellsX = new IntArrayList();
        this.cellsZ = new IntArrayList();
        this.anchorBlend = new BiomeBlendStruct();
    }
}
