package com.AdventureRPG.WorldSystem.Chunks;

import java.util.HashSet;
import java.util.Set;

import com.AdventureRPG.Util.Direction;

public class NeighborChunks {

    private Chunk up;
    private Chunk down;
    private Chunk left;
    private Chunk right;
    private Chunk front;
    private Chunk back;

    private Set<Chunk> chunks;

    public NeighborChunks() {
        this.chunks = new HashSet<>();
    }

    public void set(Direction direction, Chunk chunk) {
        switch (direction) {
            case UP -> up = chunk;
            case DOWN -> down = chunk;
            case LEFT -> left = chunk;
            case RIGHT -> right = chunk;
            case FRONT -> front = chunk;
            case BACK -> back = chunk;
        }

        rebuildSet();
    }

    public Chunk get(Direction direction) {
        return switch (direction) {
            case UP -> up;
            case DOWN -> down;
            case LEFT -> left;
            case RIGHT -> right;
            case FRONT -> front;
            case BACK -> back;
        };
    }

    public void remove(Chunk chunk) {
        for (Direction dir : Direction.values()) {
            if (get(dir) == chunk) {
                set(dir, null);
                chunks.remove(chunk);
            }
        }
    }

    public void clear() {
        up = down = left = right = front = back = null;
        chunks.clear();
    }

    public boolean isValid() {
        return chunks.size() < 6; // Number of adjacent faces to a chunk
    }

    private void rebuildSet() {
        chunks.clear();
        if (up != null)
            chunks.add(up);
        if (down != null)
            chunks.add(down);
        if (left != null)
            chunks.add(left);
        if (right != null)
            chunks.add(right);
        if (front != null)
            chunks.add(front);
        if (back != null)
            chunks.add(back);
    }

    public Set<Chunk> chunks() {
        return chunks;
    }
}