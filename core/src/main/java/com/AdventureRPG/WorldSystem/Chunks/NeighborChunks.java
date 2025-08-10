package com.AdventureRPG.WorldSystem.Chunks;

import java.util.HashSet;
import java.util.Set;

import com.AdventureRPG.Util.Direction;

public class NeighborChunks {

    private Chunk up;
    private Chunk north;
    private Chunk south;
    private Chunk east;
    private Chunk west;
    private Chunk down;

    private Set<Chunk> chunks;

    public NeighborChunks() {
        this.chunks = new HashSet<>();
    }

    public void set(Direction direction, Chunk chunk) {
        switch (direction) {
            case UP -> up = chunk;
            case NORTH -> north = chunk;
            case SOUTH -> south = chunk;
            case EAST -> east = chunk;
            case WEST -> west = chunk;
            case DOWN -> down = chunk;
        }

        rebuildSet();
    }

    public Chunk get(Direction direction) {
        return switch (direction) {
            case UP -> up;
            case NORTH -> north;
            case SOUTH -> south;
            case EAST -> east;
            case WEST -> west;
            case DOWN -> down;
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
        up = north = south = east = west = down = null;
        chunks.clear();
    }

    public boolean isValid() {
        return chunks.size() < 6; // Number of adjacent faces to a chunk
    }

    private void rebuildSet() {
        chunks.clear();
        if (up != null)
            chunks.add(up);
        if (north != null)
            chunks.add(north);
        if (south != null)
            chunks.add(south);
        if (east != null)
            chunks.add(east);
        if (west != null)
            chunks.add(west);
        if (down != null)
            chunks.add(down);
    }

    public Set<Chunk> chunks() {
        return chunks;
    }
}