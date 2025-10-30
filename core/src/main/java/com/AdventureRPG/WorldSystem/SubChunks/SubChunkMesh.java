package com.AdventureRPG.WorldSystem.SubChunks;

import com.AdventureRPG.Util.Coordinate2Int;
import com.AdventureRPG.WorldSystem.RenderManager.MeshPacket;

public final class SubChunkMesh {

    // Debug
    private final boolean debug = false; // TODO: Debug line

    // Data
    public final SubChunk subChunk;
    public final int subChunkIndex;
    private final MeshPacket meshPacket;

    // Base \\

    public SubChunkMesh(SubChunk subChunk, int subChunkIndex) {

        // Data
        this.subChunk = subChunk;
        this.subChunkIndex = subChunkIndex;
        this.meshPacket = new MeshPacket();
    }

    // Accessible \\

    public MeshPacket meshPacket() {
        return meshPacket;
    }

    public void clear() {

        meshPacket.clear();
    }

    public void addVertices(int materialId, float... verts) {

        meshPacket.addVertices(materialId, verts);

        if (debug && subChunkIndex == 0 && subChunk.chunk.coordinate == Coordinate2Int.pack(0, 0))
            debug("Added to chunk: " + Coordinate2Int.toString(subChunk.chunk.coordinate) +
                    " at index " + subChunkIndex +
                    " adding : " + verts.length / 9 + " verts," +
                    " total verts: " + meshPacket.getTotalVertexCount());
    }

    public boolean hasData() {
        return meshPacket.getTotalBatchCount() != 0;
    }

    // Debug \\

    private void debug(String input) {

        System.out.println("[SubChunkMesh] " + input);
    }
}
