package com.AdventureRPG.WorldSystem.QueueSystem.Queue;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;

public class QueueBundle {

    protected final LongArrayFIFOQueue queue;

    public QueueBundle(int capacity) {
        this.queue = new LongArrayFIFOQueue(capacity);
    }

    public void enqueue(long coordinate) {
        queue.enqueue(coordinate);
    }

    public long dequeue() {
        return queue.dequeueLong();
    }

    public int size() {
        return queue.size();
    }

    public void clear() {
        queue.clear();
    }
}
