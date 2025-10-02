package com.AdventureRPG.WorldSystem.SubChunks;

import java.util.concurrent.atomic.AtomicReference;

import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.Util.MeshPacket;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public final class SubChunkMesh {

    // Game Manager
    private final WorldSystem worldSystem;

    // Data
    public static final int VERT_POS = 3;
    public static final int VERT_NOR = 3;
    public static final int VERT_COL = 1;
    public static final int VERT_UV0 = 2;
    public static final int VERT_STRIDE = VERT_POS + VERT_NOR + VERT_COL + VERT_UV0;

    private final AtomicReference<MeshPacket> pending = new AtomicReference<>(null);
    private final Int2ObjectOpenHashMap<MeshPacket.MaterialBatch> batches;

    // Base \\

    public SubChunkMesh(WorldSystem worldSystem) {

        // Game Manager
        this.worldSystem = worldSystem;

        // Data
        this.batches = new Int2ObjectOpenHashMap<>();
    }

    public void dispose() {

        batches.clear();
        pending.set(null);
    }

    // Build \\

    public void submit(MeshPacket packet) {

        pending.set(packet);
    }

    public void build() {

        MeshPacket packet = pending.getAndSet(null);

        if (packet == null)
            return;

        batches.clear();

        // Deep copy batches so this SubChunkMesh owns its data
        for (MeshPacket.MaterialBatch mb : packet.batches.values())
            batches.put(mb.materialId, new MeshPacket.MaterialBatch(mb));
    }

    public Iterable<MeshPacket.MaterialBatch> getBatches() {
        return batches.values();
    }
}
