package com.AdventureRPG.WorldSystem.Chunks;

import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public final class SubChunkPacket {

    public final Int2ObjectOpenHashMap<MaterialBatch> batches;
    public final int subChunkIndex;

    SubChunkPacket(int subChunkIndex, Int2ObjectOpenHashMap<MaterialBatch> batches) {

        this.subChunkIndex = subChunkIndex;
        this.batches = batches;
    }

    public static final class MaterialBatch {

        public final int materialId;
        public final float[] vertices;
        public final short[] indices;
        public final int vertexCount;
        public final int indexCount;

        MaterialBatch(int materialId, FloatArray v, ShortArray i) {

            this.materialId = materialId;
            this.vertices = v.toArray();
            this.indices = i.toArray();
            this.vertexCount = this.vertices.length / SubChunkMesh.VERT_STRIDE;
            this.indexCount = this.indices.length;
        }
    }
}
