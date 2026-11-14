package application.bootstrap.worldpipeline.liquidmanager;

import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import engine.root.StructPackage;

class LiquidCellStruct extends StructPackage {

    /*
     * Addresses one block cell anywhere in the loaded world — the chunk that
     * owns it, the subchunk slice it sits in, and its packed position inside
     * that slice. Written in place by neighbor resolution and reused for the
     * life of the liquid pipeline, never allocated per step.
     */

    // Internal
    private ChunkInstance chunkInstance;
    private SubChunkInstance subChunkInstance;
    private int packedXYZ;

    // Management \\

    void set(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, int packedXYZ) {
        this.chunkInstance = chunkInstance;
        this.subChunkInstance = subChunkInstance;
        this.packedXYZ = packedXYZ;
    }

    void set(LiquidCellStruct other) {
        set(other.chunkInstance, other.subChunkInstance, other.packedXYZ);
    }

    // Accessible \\

    ChunkInstance getChunkInstance() {
        return chunkInstance;
    }

    SubChunkInstance getSubChunkInstance() {
        return subChunkInstance;
    }

    int getPackedXYZ() {
        return packedXYZ;
    }
}
