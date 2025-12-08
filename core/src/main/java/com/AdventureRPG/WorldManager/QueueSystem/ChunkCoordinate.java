package com.AdventureRPG.worldmanager.queuesystem;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class ChunkCoordinate extends QueueBundle {

    private final LongOpenHashSet check;

    public ChunkCoordinate(int capacity) {

        super(capacity);
        this.check = new LongOpenHashSet(capacity);
    }

    @Override
    public void enqueue(long coordinate) {

        if (check.add(coordinate)) // only enqueue if not already present
            super.enqueue(coordinate);
    }

    @Override
    public long dequeue() {

        long coordinate = super.dequeue();
        check.remove(coordinate);

        return coordinate;
    }

    @Override
    public void clear() {
        super.clear();
        check.clear();
    }
}
