package com.AdventureRPG.WorldPipeline.chunks;

import com.AdventureRPG.WorldPipeline.subchunks.SubChunk;
import com.AdventureRPG.core.geometrypipeline.modelmanager.MeshPacketData;
import com.AdventureRPG.core.geometrypipeline.modelmanager.ModelManager;
import com.AdventureRPG.core.geometrypipeline.vaomanager.VAOHandle;
import com.AdventureRPG.core.kernel.DataFrame;

public final class ChunkMeshData extends DataFrame {

    // Chunk
    private final Chunk chunk;

    // Data
    private final MeshPacketData meshPacketData;

    // Base \\

    public ChunkMeshData(
            VAOHandle vao,
            ModelManager modelManager,
            Chunk chunk) {

        // Chunk
        this.chunk = chunk;

        // Data
        this.meshPacketData = modelManager.requestMeshPacketData(vao);
    }

    // Accessible \\

    public void Clear() {
        meshPacketData.clear();
    }

    public void merge(int subChunkIndex) {

        SubChunk subChunk = chunk.getSubChunk(subChunkIndex);
        MeshPacketData subChunkMeshPacketData = subChunk.getMeshPacketData();

        if (subChunkMeshPacketData == null)
            return;

        meshPacketData.merge(subChunkMeshPacketData);
    }

    public MeshPacketData getMeshPacketData() {
        return meshPacketData;
    }
}
