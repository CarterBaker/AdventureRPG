package com.AdventureRPG.core.util.queuesystem;

import com.AdventureRPG.core.engine.InstancePackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class QueueInstance extends InstancePackage {

    // Internal

    // id -> handle
    private final Int2ObjectOpenHashMap<QueueItemHandle> items = new Int2ObjectOpenHashMap<>();

    // active IDs (only IDs with count > 0)
    private final IntOpenHashSet activeIds = new IntOpenHashSet();

    // last ID sent (for stable tie-breaking)
    private int lastSentId = -1;

    // Queue \\

    public QueueItemHandle addQueueItem(
            String queueItemName,
            int priority) {

        int id = items.size();

        QueueItemHandle handle = create(QueueItemHandle.class);
        handle.constructor(
                queueItemName,
                id,
                priority);

        items.put(id, handle);

        return handle;
    }

    public QueueItemHandle getNextQueueItem() {

        QueueItemHandle best = null;
        int bestScore = Integer.MIN_VALUE;
        int bestDistance = Integer.MAX_VALUE;
        int bestId = -1;

        int totalIds = items.size();

        for (int id : activeIds) {

            int queueItemPriority = items.get(id).getQueueItemPriority();
            int itemCount = items.get(id).getItemCount();
            int score = queueItemPriority * 100 + itemCount;

            if (score > bestScore) {

                // New best score → pick immediately
                bestScore = score;
                best = items.get(id);
                bestDistance = 0;
                bestId = id;

            }

            else if (score == bestScore) {

                // Tie → pick next cyclically after lastSentId
                int distance = (id - lastSentId + totalIds) % totalIds;

                if (distance > 0 && distance < bestDistance) {
                    best = items.get(id);
                    bestDistance = distance;
                    bestId = id;
                }
            }
        }

        if (best != null)
            lastSentId = bestId;

        return best;
    }

    // Accessible \\

    public void addItemToQueue(int id) {

        QueueItemHandle handle = items.get(id);
        if (handle == null)
            return;

        // increment internal counter
        handle.addItemToCount();

        // track active IDs
        activeIds.add(id);
    }

    public void removeItemFromQueue(int id) {

        QueueItemHandle handle = items.get(id);
        if (handle == null)
            return;

        // decrement internal counter
        handle.removeItemFromCount();

        // remove from active set if count reaches 0
        if (handle.getItemCount() <= 0)
            activeIds.remove(id);
    }
}
