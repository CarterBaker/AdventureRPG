package com.AdventureRPG.WorldSystem.RenderManager;

import com.AdventureRPG.MaterialManager.MaterialManager;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.MegaChunk.MegaChunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class RenderManager {

    private final MaterialManager materialManager;

    private Long2ObjectOpenHashMap<GPUPacket> gpuPackets;
    private Long2ObjectOpenHashMap<MegaChunk> removalQueue;

    // Base \\

    public RenderManager(WorldSystem worldSystem) {

        this.materialManager = worldSystem.materialManager;

        this.gpuPackets = new Long2ObjectOpenHashMap<>();
        this.removalQueue = new Long2ObjectOpenHashMap<>();
    }

    public void update() {

        processRemovalQueue();
    }

    public void render() {

        processGPUPackets();
    }

    // Render \\

    private void processGPUPackets() {

        for (Long2ObjectOpenHashMap.Entry<GPUPacket> entry : gpuPackets.long2ObjectEntrySet())
            entry.getValue().render();
    }

    // Utility \\

    private void processRemovalQueue() {

        // Iterate over all MegaChunks marked for removal
        for (Long2ObjectOpenHashMap.Entry<MegaChunk> entry : removalQueue.long2ObjectEntrySet()) {

            long megaCoord = entry.getLongKey();

            // Retrieve and remove the corresponding gpuPacket
            GPUPacket gpuPacket = gpuPackets.remove(megaCoord);

            // If it exists, dispose it properly
            if (gpuPacket != null)
                gpuPacket.dispose();
        }

        // Clear the queue after processing
        removalQueue.clear();
    }
    // Accessible \\

    public void assessMega(MegaChunk megaChunk) {

        // Check if a gpuPacket already exists
        GPUPacket existing = gpuPackets.get(megaChunk.megaCoordinate);

        if (existing == null)
            addMega(megaChunk);
    }

    public void addMega(MegaChunk megaChunk) {

        // Check if a gpuPacket already exists
        GPUPacket existing = gpuPackets.get(megaChunk.megaCoordinate);

        if (existing != null)
            existing.dispose();

        // Create and insert the new gpuPacket
        GPUPacket gpuPacket = new GPUPacket(
                materialManager,
                megaChunk,
                megaChunk.renderPacket());

        gpuPackets.put(megaChunk.megaCoordinate, gpuPacket);
    }

    public void removeMega(MegaChunk megaChunk) {

        removalQueue.put(megaChunk.megaCoordinate, megaChunk);
    }
}
