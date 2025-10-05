package com.AdventureRPG.WorldSystem.SubChunks;

import com.AdventureRPG.WorldSystem.Util.MeshPacket;

public final class SubChunkMesh {

    // Data
    private final MeshPacket meshPacket;

    // Base \\

    public SubChunkMesh() {

        // Data
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
}
