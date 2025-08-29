package com.AdventureRPG.WorldSystem.Chunks;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.utils.Array;

public final class SubChunkMesh {

    public static final int VERTEX_SIZE = 3 + 3 + 4 + 2;
    // position(3) + normal(3) + color(4) + uv(2) = 12 floats per vertex

    public static final int FLOATS_PER_VERTEX = VERTEX_SIZE;

    // Holds multiple material batches
    public static class MeshBatch {
        public int materialId;
        public float[] vertices; // interleaved
        public short[] indices;
        public int vertexCount;
        public int indexCount;
    }

    private final Array<MeshBatch> batches = new Array<>(4);

    public MeshBatch beginBatch(int materialId, int estimatedVerts, int estimatedIndices) {
        MeshBatch batch = new MeshBatch();
        batch.materialId = materialId;
        batch.vertices = new float[estimatedVerts * FLOATS_PER_VERTEX];
        batch.indices = new short[estimatedIndices];
        batch.vertexCount = 0;
        batch.indexCount = 0;
        batches.add(batch);
        return batch;
    }

    public Array<MeshBatch> getBatches() {
        return batches;
    }

    /**
     * Builds and returns a new Mesh sized exactly for the combined batches.
     * Caller is responsible for disposing this Mesh when no longer needed.
     */
    public Mesh build() {
        int totalVerts = 0;
        int totalIndices = 0;

        for (MeshBatch batch : batches) {
            totalVerts += batch.vertexCount;
            totalIndices += batch.indexCount;
        }

        if (totalVerts == 0 || totalIndices == 0) {
            return null; // nothing to render
        }

        // Create mesh with exact capacity
        Mesh mesh = new Mesh(
                true, // static mesh (won't update every frame)
                totalVerts,
                totalIndices,
                new VertexAttribute(Usage.Position, 3, "a_position"),
                new VertexAttribute(Usage.Normal, 3, "a_normal"),
                new VertexAttribute(Usage.ColorUnpacked, 4, "a_color"),
                new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord0"));

        // Flatten all batches into single vertex/index arrays
        float[] combinedVerts = new float[totalVerts * FLOATS_PER_VERTEX];
        short[] combinedIndices = new short[totalIndices];

        int vertOffset = 0;
        int indexOffset = 0;
        short baseVertex = 0;

        for (MeshBatch batch : batches) {
            int vertCountFloats = batch.vertexCount * FLOATS_PER_VERTEX;
            System.arraycopy(batch.vertices, 0, combinedVerts, vertOffset, vertCountFloats);

            for (int i = 0; i < batch.indexCount; i++) {
                combinedIndices[indexOffset + i] = (short) (batch.indices[i] + baseVertex);
            }

            vertOffset += vertCountFloats;
            indexOffset += batch.indexCount;
            baseVertex += batch.vertexCount;
        }

        mesh.setVertices(combinedVerts);
        mesh.setIndices(combinedIndices);

        return mesh;
    }

    public void clear() {
        batches.clear();
    }
}
