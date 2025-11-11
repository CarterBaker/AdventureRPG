package com.AdventureRPG.WorldManager.Util;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import java.util.ArrayList;
import java.util.List;

import com.AdventureRPG.Core.Util.GlobalConstant;

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
            final int stride = GlobalConstant.VERT_STRIDE;
            final int vertsPerQuad = 4;
            final int vertsToAdd = newVertices.length / stride;

            if (vertsToAdd % vertsPerQuad != 0)
                throw new IllegalArgumentException("Vertices must be quad-aligned (multiples of 4 verts)");

            // How many quads are in the input
            final int quadsToAdd = vertsToAdd / vertsPerQuad;

            // How many vertices fit in this batch
            int spaceLeft = GlobalConstant.MESH_VERT_LIMIT - vertexCount;

            // How many quads *actually* fit without splitting
            int quadsThatFit = Math.min(spaceLeft / vertsPerQuad, quadsToAdd);

            // If nothing fits, early overflow
            if (quadsThatFit <= 0) {
                // Create overflow batch for all new vertices
                MaterialBatch overflow = new MaterialBatch(materialId);
                overflow.vertices.addElements(0, newVertices);
                overflow.vertexCount = vertsToAdd;

                // Build indices for overflow
                for (int q = 0; q < quadsToAdd; q++) {
                    int base = q * vertsPerQuad;
                    overflow.indices.add((short) base);
                    overflow.indices.add((short) (base + 1));
                    overflow.indices.add((short) (base + 2));
                    overflow.indices.add((short) base);
                    overflow.indices.add((short) (base + 2));
                    overflow.indices.add((short) (base + 3));
                }
                return overflow;
            }

            // Number of vertices we can safely add
            int vertsToActuallyAdd = quadsThatFit * vertsPerQuad;
            int floatsToAdd = vertsToActuallyAdd * stride;

            // Add the vertices that fit in this batch
            vertices.addElements(vertices.size(), newVertices, 0, floatsToAdd);

            // Generate indices for those quads
            for (int q = 0; q < quadsThatFit; q++) {
                int base = vertexCount + q * vertsPerQuad;
                indices.add((short) base);
                indices.add((short) (base + 1));
                indices.add((short) (base + 2));
                indices.add((short) base);
                indices.add((short) (base + 2));
                indices.add((short) (base + 3));
            }

            vertexCount += vertsToActuallyAdd;

            // If everything fit, done
            if (quadsThatFit == quadsToAdd)
                return null;

            // Otherwise, create overflow with remaining quads
            int remainingQuads = quadsToAdd - quadsThatFit;
            int floatsRemaining = remainingQuads * vertsPerQuad * stride;
            int start = floatsToAdd;

            MaterialBatch overflow = new MaterialBatch(materialId);
            overflow.vertices.addElements(0, newVertices, start, floatsRemaining);
            overflow.vertexCount = remainingQuads * vertsPerQuad;

            // Build indices for overflow
            for (int q = 0; q < remainingQuads; q++) {
                int base = q * vertsPerQuad;
                overflow.indices.add((short) base);
                overflow.indices.add((short) (base + 1));
                overflow.indices.add((short) (base + 2));
                overflow.indices.add((short) base);
                overflow.indices.add((short) (base + 2));
                overflow.indices.add((short) (base + 3));
            }

            return overflow;
        }

        public MaterialBatch append(MaterialBatch other) {
            return addVerticesSafe(other.getVerticesArray());
        }
    }

    public int getTotalVertexCount() {

        int total = 0;

        for (List<MaterialBatch> list : batches.values())
            for (MaterialBatch batch : list)
                total += batch.getVertexCount();

        return total;
    }

    public int getTotalBatchCount() {

        int total = 0;

        for (List<MaterialBatch> list : batches.values())
            total += list.size();

        return total;
    }
}
