package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.Blocks.Block;
import com.AdventureRPG.WorldSystem.Blocks.BlockAtlas;
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

    // Chunk
    public final Vector3Int coordinate;
    private Block[][][] blocks;

    // Base
    public final Vector3Int position;

    // Model
    private final NeighborChunks NeighborChunks;
    private final BlockAtlas BlockAtlas;
    private ModelInstance mesh;
    private MeshData meshData;

    // Save System
    private boolean isDirty;

    // Base \\

    public Chunk(Vector3Int coordinate, Vector3Int position, WorldSystem WorldSystem) {

        // Chunk
        this.coordinate = coordinate;

        // Base
        this.position = position;

        // Model
        this.NeighborChunks = new NeighborChunks();
        this.BlockAtlas = WorldSystem.BlockAtlas;

        // Save System
        this.isDirty = false;
    }

    public void Generate(Block[][][] blocks) {
        this.blocks = blocks;
    }

    public void MoveTo(Vector3Int position) {
        this.position.set(position);
    }

    public Block getBlock(int localX, int localY, int localZ) {
        return blocks[localX][localY][localZ];
    }

    public void setBlock(int x, int y, int z, Block block) {
        blocks[x][y][z] = block;
        isDirty = true;
        TryBuild();
    }

    // Mesh \\

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
