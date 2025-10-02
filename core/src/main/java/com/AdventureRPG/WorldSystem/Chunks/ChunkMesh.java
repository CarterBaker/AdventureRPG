package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.WorldSystem.Util.MeshPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public final class ChunkMesh {

    private final Int2ObjectOpenHashMap<MeshPacket.MaterialBatch> batches;

    public ChunkMesh() {

        this.batches = new Int2ObjectOpenHashMap<>();
    }

    public void clear() {

        batches.clear();
    }

    public void addBatch(MeshPacket.MaterialBatch batch) {

        batches.compute(batch.materialId, (mat, existing) -> {

            if (existing == null)
                return new MeshPacket.MaterialBatch(batch);

            existing.append(batch);

            return existing;
        });
    }

    public void merge(MeshPacket packet) {

        for (MeshPacket.MaterialBatch batch : packet.batches.values())
            addBatch(batch);
    }

    public Int2ObjectOpenHashMap<MeshPacket.MaterialBatch> getBatches() {
        return batches;
    }

    public boolean isEmpty() {
        return batches.isEmpty();
    }
}
