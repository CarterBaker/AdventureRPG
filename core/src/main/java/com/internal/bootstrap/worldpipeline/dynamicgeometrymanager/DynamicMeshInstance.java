package com.internal.bootstrap.worldpipeline.dynamicgeometrymanager;

import com.internal.bootstrap.geometrypipeline.ibomanager.IBOHandle;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshHandle;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vbomanager.VBOHandle;
import com.internal.core.engine.InstancePackage;
import com.internal.core.engine.settings.EngineSetting;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

class DynamicMeshInstance extends InstancePackage {

    // Internal
    final String meshName;
    final int meshID;

    final VAOHandle vaoHandle;
    VBOHandle vboHandle;
    IBOHandle iboHandle;

    MeshHandle meshHandle;

    private final FloatArrayList vertices;
    private final ShortArrayList indices;
    private int vertexCount;

    // Base \\

    DynamicMeshInstance(
            String meshName,
            int meshID,
            VAOHandle vaoHandle,
            FloatArrayList vertices,
            ShortArrayList indices,
            int vertCount) {

        // Internal
        this.meshName = meshName;
        this.meshID = meshID;

        this.vaoHandle = vaoHandle;

        this.vertices = vertices;
        this.indices = indices;
        this.vertexCount = vertCount;
    }

    public DynamicMeshInstance(
            VAOHandle vaoHandle) {

        // System Data
        this.meshName = "";
        this.meshID = 0;

        // Internal
        this.vaoHandle = vaoHandle;
        this.vertices = new FloatArrayList();
        this.indices = new ShortArrayList();
        this.vertexCount = 0;
    }

    // Data \\

    int tryAddVertices(FloatArrayList sourceVerts, int offset, int length) {

        int floatsPerQuad = vaoHandle.getVertStride() * 4;

        if (length % floatsPerQuad != 0) // TODO: Add my own error
            throw new IllegalArgumentException("Vertex data is not aligned to quads");

        int quadsToAdd = length / floatsPerQuad;
        int availableVerts = EngineSetting.MESH_VERT_LIMIT - vertexCount;
        int quadsFit = Math.min(availableVerts / 4, quadsToAdd);

        if (quadsFit <= 0)
            return 0;

        int vertsToAdd = quadsFit * 4;
        int floatsToAdd = vertsToAdd * vaoHandle.getVertStride();

        // Direct copy from source list's backing array
        vertices.addElements(vertices.size(), sourceVerts.elements(), offset, floatsToAdd);

        appendQuadIndices(vertexCount, quadsFit);
        vertexCount += vertsToAdd;

        return vertsToAdd;
    }

    boolean tryAddCompleteMesh(DynamicMeshInstance source) {

        if (source.isEmpty())
            return true;

        int sourceVertCount = source.getVertexCount();

        if (sourceVertCount % 4 != 0) // TODO: Add my own error
            throw new IllegalStateException("Source mesh has non-quad-aligned vertex count: " + sourceVertCount);

        int availableVerts = EngineSetting.MESH_VERT_LIMIT - vertexCount;

        if (sourceVertCount > availableVerts)
            return false;

        // Direct array copy - much faster than going through tryAddVertices
        FloatArrayList sourceVerts = source.getVerticesList();
        vertices.addElements(vertices.size(), sourceVerts.elements(), 0, sourceVerts.size());

        // Offset and add indices
        ShortArrayList sourceIndices = source.getIndicesList();
        int indexOffset = vertexCount;

        for (int i = 0; i < sourceIndices.size(); i++)
            indices.add((short) (sourceIndices.getShort(i) + indexOffset));

        vertexCount += sourceVertCount;
        return true;
    }

    private void appendQuadIndices(int baseVertex, int quadCount) {

        indices.ensureCapacity(indices.size() + quadCount * 6);

        for (int q = 0; q < quadCount; q++) {
            int base = baseVertex + q * 4;

            // Triangle 1
            indices.add((short) base);
            indices.add((short) (base + 1));
            indices.add((short) (base + 2));

            // Triangle 2
            indices.add((short) base);
            indices.add((short) (base + 2));
            indices.add((short) (base + 3));
        }
    }

    void clear() {
        vertices.clear();
        indices.clear();
        vertexCount = 0;
    }

    // Utility \\

    int getVertexCount() {
        return vertexCount;
    }

    int getAvailableVertexSlots() {
        return EngineSetting.MESH_VERT_LIMIT - vertexCount;
    }

    boolean isFull() {
        return vertexCount >= EngineSetting.MESH_VERT_LIMIT;
    }

    boolean isEmpty() {
        return vertexCount == 0;
    }

    float getFillPercentage() {
        return (float) vertexCount / EngineSetting.MESH_VERT_LIMIT;
    }

    float[] getVerticesArray() {
        return vertices.toFloatArray();
    }

    short[] getIndicesArray() {
        return indices.toShortArray();
    }

    FloatArrayList getVerticesList() {
        return vertices;
    }

    ShortArrayList getIndicesList() {
        return indices;
    }

    void pushToGPU() {

        if (meshHandle != null)
            pullFromGPU();

        vboHandle = GLSLUtility.uploadVertexData(vaoHandle, create(VBOHandle.class), getVerticesArray());
        iboHandle = GLSLUtility.uploadIndexData(vaoHandle, create(IBOHandle.class), getIndicesArray());

        MeshHandle meshHandle = create(MeshHandle.class);
        meshHandle.constructor(
                vaoHandle.getAttributeHandle(),
                vaoHandle.getVertStride(),
                vboHandle.getVertexHandle(),
                vboHandle.getVertexCount(),
                iboHandle.getIndexHandle(),
                iboHandle.getIndexCount());
    }

    void pullFromGPU() {

        GLSLUtility.freeVBO(vboHandle);
        GLSLUtility.freeIBO(iboHandle);

        vboHandle = null;
        iboHandle = null;

        meshHandle = null;
    }
}