package com.AdventureRPG.worldmanager.subchunks;

import com.AdventureRPG.core.shaderpipeline.RenderableInstance.MeshPacket;

public final class SubChunkMesh {

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
    }

    public boolean hasData() {
        return meshPacket.getTotalVertexCount() > 0;
    }
}
