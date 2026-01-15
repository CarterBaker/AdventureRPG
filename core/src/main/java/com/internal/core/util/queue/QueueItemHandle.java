package com.internal.core.util.queue;

import com.internal.core.engine.HandlePackage;

public class QueueItemHandle extends HandlePackage {

    // Internal
    private String queueItemName;
    private int QueueItemID;

    // Internal \\

    void constructor(
            String queueItemName,
            int QueueItemID) {

        // Internal
        this.queueItemName = queueItemName;
        this.QueueItemID = QueueItemID;
    }

    // Accessible \\

    public String getQueueItemName() {
        return queueItemName;
    }

    public int getQueueItemID() {
        return QueueItemID;
    }
}