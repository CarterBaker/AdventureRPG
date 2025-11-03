package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.WorldSystem.SubChunks.SubChunk;
import com.AdventureRPG.WorldSystem.Util.MeshPacket;

public final class ChunkMesh {

    // Debug
    private final boolean debug = false; // TODO: Debug line

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

    public MeshPacket getMeshPacket() {
        return meshPacket;
    }

    // Debug \\

    private void debug(String input) {
        System.out.println("[ChunkMesh] " + input);
    }
}
