package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.Blocks.Block;
import com.AdventureRPG.WorldSystem.Blocks.BlockAtlas;
import com.AdventureRPG.WorldSystem.Blocks.BlockData;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

public class Chunk {

    // Game Manager
    private final WorldSystem worldSystem;
    private final BlockAtlas BlockAtlas;

    // Chunk
    public final Vector3Int coordinate;
    public final Vector3Int position;
    private final NeighborChunks NeighborChunks;
    private BlockData[][][] blocks;

    // Model
    private ModelInstance mesh;
    private MeshData meshData;

    // Save System
    private boolean isDirty;

    // Base \\

    public Chunk(WorldSystem worldSystem, Vector3Int coordinate, Vector3Int position) {

        // Game Manager
        this.worldSystem = worldSystem;
        this.BlockAtlas = worldSystem.BlockAtlas;

        // Chunk
        this.coordinate = coordinate;
        this.position = position;
        this.NeighborChunks = new NeighborChunks();

        // Save System
        this.isDirty = false;
    }

    // Chunk \\

    public void Generate(BlockData[][][] blocks) {
        this.blocks = blocks;
    }

    public void MoveTo(Vector3Int position) {
        this.position.set(position);
    }

    public BlockData getBlockData(int localX, int localY, int localZ) {
        return blocks[localX][localY][localZ];
    }

    public Block getBlock(int localX, int localY, int localZ) {
        return worldSystem.GetBlockByID(blocks[localX][localY][localZ].blockID);
    }

    public void PlaceBlock(int x, int y, int z, int blockID) {
        blocks[x][y][z].PlaceBlock(blockID);
        isDirty = true;
        TryBuild();
    }

    public void BreakBlock(int x, int y, int z) {
        blocks[x][y][z].BreakBlock();
        isDirty = true;
        TryBuild();
    }

    // Model \\

    public ModelInstance getMesh() {
        return mesh;
    }

    public NeighborChunks getNeighbors() {
        return NeighborChunks;
    }

    public boolean TryBuild() {

        if (!NeighborChunks.isValid()) {
            mesh = null;
            return false;
        }

        meshData = ChunkBuilder.build(BlockAtlas, meshData, this);
        mesh = buildModelInstanceFromData(meshData);

        return true;
    }

    private ModelInstance buildModelInstanceFromData(MeshData data) {

        if (data.vertices.isEmpty() || data.indices.isEmpty())
            return null;

        int vertexCount = data.vertices.size() / 3;

        float[] vertexArray = new float[vertexCount * 5];
        for (int i = 0, vi = 0, ui = 0; i < vertexCount; i++) {
            vertexArray[i * 5] = data.vertices.get(vi++);
            vertexArray[i * 5 + 1] = data.vertices.get(vi++);
            vertexArray[i * 5 + 2] = data.vertices.get(vi++);
            vertexArray[i * 5 + 3] = data.uvs.get(ui++);
            vertexArray[i * 5 + 4] = data.uvs.get(ui++);
        }

        short[] indexArray = new short[data.indices.size()];
        for (int i = 0; i < indexArray.length; i++) {
            indexArray[i] = data.indices.get(i).shortValue();
        }

        Mesh mesh = new Mesh(true, vertexCount, indexArray.length,
                new VertexAttribute(Usage.Position, 3, "a_position"),
                new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord0"));

        mesh.setVertices(vertexArray);
        mesh.setIndices(indexArray);

        Material material = new Material(TextureAttribute.createDiffuse(BlockAtlas.getTexture()));

        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        modelBuilder.part("chunk", mesh, GL20.GL_TRIANGLES, material);
        Model model = modelBuilder.end();

        return new ModelInstance(model);
    }

    // Save System \\

    public boolean needsSaving() {
        return isDirty;
    }

    public void markClean() {
        isDirty = false;
    }
}
