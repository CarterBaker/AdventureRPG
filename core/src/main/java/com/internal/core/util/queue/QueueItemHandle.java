package com.internal.core.util.queue;

import com.internal.core.engine.HandlePackage;

public class QueueItemHandle extends HandlePackage {

    // Internal
    private String queueItemName;
    private int QueueItemID;
    private int queueItemPriority;
    private int itemCount;

    // Internal \\

    void constructor(
            String queueItemName,
            int QueueItemID,
            int queueItemPriority) {

        // Internal
        this.queueItemName = queueItemName;
        this.QueueItemID = QueueItemID;
        this.queueItemPriority = queueItemPriority;
        this.itemCount = 0;
    }

    // Accessible \\

    public int getQueueItemID() {
        return QueueItemID;
    }

    public int getQueueItemPriority() {
        return queueItemPriority;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void addItemToCount() {
        itemCount++;
    }

    public void removeItemFromCount() {

        if (itemCount > 1)
            itemCount--;

        else
            itemCount = 0;
    }
}