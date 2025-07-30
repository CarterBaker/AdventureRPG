package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.Blocks.Block;

public class Chunk {

    private ChunkSystem ChunkSystem;

    public final int x, y, z;
    private Block[][][] blocks;
    private boolean isDirty;

    public Chunk(int x, int y, int z, int CHUNK_SIZE) {

        this.x = x;
        this.y = y;
        this.z = z;
        this.blocks = new Block[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];
        this.isDirty = false;
    }

    public void AssignChunkSystem(ChunkSystem ChunkSystem) {
        this.ChunkSystem = ChunkSystem;
    }

    public void Generate(Block[][][] blocks) {
        this.blocks = blocks;
    }

    public Block getBlock(int localX, int localY, int localZ) {
        return blocks[localX][localY][localZ];
    }

    public void setBlock(int x, int y, int z, Block block) {

        blocks[x][y][z] = block;
        isDirty = true;
    }

    public boolean needsSaving() {
        return isDirty;
    }

    public void markClean() {
        isDirty = false;
    }

    // Mesh \\

    public void BuildAt(Vector3Int position) {
        Build();
        MoveTo(position);
    }

    public void Build() {

    }

    public void MoveTo(Vector3Int position) {

    }

}
