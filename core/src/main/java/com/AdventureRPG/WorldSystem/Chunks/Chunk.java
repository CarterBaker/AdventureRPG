package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.WorldSystem.Blocks.Block;

public class Chunk {
    public final int x, y, z;
    private Block[][][] blocks;
    private boolean isDirty;

    public Chunk(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.blocks = new Block[16][16][16];
        this.isDirty = false;
    }

    public Block getBlock(int localX, int localY, int localZ) {
        return blocks[localX][localY][localZ];
    }

    public void setBlock(int localX, int localY, int localZ, Block block) {
        blocks[localX][localY][localZ] = block;
        isDirty = true;
    }

    public void markClean() {
        isDirty = false;
    }

    public boolean needsSaving() {
        return isDirty;
    }
}
