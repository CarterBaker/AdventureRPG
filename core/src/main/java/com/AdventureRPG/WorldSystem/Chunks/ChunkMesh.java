package com.AdventureRPG.WorldSystem.Chunks;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ShortArray;
import com.badlogic.gdx.graphics.GL20;

/**
 * ChunkMesh - fast primitive-backed buffer for chunk geometry.
 *
 * Vertex layout (floats): pos(3) + normal(3) + uv(2) + color(4) = 12 floats per
 * vertex.
 */
public final class ChunkMesh {

    // Vertex layout size
    private static final int VERTEX_SIZE = 12;

    // primitive buffers (avoid boxing)
    private final FloatArray vertices = new FloatArray(false, 1024); // grows as needed
    private final IntMap<ShortArray> materialIndices = new IntMap<>(); // materialId -> indices

    public ChunkMesh() {
    }

    // --------------------
    // BUILDING GEOMETRY (called from builder thread)
    // --------------------

    /** Add one vertex with all attributes. */
    public void addVertex(
            float x, float y, float z,
            float nx, float ny, float nz,
            float u, float v,
            float r, float g, float b, float a) {

        // addAll with small array is fine; avoids creating Float objects
        vertices.add(x);
        vertices.add(y);
        vertices.add(z);

        vertices.add(nx);
        vertices.add(ny);
        vertices.add(nz);

        vertices.add(u);
        vertices.add(v);

        vertices.add(r);
        vertices.add(g);
        vertices.add(b);
        vertices.add(a);
    }

    /** Add an index to the given material bucket. */
    public void addIndex(int materialId, short i) {
        ShortArray arr = materialIndices.get(materialId);
        if (arr == null) {
            arr = new ShortArray(false, 256);
            materialIndices.put(materialId, arr);
        }
        arr.add(i);
    }

    /** Current number of vertices (useful for base index math). */
    public int getVertexCount() {
        return vertices.size / VERTEX_SIZE;
    }

    /** Clear geometry but keep buffers (no GC churn). */
    public void clear() {
        vertices.clear();
        // keep allocated arrays for indices
        for (ShortArray arr : materialIndices.values()) {
            arr.clear();
        }
    }

    // --------------------
    // BUILDING Mesh (should be called on main thread)
    // --------------------

    /**
     * Uploads current data into a Mesh.
     *
     * @param mesh If null, a new Mesh is created. If a Mesh is supplied it will be
     *             reused.
     * @return Array of MeshParts (one per materialId used). Each MeshPart.id is
     *         "material_<id>".
     */
    public MeshPart[] build(Mesh mesh) {
        int numVertices = vertices.size / VERTEX_SIZE;

        // compute total indices across all materials
        int totalIndices = 0;
        for (ShortArray arr : materialIndices.values()) {
            totalIndices += arr.size;
        }

        // create mesh if needed
        if (mesh == null) {
            mesh = new Mesh(
                    true, // static (fast) - toggle as you need
                    numVertices,
                    totalIndices,
                    new VertexAttributes(
                            VertexAttribute.Position(),
                            VertexAttribute.Normal(),
                            VertexAttribute.TexCoords(0),
                            VertexAttribute.ColorUnpacked()));
        } else {
            // If mesh exists, we will overwrite its vertex & index buffers
            // (libGDX handles resizing inside setVertices/setIndices).
        }

        // Upload vertex data (bulk)
        if (vertices.size > 0)
            mesh.setVertices(vertices.items, 0, vertices.size);
        else
            mesh.setVertices(new float[0]);

        // Concatenate all indices in material order and create MeshParts
        short[] allIndices = new short[totalIndices];
        MeshPart[] parts = new MeshPart[materialIndices.size];

        int offset = 0;
        int partIdx = 0;

        for (IntMap.Entry<ShortArray> entry : materialIndices.entries()) {
            ShortArray arr = entry.value;

            if (arr.size == 0)
                continue;

            System.arraycopy(arr.items, 0, allIndices, offset, arr.size);

            MeshPart part = new MeshPart();
            part.mesh = mesh;
            part.primitiveType = GL20.GL_TRIANGLES;
            part.offset = offset;
            part.size = arr.size;
            part.id = "material_" + entry.key; // use this id to lookup Material when rendering

            parts[partIdx++] = part;
            offset += arr.size;
        }

        if (totalIndices > 0)
            mesh.setIndices(allIndices);
        else
            mesh.setIndices(new short[0]);

        // trim returned array to actual number of parts used
        if (partIdx < parts.length) {
            MeshPart[] trimmed = new MeshPart[partIdx];
            System.arraycopy(parts, 0, trimmed, 0, partIdx);
            return trimmed;
        }

        return parts;
    }
}
