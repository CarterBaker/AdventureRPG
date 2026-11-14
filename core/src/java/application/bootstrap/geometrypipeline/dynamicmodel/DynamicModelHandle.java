package application.bootstrap.geometrypipeline.dynamicmodel;

import application.bootstrap.geometrypipeline.vao.VAOHandle;
import engine.root.EngineSetting;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class DynamicModelHandle extends HandlePackage {

    /*
     * CPU-side vertex and index buffer for one material bucket within a dynamic
     * draw. Accumulates quad geometry at runtime and enforces the engine vertex
     * limit. Offset copies shift the appended range in place, so merging never
     * builds a temporary vertex list. Owned by DynamicPacketInstance — never
     * shared across packets.
     */

    // Internal
    private int materialID;
    private VAOHandle vaoHandle;
    private int vertStride;
    private FloatArrayList vertices;
    private ShortArrayList indices;

    // Constructor \\

    public void constructor(int materialID, VAOHandle vaoHandle) {

        // Internal
        this.materialID = materialID;
        this.vaoHandle = vaoHandle;
        this.vertStride = vaoHandle.getVAOData().getVertStride();
        this.vertices = new FloatArrayList();
        this.indices = new ShortArrayList();
    }

    // Utility \\

    public int tryAddVertices(FloatArrayList sourceVerts, int offset, int length) {

        int floatsPerQuad = vertStride * 4;

        if (length % floatsPerQuad != 0)
            throwException("Vertex data must be quad-aligned");

        int currentVertCount = vertices.size() / vertStride;
        int availableVerts = EngineSetting.MESH_VERT_LIMIT - currentVertCount;
        int availableQuads = availableVerts / 4;

        if (availableQuads <= 0)
            return 0;

        int quadsRequested = length / floatsPerQuad;
        int quadsFit = Math.min(availableQuads, quadsRequested);
        int floatsToAdd = quadsFit * floatsPerQuad;

        vertices.addElements(vertices.size(), sourceVerts.elements(), offset, floatsToAdd);
        appendQuadIndices(currentVertCount, quadsFit);

        return floatsToAdd;
    }

    public int tryAddVertices(
            FloatArrayList sourceVerts,
            int offset,
            int length,
            int[] offsetIndices,
            float[] offsets) {

        int start = vertices.size();
        int added = tryAddVertices(sourceVerts, offset, length);

        applyOffsets(start, start + added, offsetIndices, offsets);

        return added;
    }

    public void addQuadVertices(FloatArrayList sourceVerts) {

        int floatsPerQuad = vertStride * 4;

        if (sourceVerts.size() % floatsPerQuad != 0)
            throwException("Vertex data must be quad-aligned");

        int startVertex = vertices.size() / vertStride;
        int quadCount = sourceVerts.size() / floatsPerQuad;

        vertices.addElements(vertices.size(), sourceVerts.elements(), 0, sourceVerts.size());
        appendQuadIndices(startVertex, quadCount);
    }

    public void mergeWithOffset(DynamicModelHandle source, int[] offsetIndices, float[] offsets) {

        if (source == null || source.isEmpty())
            return;

        if (offsetIndices.length != offsets.length)
            throwException("offsetIndices and offsets must have the same length");

        int start = vertices.size();

        addQuadVertices(source.vertices);
        applyOffsets(start, vertices.size(), offsetIndices, offsets);
    }

    private void applyOffsets(int start, int end, int[] offsetIndices, float[] offsets) {

        float[] elements = vertices.elements();

        for (int i = start; i < end; i += vertStride)
            for (int k = 0; k < offsetIndices.length; k++)
                elements[i + offsetIndices[k]] += offsets[k];
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

    public void clear() {
        vertices.clear();
        indices.clear();
    }

    // Accessible \\

    public int getMaterialID() {
        return materialID;
    }

    public VAOHandle getVAOHandle() {
        return vaoHandle;
    }

    public FloatArrayList getVertices() {
        return vertices;
    }

    public ShortArrayList getIndices() {
        return indices;
    }

    public boolean isEmpty() {
        return vertices.isEmpty();
    }

    public int getVertexCount() {
        return vertices.size() / vertStride;
    }

    public boolean isFull() {

        int currentVertCount = vertices.size() / vertStride;
        int availableVerts = EngineSetting.MESH_VERT_LIMIT - currentVertCount;

        return availableVerts < 4;
    }
}