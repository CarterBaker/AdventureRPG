package application.bootstrap.worldpipeline.structuremanager;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class StructurePlacementAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for stamping structures into one chunk: the chunk
     * being generated, the placement cells that can reach it, the blend used
     * to judge an anchor's biome, and the lowest solid structure block per
     * column that a foundation is filled up to.
     */

    static final int COLUMN_COUNT = EngineSetting.CHUNK_SIZE * EngineSetting.CHUNK_SIZE;

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

    // Foundation
    int[] columnFloorY;

    @Override
    protected void create() {

        this.cellsX = new IntArrayList();
        this.cellsZ = new IntArrayList();
        this.anchorBlend = new BiomeBlendStruct();
        this.columnFloorY = new int[COLUMN_COUNT];
    }
}
