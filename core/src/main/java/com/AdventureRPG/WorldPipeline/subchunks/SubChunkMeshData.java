package com.AdventureRPG.WorldPipeline.subchunks;

import com.AdventureRPG.core.geometrypipeline.modelmanager.MeshPacketData;
import com.AdventureRPG.core.geometrypipeline.modelmanager.ModelManager;
import com.AdventureRPG.core.geometrypipeline.vaomanager.VAOHandle;
import com.AdventureRPG.core.kernel.DataFrame;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

public final class SubChunkMeshData extends DataFrame {

    // Sub Chunk
    public final SubChunk subChunk;
    public final int subChunkIndex;

    // Data
    private final MeshPacketData meshPacketData;

    // Base \\

    public SubChunkMeshData(
            VAOHandle vao,
            ModelManager modelManager,
            SubChunk subChunk,
            int subChunkIndex) {

        // Sub Chunk
        this.subChunk = subChunk;
        this.subChunkIndex = subChunkIndex;

        // Data
        this.meshPacketData = modelManager.requestMeshPacketData(vao);
    }

    // Accessible \\

    public MeshPacketData getMeshPacketData() {
        return meshPacketData;
    }

    public void clear() {
        meshPacketData.clear();
    }

    public void addVertices(int materialId, FloatArrayList verts) {
        meshPacketData.addVertices(materialId, verts);
    }

    public boolean hasData() {
        return meshPacketData.getTotalVertexCount() > 0;
    }
}
