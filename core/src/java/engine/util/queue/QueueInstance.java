package engine.util.queue;

import engine.root.EngineSetting;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class QueueInstance extends InstancePackage {

    /*
     * Round-robin streaming queue. Distributes load across registered queue
     * items within per-frame and per-batch limits. Resets automatically when
     * the frame limit is reached.
     */

    // Queue Items
    private Int2ObjectOpenHashMap<QueueItemHandle> items;
    private int totalQueues;

    // Round-Robin Tracking
    private int currentQueueIndex;

    // Frame Limits
    private int maxPerFrame;
    private int processedItemsThisFrame;

    // Batch Limits
    private int maxPerBatch;
    private int processedItemsThisBatch;

    // Internal \\

    @Override
    protected void create() {

        // Queue Items
        this.items = new Int2ObjectOpenHashMap<>();
        this.totalQueues = 0;

        // Round-Robin Tracking
        this.currentQueueIndex = 0;

        // Frame Limits
        this.maxPerFrame = EngineSetting.MAX_CHUNK_STREAM_PER_FRAME;
        this.processedItemsThisFrame = 0;

        // Batch Limits
        this.maxPerBatch = EngineSetting.MAX_CHUNK_STREAM_PER_BATCH;
        this.processedItemsThisBatch = 0;
    }

    // Management \\

    public QueueItemHandle addQueueItem(String queueItemName) {
        int id = totalQueues++;
        QueueItemHandle handle = create(QueueItemHandle.class);
        handle.constructor(queueItemName, id);
        items.put(id, handle);
        return handle;
    }

    // Accessible \\

    public QueueItemHandle getNextQueueItem() {

        if (processedItemsThisFrame >= maxPerFrame) {
            processedItemsThisFrame = 0;
            processedItemsThisBatch = 0;
            return null;
        }

        processedItemsThisFrame++;
        processedItemsThisBatch++;

        if (processedItemsThisBatch >= maxPerBatch) {
            processedItemsThisBatch = 0;
            currentQueueIndex = (currentQueueIndex + 1) % totalQueues;
        }

        return items.get(currentQueueIndex);
    }
}