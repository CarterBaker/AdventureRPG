package com.AdventureRPG.WorldSystem.Util;

import com.AdventureRPG.WorldSystem.SubChunks.SubChunkMesh;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public final class MeshPacket {

    public final Int2ObjectOpenHashMap<MaterialBatch> batches;
    public final int subChunkIndex;

    public MeshPacket(int subChunkIndex) {

        this.subChunkIndex = subChunkIndex;
        this.batches = new Int2ObjectOpenHashMap<>();
    }

    public void addBatch(MaterialBatch batch) {

        batches.put(batch.materialId, batch);
    }

    public void merge(MeshPacket other) {

        for (MaterialBatch batch : other.batches.values()) {

            batches.compute(batch.materialId, (mat, existing) -> {

                if (existing == null)
                    return new MaterialBatch(batch);

                existing.append(batch);

                return existing;
            });
        }
    }

    public static final class MaterialBatch {

        public final int materialId;
        private final FloatArrayList vertices;
        private final ShortArrayList indices;

        public MaterialBatch(int materialId) {

            this.materialId = materialId;
            this.vertices = new FloatArrayList();
            this.indices = new ShortArrayList();
        }

        public MaterialBatch(MaterialBatch other) {

            this.materialId = other.materialId;
            this.vertices = new FloatArrayList(other.vertices);
            this.indices = new ShortArrayList(other.indices);
        }

        public void addVertex(float... vertexData) {

            vertices.addElements(vertices.size(), vertexData);
        }

        public void addIndex(short index) {

            indices.add(index);
        }

        public void append(MaterialBatch other) {

            int vertexOffset = getVertexCount();

            for (float v : other.vertices)
                vertices.add(v);

            for (short i : other.indices)
                indices.add((short) (i + vertexOffset));
        }

        public int getVertexCount() {
            return vertices.size() / SubChunkMesh.VERT_STRIDE;
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
    }
}
