package com.AdventureRPG.WorldSystem.Chunks;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public final class MeshPacket {

    public final Int2ObjectOpenHashMap<MaterialBatch> batches;
    public final int subChunkIndex;

    MeshPacket(int subChunkIndex, Int2ObjectOpenHashMap<MaterialBatch> batches) {

        this.subChunkIndex = subChunkIndex;
        this.batches = batches;
    }

    public static final class MaterialBatch {
        public final int materialId;
        public final float[] vertices;
        public final int vertexFloatCount;
        public final short[] indices;
        public final int indexCount;

        MaterialBatch(int materialId, float[] vertices, int vertexFloatCount, short[] indices, int indexCount) {

            this.materialId = materialId;
            this.vertices = vertices;
            this.vertexFloatCount = vertexFloatCount;
            this.indices = indices;
            this.indexCount = indexCount;
        }

        public int getVertexCount() {
            return vertexFloatCount / SubChunkMesh.VERT_STRIDE;
        }
    }
}
