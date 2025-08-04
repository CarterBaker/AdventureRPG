package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.Util.MeshData;
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
import com.badlogic.gdx.math.Vector3;

public class Chunk {

    // Base
    private final BlockAtlas BlockAtlas;
    private final ChunkSystem ChunkSystem;
    private ModelInstance mesh;
    public Vector3Int position;

    // Chunk
    public final Vector3Int coordinate;
    private Block[][][] blocks;
    private boolean isDirty;

    // Temp
    private MeshData meshData;
    private Vector3 newPosition;
    private NeighborChunks NeighborChunks;

    public Chunk(Vector3Int coordinate, Vector3Int position, WorldSystem WorldSystem) {

        // Base
        this.BlockAtlas = WorldSystem.BlockAtlas;
        this.ChunkSystem = WorldSystem.ChunkSystem;
        this.position = position;

        // Chunk
        this.coordinate = coordinate;
        this.isDirty = false;

        // Temp
        this.meshData = new MeshData();
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
            mesh = null;
            return false;
        }

        meshData = ChunkBuilder.build(BlockAtlas, meshData, this);
        mesh = buildModelInstanceFromData(meshData);

        MoveTo(position);

        return mesh != null;
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

    public void MoveTo(Vector3Int position) {

        if (mesh == null)
            return;

        this.position.set(position.x, position.y, position.z);
        this.newPosition.set(position.x, position.y, position.z);

        mesh.transform.setTranslation(newPosition);

        NeighborChunks = ChunkSystem.SetNearbyChunks(coordinate, NeighborChunks);
    }
}
