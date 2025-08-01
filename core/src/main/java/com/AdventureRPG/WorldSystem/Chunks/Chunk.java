package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.Blocks.Block;

public class Chunk {

    // Base

    private final Settings settings;
    private final ChunkSystem ChunkSystem;

    // Chunk

    public final int x, y, z;
    private Block[][][] blocks;
    private boolean isDirty;

    // Settings

    private final int CHUNK_SIZE;
    private final int LOD_START_DISTANCE;
    private final int MAX_LOD_DISTANCE;

    public Chunk(int x, int y, int z, ChunkSystem ChunkSystem) {

        // Base

        this.settings = ChunkSystem.settings;
        this.ChunkSystem = ChunkSystem;

        // Settings

        this.CHUNK_SIZE = settings.CHUNK_SIZE;
        this.LOD_START_DISTANCE = settings.LOD_START_DISTANCE;
        this.MAX_LOD_DISTANCE = settings.MAX_LOD_DISTANCE;

        // Chunk

        this.x = x;
        this.y = y;
        this.z = z;
        this.blocks = new Block[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];
        this.isDirty = false;

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

    public void Render(Vector3Int position) {

        MoveTo(position);
    }

    public void MoveTo(Vector3Int position) {

    }

}
