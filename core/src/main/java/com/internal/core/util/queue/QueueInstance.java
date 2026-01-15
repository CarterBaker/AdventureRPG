package com.internal.core.util.queue;

import com.internal.core.engine.InstancePackage;
import com.internal.core.engine.settings.EngineSetting;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class QueueInstance extends InstancePackage {

    // Queue items
    private Int2ObjectOpenHashMap<QueueItemHandle> items;
    private int totalQueues;

    // Round-robin tracking
    private int currentQueueIndex;

    // Frame limits
    private int MAX_CHUNK_STREAM_PER_FRAME;
    private int processedItemsThisFrame;

    // Batch limits
    private int MAX_CHUNK_STREAM_PER_BATCH;
    private int processedItemsThisBatch;

    @Override
    protected void create() {
        this.items = new Int2ObjectOpenHashMap<>();
        this.totalQueues = 0;
        this.currentQueueIndex = 0;

        this.MAX_CHUNK_STREAM_PER_FRAME = EngineSetting.MAX_CHUNK_STREAM_PER_FRAME;
        this.processedItemsThisFrame = 0;

        this.MAX_CHUNK_STREAM_PER_BATCH = EngineSetting.MAX_CHUNK_STREAM_PER_BATCH;
        this.processedItemsThisBatch = 0;
    }

    public QueueItemHandle addQueueItem(String queueItemName) {
        int id = totalQueues++;

        QueueItemHandle handle = create(QueueItemHandle.class);
        handle.constructor(queueItemName, id);

        items.put(id, handle);
        return handle;
    }

    public QueueItemHandle getNextQueueItem() {
        // Check frame limit
        if (processedItemsThisFrame >= MAX_CHUNK_STREAM_PER_FRAME) {
            processedItemsThisFrame = 0;
            processedItemsThisBatch = 0;
            return null;
        }

        // Increment frame counter
        processedItemsThisFrame++;
        processedItemsThisBatch++;

        // Check batch limit - switch to next queue
        if (processedItemsThisBatch >= MAX_CHUNK_STREAM_PER_BATCH) {
            processedItemsThisBatch = 0;
            currentQueueIndex = (currentQueueIndex + 1) % totalQueues;
        }

        // Return current queue
        return items.get(currentQueueIndex);
    }
}