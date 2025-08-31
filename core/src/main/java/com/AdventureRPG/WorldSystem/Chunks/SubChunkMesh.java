package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.MaterialManager.MaterialData;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.TextureArray;
import com.badlogic.gdx.utils.Array;

public final class SubChunkMesh {

    public static final int VERTEX_SIZE = 3 + 3 + 4 + 2;
    public static final int FLOATS_PER_VERTEX = VERTEX_SIZE;

    // Raw batch built on worker thread
    public static class MeshBatch {

        public int materialId;
        public Material material;
        public ShaderProgram shaderProgram;
        public TextureArray textureArray;

        public float[] vertices; // interleaved
        public short[] indices;
        public int vertexCount;
        public int indexCount;
    }

    // Final GPU-ready object
    public static class RenderBatch {
        public Mesh mesh;
        public Material material;
        public ShaderProgram shaderProgram;
        public TextureArray textureArray;
    }

    private final Array<MeshBatch> batches = new Array<>(4);
    private final Array<RenderBatch> renderBatches = new Array<>(4);

    public MeshBatch beginBatch(
            MaterialData materialData,
            int estimatedVerts, int estimatedIndices) {

        MeshBatch batch = new MeshBatch();

        batch.materialId = materialData.id;
        batch.material = materialData.material;
        batch.shaderProgram = materialData.shaderProgram;
        batch.textureArray = materialData.textureArray;
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

    public Array<RenderBatch> getRenderBatches() {
        return renderBatches;
    }

    public Node build() {

        dispose();

        Node node = new Node();
        node.id = "subchunk_" + hashCode();

        for (MeshBatch batch : batches) {
            if (batch.vertexCount == 0 || batch.indexCount == 0)
                continue;

            Mesh mesh = new Mesh(
                    true,
                    batch.vertexCount,
                    batch.indexCount,
                    new VertexAttribute(Usage.Position, 3, "a_position"),
                    new VertexAttribute(Usage.Normal, 3, "a_normal"),
                    new VertexAttribute(Usage.ColorUnpacked, 4, "a_color"),
                    new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord0"));

            float[] verts = new float[batch.vertexCount * FLOATS_PER_VERTEX];
            System.arraycopy(batch.vertices, 0, verts, 0, verts.length);

            short[] inds = new short[batch.indexCount];
            System.arraycopy(batch.indices, 0, inds, 0, inds.length);

            mesh.setVertices(verts);
            mesh.setIndices(inds);

            // store GPU-ready data if needed
            RenderBatch renderBatch = new RenderBatch();
            renderBatch.mesh = mesh;
            renderBatch.material = batch.material;
            renderBatch.shaderProgram = batch.shaderProgram;
            renderBatch.textureArray = batch.textureArray;
            renderBatches.add(renderBatch);

            // --- NEW: turn this into a NodePart ---
            MeshPart meshPart = new MeshPart();
            meshPart.mesh = mesh;
            meshPart.offset = 0;
            meshPart.size = inds.length;
            meshPart.primitiveType = GL20.GL_TRIANGLES;

            NodePart nodePart = new NodePart(meshPart, batch.material);
            node.parts.add(nodePart);
        }

        batches.clear();

        return node;
    }

    public void dispose() {

        for (RenderBatch rb : renderBatches) {
            if (rb.mesh != null)
                rb.mesh.dispose();
        }

        renderBatches.clear();
    }

    public void clearRaw() {
        batches.clear();
    }
}
