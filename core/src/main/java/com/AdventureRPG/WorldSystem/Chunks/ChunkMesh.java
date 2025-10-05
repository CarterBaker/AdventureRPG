package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.WorldSystem.SubChunks.SubChunk;
import com.AdventureRPG.WorldSystem.Util.MeshPacket;

public final class ChunkMesh {

    // Chunk
    private final Chunk chunk;

    // Data
    private final MeshPacket meshPacket;

    // Base \\

    public ChunkMesh(Chunk chunk) {

        // Chunk
        this.chunk = chunk;

        // Data
        this.meshPacket = new MeshPacket();
    }

    // Accessible \\

    public void Clear() {

        meshPacket.clear();
    }

    public void merge(int subChunkIndex) {

        SubChunk subChunk = chunk.getSubChunk(subChunkIndex);
        MeshPacket other = subChunk.subChunkMesh.meshPacket();

        meshPacket.merge(other);
    }

    public MeshPacket meshPacket() {
        return meshPacket;
    }
}
