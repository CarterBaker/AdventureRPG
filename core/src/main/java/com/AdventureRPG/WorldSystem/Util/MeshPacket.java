package com.AdventureRPG.WorldSystem.Util;

import com.AdventureRPG.SettingsSystem.GlobalConstant;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import java.util.ArrayList;
import java.util.List;

public final class MeshPacket {

    public final Int2ObjectOpenHashMap<List<MaterialBatch>> batches;

    public MeshPacket() {
        this.batches = new Int2ObjectOpenHashMap<>();
    }

    public void addVertices(int materialId, float... verts) {
        if (verts.length % (GlobalConstant.VERT_STRIDE * 4) != 0)
            throw new IllegalArgumentException(
                    "Vertex array length must be multiple of 4 verts (quad-aligned)");

        List<MaterialBatch> list = batches.computeIfAbsent(materialId, k -> new ArrayList<>());
        MaterialBatch batch = list.isEmpty() ? null : list.get(list.size() - 1);

        if (batch == null) {

            batch = new MaterialBatch(materialId);
            list.add(batch);
        }

        MaterialBatch overflow = batch.addVerticesSafe(verts);

        if (overflow != null)
            list.add(overflow);
    }

    public void addQuad(int materialId, float[] quadVerts) {

        if (quadVerts.length != GlobalConstant.VERT_STRIDE * 4)
            throw new IllegalArgumentException(
                    "Quad must be exactly 4 verts (" + (GlobalConstant.VERT_STRIDE * 4) + " floats)");

        addVertices(materialId, quadVerts);
    }

    public void addBatch(MaterialBatch batch) {

        List<MaterialBatch> list = batches.computeIfAbsent(batch.materialId, k -> new ArrayList<>());
        MaterialBatch current = batch;

        for (MaterialBatch existing : list) {

            MaterialBatch overflow = existing.append(current);

            if (overflow != null)
                current = overflow;

            else
                return; // fully merged
        }

        list.add(current);
    }

    public void merge(MeshPacket other) {

        if (other.batches.isEmpty())
            return;

        for (List<MaterialBatch> otherList : other.batches.values())
            for (MaterialBatch batch : otherList)
                addBatch(batch);
    }

    public void clear() {
        batches.clear();
    }

    public static final class MaterialBatch {

        public final int materialId;
        private final FloatArrayList vertices;
        private final ShortArrayList indices;
        private int vertexCount = 0;

        public MaterialBatch(int materialId) {

            this.materialId = materialId;
            this.vertices = new FloatArrayList();
            this.indices = new ShortArrayList();
        }

        public MaterialBatch(MaterialBatch other) {

            this.materialId = other.materialId;
            this.vertices = new FloatArrayList(other.vertices);
            this.indices = new ShortArrayList(other.indices);
            this.vertexCount = other.vertexCount;
        }

        public int getVertexCount() {
            return vertexCount;
        }

        public int getIndexCount() {
            return indices.size();
        }

        public float[] getVerticesArray() {
            return vertices.toFloatArray();
        }

        public short[] getIndicesArray() {
            return indices.toShortArray();
        }

        public MaterialBatch addVerticesSafe(float... newVertices) {

            int vertsToAdd = newVertices.length / GlobalConstant.VERT_STRIDE;

            if (vertsToAdd % 4 != 0)
                throw new IllegalArgumentException("Vertices must be quad-aligned (multiples of 4 verts)");

            // If adding exceeds limit
            if (vertexCount + vertsToAdd > GlobalConstant.MESH_VERT_LIMIT) {

                int spaceLeft = GlobalConstant.MESH_VERT_LIMIT - vertexCount;

                // Round down to nearest multiple of 4
                spaceLeft = (spaceLeft / 4) * 4;

                if (spaceLeft > 0) {

                    // Add only quads that fit
                    int floatsToAdd = spaceLeft * GlobalConstant.VERT_STRIDE;
                    vertices.addElements(vertices.size(), newVertices, 0, floatsToAdd);

                    int quadsToAdd = spaceLeft / 4;

                    for (int q = 0; q < quadsToAdd; q++) {

                        int base = vertexCount + q * 4;
                        // Two triangles per quad
                        indices.add((short) base);
                        indices.add((short) (base + 1));
                        indices.add((short) (base + 2));

                        indices.add((short) base);
                        indices.add((short) (base + 2));
                        indices.add((short) (base + 3));
                    }

                    vertexCount += spaceLeft;
                }

                int remainingVerts = vertsToAdd - spaceLeft;

                if (remainingVerts <= 0)
                    return null;

                MaterialBatch overflow = new MaterialBatch(materialId);
                int floatsRemaining = remainingVerts * GlobalConstant.VERT_STRIDE;
                overflow.vertices.addElements(0, newVertices,
                        spaceLeft * GlobalConstant.VERT_STRIDE, floatsRemaining);
                overflow.vertexCount = remainingVerts;

                int quadsRemaining = remainingVerts / 4;

                for (int q = 0; q < quadsRemaining; q++) {

                    int base = q * 4;
                    overflow.indices.add((short) base);
                    overflow.indices.add((short) (base + 1));
                    overflow.indices.add((short) (base + 2));

                    overflow.indices.add((short) base);
                    overflow.indices.add((short) (base + 2));
                    overflow.indices.add((short) (base + 3));
                }

                return overflow;
            }

            else {

                // Fits entirely
                vertices.addElements(vertices.size(), newVertices);
                int quadsToAdd = vertsToAdd / 4;

                for (int q = 0; q < quadsToAdd; q++) {
                    int base = vertexCount + q * 4;
                    // Two triangles per quad
                    indices.add((short) base);
                    indices.add((short) (base + 1));
                    indices.add((short) (base + 2));

                    indices.add((short) base);
                    indices.add((short) (base + 2));
                    indices.add((short) (base + 3));
                }

                vertexCount += vertsToAdd;

                return null;
            }
        }

        public MaterialBatch append(MaterialBatch other) {
            return addVerticesSafe(other.getVerticesArray());
        }
    }

}
