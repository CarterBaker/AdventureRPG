package com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager;

import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.core.engine.HandlePackage;
import com.internal.core.engine.settings.EngineSetting;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class DynamicModelHandle extends HandlePackage {

    // Internal
    private int materialID;
    private VAOHandle vaoHandle;
    private int vertStride;

    private FloatArrayList vertices;
    private ShortArrayList indices;

    // Internal \\

    void constructor(
            int materialID,
            VAOHandle vaoHandle) {

        // Internal
        this.materialID = materialID;
        this.vaoHandle = vaoHandle;
        this.vertStride = vaoHandle.getVertStride();

        this.vertices = new FloatArrayList();
        this.indices = new ShortArrayList();
    }

    // Utility \\

    int tryAddVertices(FloatArrayList sourceVerts, int offset, int length) {

        int floatsPerQuad = vertStride * 4;

        if (length % floatsPerQuad != 0)
            throwException("Vertex data must be quad-aligned");

        int currentVertCount = vertices.size() / vertStride;
        int availableVerts = EngineSetting.MESH_VERT_LIMIT - currentVertCount;

        if (availableVerts <= 0)
            return 0;

        int quadsToAdd = length / floatsPerQuad;
        int vertsToAdd = quadsToAdd * 4;
        int vertsFit = Math.min(availableVerts, vertsToAdd);
        int quadsFit = vertsFit / 4;

        if (quadsFit <= 0)
            return 0;

        int floatsToAdd = quadsFit * floatsPerQuad;

        vertices.addElements(vertices.size(), sourceVerts.elements(), offset, floatsToAdd);
        appendQuadIndices(currentVertCount, quadsFit);

        return floatsToAdd;
    }

    void addQuadVertices(FloatArrayList sourceVerts) {

        int floatsPerQuad = vertStride * 4;

        if (sourceVerts.size() % floatsPerQuad != 0)
            throwException("Vertex data must be quad-aligned");

        int startVertex = vertices.size() / vertStride;
        int quadCount = sourceVerts.size() / floatsPerQuad;

        vertices.addElements(vertices.size(), sourceVerts.elements(), 0, sourceVerts.size());
        appendQuadIndices(startVertex, quadCount);
    }

    private void appendQuadIndices(int baseVertex, int quadCount) {

        indices.ensureCapacity(indices.size() + quadCount * 6);

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

    void clear() {
        vertices.clear();
        indices.clear();
    }

    // Accessible \\

    public FloatArrayList getVertices() {
        return vertices;
    }

    public ShortArrayList getIndices() {
        return indices;
    }

    public boolean isEmpty() {
        return vertices.isEmpty();
    }

    public boolean isFull() {
        int currentVertCount = vertices.size() / vertStride;
        return currentVertCount >= EngineSetting.MESH_VERT_LIMIT;
    }

    public int getVertexCount() {
        return vertices.size() / vertStride;
    }
}