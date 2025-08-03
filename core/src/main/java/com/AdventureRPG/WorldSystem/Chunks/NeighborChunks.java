package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.Util.Direction;

public class NeighborChunks {
    public Chunk up;
    public Chunk down;
    public Chunk left;
    public Chunk right;
    public Chunk front;
    public Chunk back;

    public void set(Direction direction, Chunk chunk) {
        switch (direction) {
            case UP -> up = chunk;
            case DOWN -> down = chunk;
            case LEFT -> left = chunk;
            case RIGHT -> right = chunk;
            case FRONT -> front = chunk;
            case BACK -> back = chunk;
        }
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

    public void clear() {
        up = down = left = right = front = back = null;
    }

    public boolean isValid() {
        return (up != null &&
                down != null &&
                left != null &&
                right != null &&
                front != null &&
                back != null);
    }
}