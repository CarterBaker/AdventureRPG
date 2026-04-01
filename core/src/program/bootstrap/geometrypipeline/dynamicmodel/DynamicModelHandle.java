package program.bootstrap.geometrypipeline.dynamicmodel;

import program.bootstrap.geometrypipeline.vao.VAOHandle;
import program.core.engine.HandlePackage;
import program.core.settings.EngineSetting;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class DynamicModelHandle extends HandlePackage {

    /*
     * CPU-side vertex and index buffer for one material bucket within a dynamic
     * draw. Accumulates quad geometry at runtime and enforces the engine vertex
     * limit. Owned by DynamicPacketInstance — never shared across packets.
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

    public void addQuadVertices(FloatArrayList sourceVerts) {

        int floatsPerQuad = vertStride * 4;

        if (sourceVerts.size() % floatsPerQuad != 0)
            throwException("Vertex data must be quad-aligned");

        int startVertex = vertices.size() / vertStride;
        int quadCount = sourceVerts.size() / floatsPerQuad;

        vertices.addElements(vertices.size(), sourceVerts.elements(), 0, sourceVerts.size());
        appendQuadIndices(startVertex, quadCount);
    }

    /*
     * Merges all quads from source into this model, applying per-vertex offsets
     * at the specified attribute indices before writing. Used by the font system
     * to position per-glyph origin-space quads at cursor positions within a
     * label's merged model. offsetIndices and offsets must be the same length.
     */
    public void mergeWithOffset(DynamicModelHandle source, int[] offsetIndices, float[] offsets) {

        if (source == null || source.isEmpty())
            return;

        if (offsetIndices.length != offsets.length)
            throwException("offsetIndices and offsets must have the same length");

        FloatArrayList src = source.vertices;
        int total = src.size();
        FloatArrayList shifted = new FloatArrayList(total);

        for (int i = 0; i < total; i += vertStride) {
            for (int j = 0; j < vertStride; j++) {

                float value = src.getFloat(i + j);

                for (int k = 0; k < offsetIndices.length; k++) {
                    if (j == offsetIndices[k]) {
                        value += offsets[k];
                        break;
                    }
                }

                shifted.add(value);
            }
        }

        addQuadVertices(shifted);
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