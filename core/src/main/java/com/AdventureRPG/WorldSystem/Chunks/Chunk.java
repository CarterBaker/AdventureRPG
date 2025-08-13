package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.Util.Coordinate2Int;
import com.AdventureRPG.WorldSystem.Blocks.BlockData;

public class Chunk {

    // Chunk
    public final int CoordinateX, CoordinateY;
    public int positionX, positionY;

    // Blocks
    private BlockData[][][] blocks;

    // Base \\

    public Chunk(long coordinate) {

        // Chunk
        this.CoordinateX = Coordinate2Int.unpackX(coordinate);
        this.CoordinateY = Coordinate2Int.unpackY(coordinate);
    }

    // Chunk \\

    public void generate(BlockData[][][] blocks) {
        this.blocks = blocks;
    }

    public void moveTo(long position) {

        this.positionX = Coordinate2Int.unpackX(position);
        this.positionY = Coordinate2Int.unpackY(position);
    }

    public void placeBlock(int x, int y, int z, int blockID) {
        blocks[x][y][z].PlaceBlock(blockID);
    }

    public void breakBlock(int x, int y, int z) {
        blocks[x][y][z].BreakBlock();
    }

    public void dispose() {

    }
}
