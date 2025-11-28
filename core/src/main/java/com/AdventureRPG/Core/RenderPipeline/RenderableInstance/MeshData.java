package com.AdventureRPG.Core.RenderPipeline.RenderableInstance;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import com.AdventureRPG.Core.Bootstrap.EngineSetting;
import com.badlogic.gdx.math.Matrix4;

public final class MeshData extends RenderableInstance {

    private final MeshPacket meshPacket;
    public final int materialId;

    private final FloatArrayList vertices;
    private final ShortArrayList indices;
    private int vertexCount;

    private final int stride;
    private final int vertsPerQuad;

    public MeshData(MeshPacket meshPacket, int handle, int materialId) {

        super(handle);

        this.meshPacket = meshPacket;
        this.materialId = materialId;

        this.vertices = new FloatArrayList();
        this.indices = new ShortArrayList();
        this.vertexCount = 0;

        this.stride = meshPacket.stride;
        this.vertsPerQuad = meshPacket.vertsPerQuad;
    }

    // Mesh Data \\

    public MeshData addVertices(float[] verts) {
        return addVerticesPartial(verts, 0, verts.length);
    }

    public MeshData addVerticesPartial(float[] verts, int offset, int length) {

        if (length % (stride * vertsPerQuad) != 0) // TODO: Centralize error
            throw new IllegalArgumentException("Vertex array not quad-aligned.");

        int quadsToAdd = length / (stride * vertsPerQuad);
        int quadsFit = Math.min((EngineSetting.MESH_VERT_LIMIT - vertexCount) / vertsPerQuad, quadsToAdd);

        if (quadsFit <= 0) {

            MeshData overflow = new MeshData(meshPacket, handle, materialId);

            overflow.vertices.addElements(0, verts, offset, length);
            overflow.vertexCount = quadsToAdd * vertsPerQuad;
            overflow.buildQuadIndices(0, quadsToAdd);

            return overflow;
        }

        // Add what fits
        int vertsToAdd = quadsFit * vertsPerQuad;
        int floatsToAdd = vertsToAdd * stride;

        vertices.addElements(vertices.size(), verts, offset, floatsToAdd);
        buildQuadIndices(vertexCount, quadsFit);
        vertexCount += vertsToAdd;

        // If all data fit return null
        if (quadsFit == quadsToAdd)
            return null;

        // Overflow remaining
        int remainingQuads = quadsToAdd - quadsFit;
        int start = offset + floatsToAdd;
        int floatsRemaining = remainingQuads * vertsPerQuad * stride;

        MeshData overflow = new MeshData(meshPacket, handle, materialId);
        overflow.vertices.addElements(0, verts, start, floatsRemaining);
        overflow.vertexCount = remainingQuads * vertsPerQuad;
        overflow.buildQuadIndices(0, remainingQuads);

        return overflow;
    }

    private void buildQuadIndices(int baseVertex, int quadCount) {

        for (int q = 0; q < quadCount; q++) {

            int base = baseVertex + q * 4;

            indices.add((short) base);
            indices.add((short) (base + 1));
            indices.add((short) (base + 2));
            indices.add((short) base);
            indices.add((short) (base + 2));
            indices.add((short) (base + 3));
        }
    }

    public MeshData merge(MeshData other) {

        if (this.materialId != other.materialId) // TODO: Centralize error
            throw new IllegalArgumentException("Cannot merge MeshData with different materials.");

        float[] otherVerts = other.getVerticesArray();
        int offset = 0;

        MeshData current = this;

        while (offset < otherVerts.length) {

            MeshData overflow = current.addVerticesPartial(otherVerts, offset, otherVerts.length - offset);

            if (overflow != null) {

                offset += current.vertexCount * stride;
                return overflow;
            }

            else
                break;
        }
        return null; // everything merged successfully
    }

    // Accessible \\

    public int getVertexCount() {
        return vertexCount;
    }

    public float[] getVerticesArray() {
        return vertices.toFloatArray();
    }

    public short[] getIndicesArray() {
        return indices.toShortArray();
    }

    public boolean hasRoomForQuads(int quadCount) {
        return vertexCount + quadCount * 4 <= EngineSetting.MESH_VERT_LIMIT;
    }

    public MeshPacket getMeshPacket() {
        return meshPacket;
    }

    public Matrix4 getTransform() {
        return meshPacket.getTransform();
    }
}
