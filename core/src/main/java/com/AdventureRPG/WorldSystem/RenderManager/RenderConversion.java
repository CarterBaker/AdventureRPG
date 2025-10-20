package com.AdventureRPG.WorldSystem.RenderManager;

import com.AdventureRPG.MaterialManager.MaterialData;
import com.AdventureRPG.MaterialManager.MaterialManager;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.List;

/**
 * Converts a MeshPacket into a RenderPacket efficiently.
 * This version is pure data conversion — no rendering, no GL context.
 */

// TODO: AI Created, needs scrutiny
public final class RenderConversion {

    /**
     * Converts a MeshPacket into a RenderPacket.
     *
     * @param meshPacket      The CPU-side geometry data grouped by material.
     * @param materialManager Material manager for resolving shader + texture.
     * @return RenderPacket ready for GPU upload.
     */
    public static RenderPacket convert(MeshPacket meshPacket, MaterialManager materialManager) {

        if (meshPacket == null || meshPacket.batches.isEmpty())
            return new RenderPacket();

        Int2ObjectOpenHashMap<MaterialData> materialCache = new Int2ObjectOpenHashMap<>();
        RenderPacket renderPacket = new RenderPacket();

        for (var entry : meshPacket.batches.int2ObjectEntrySet()) {

            final int matId = entry.getIntKey();
            MaterialData matData = materialCache.computeIfAbsent(matId, id -> {
                MaterialData m = materialManager.getById(id);
                return (m != null) ? m : materialManager.getFirstMaterialUsingID(id);
            });
            if (matData == null)
                continue;

            RenderPacket.RenderKey key = renderPacket.getOrCreateKey(matData);
            List<MeshPacket.MaterialBatch> materialBatches = entry.getValue();

            for (MeshPacket.MaterialBatch batch : materialBatches) {

                float[] verts = batch.getVerticesArray();
                short[] inds = batch.getIndicesArray();

                // CPU-only batch; do NOT instantiate a Mesh here
                RenderPacket.RenderBatch renderBatch = new RenderPacket.RenderBatch(key.id, verts, inds);

                renderPacket.addBatch(renderBatch);
            }
        }

        return renderPacket;
    }

}
