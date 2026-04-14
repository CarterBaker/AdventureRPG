package engine.util.queue;

import engine.root.HandlePackage;

public class QueueItemHandle extends HandlePackage {

    // Internal
    private String queueItemName;
    private int queueItemID;

    // Internal \\

    void constructor(
            String queueItemName,
            int QueueItemID) {

        // Internal
        this.queueItemName = queueItemName;
        this.queueItemID = QueueItemID;
    }

    // Accessible \\

    public String getQueueItemName() {
        return queueItemName;
    }

    public int getQueueItemID() {
        return queueItemID;
    }
}