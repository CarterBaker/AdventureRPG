package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.Blocks.Block;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

public class Chunk {

    // Base
    private final Settings settings;
    private final ChunkSystem ChunkSystem;
    private ModelInstance mesh;
    public Vector3Int position;

    // Chunk
    public final Vector3Int coordinate;
    private Block[][][] blocks;
    private boolean isDirty;

    // Settings
    private final int CHUNK_SIZE;

    // Temp
    private Vector3 newPosition;
    private NeighborChunks NeighborChunks;

    public Chunk(Vector3Int coordinate, Vector3Int position, ChunkSystem ChunkSystem) {

        // Base
        this.settings = ChunkSystem.settings;
        this.ChunkSystem = ChunkSystem;
        this.CHUNK_SIZE = settings.CHUNK_SIZE;
        this.position = position;

        // Chunk
        this.coordinate = coordinate;
        this.isDirty = false;

        // Temp
        this.newPosition = new Vector3();
        this.NeighborChunks = ChunkSystem.GetNearbyChunks(coordinate);

        Build(position);
    }

    public void Generate(Block[][][] blocks) {
        this.blocks = blocks;
    }

    public ModelInstance getMesh() {
        return mesh;
    }

    public Block getBlock(int localX, int localY, int localZ) {
        return blocks[localX][localY][localZ];
    }

    public void setBlock(int x, int y, int z, Block block) {
        blocks[x][y][z] = block;
        isDirty = true;
        Build(position);
    }

    public boolean needsSaving() {
        return isDirty;
    }

    public void markClean() {
        isDirty = false;
    }

    // Mesh \\

    public boolean Build(Vector3Int position) {

        if (!NeighborChunks.isValid()) {

        }

        mesh = ChunkBuilder.build(this);

        MoveTo(position);

        return mesh != null;
    }

    private void Destroy() {
        this.mesh = ChunkBuilder.destroy(this);
    }

    public void MoveTo(Vector3Int position) {

        if (mesh == null)
            return;

        this.position.set(position.x, position.y, position.z);
        this.newPosition.set(position.x, position.y, position.z);

        mesh.transform.setTranslation(newPosition);

        NeighborChunks = ChunkSystem.SetNearbyChunks(coordinate, NeighborChunks);
    }
}
