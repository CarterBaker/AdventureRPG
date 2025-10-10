package com.AdventureRPG.WorldSystem.RenderManager;

import com.AdventureRPG.MaterialManager.MaterialManager;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.MegaChunk.MegaChunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class RenderManager {

    private final MaterialManager materialManager;

    private Long2ObjectOpenHashMap<DrawCall> drawCalls;
    private Long2ObjectOpenHashMap<MegaChunk> removalQueue;

    // Base \\

    public RenderManager(WorldSystem worldSystem) {

        this.materialManager = worldSystem.materialManager;

        this.drawCalls = new Long2ObjectOpenHashMap<>();
        this.removalQueue = new Long2ObjectOpenHashMap<>();
    }

    public void update() {

        processRemovalQueue();
    }

    public void render() {

        processDrawCalls();
    }

    // Render \\

    private void processDrawCalls() {

        for (Long2ObjectOpenHashMap.Entry<DrawCall> entry : drawCalls.long2ObjectEntrySet())
            entry.getValue().render();
    }

    // Utility \\

    private void processRemovalQueue() {

        // Iterate over all MegaChunks marked for removal
        for (Long2ObjectOpenHashMap.Entry<MegaChunk> entry : removalQueue.long2ObjectEntrySet()) {

            long megaCoord = entry.getLongKey();

            // Retrieve and remove the corresponding DrawCall
            DrawCall drawCall = drawCalls.remove(megaCoord);

            // If it exists, dispose it properly
            if (drawCall != null)
                drawCall.dispose();
        }

        // Clear the queue after processing
        removalQueue.clear();
    }
    // Accessible \\

    public void assessMega(MegaChunk megaChunk) {

        // Check if a DrawCall already exists
        DrawCall existing = drawCalls.get(megaChunk.megaCoordinate);

        if (existing == null)
            addMega(megaChunk);
    }

    public void addMega(MegaChunk megaChunk) {

        // Check if a DrawCall already exists
        DrawCall existing = drawCalls.get(megaChunk.megaCoordinate);

        if (existing != null)
            existing.dispose();

        // Create and insert the new DrawCall
        DrawCall drawCall = new DrawCall(
                materialManager,
                megaChunk,
                megaChunk.renderPacket());

        drawCalls.put(megaChunk.megaCoordinate, drawCall);
    }

    public void removeMega(MegaChunk megaChunk) {

        removalQueue.put(megaChunk.megaCoordinate, megaChunk);
    }
}
