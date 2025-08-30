package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.WorldSystem.Biomes.BiomeContainer;
import com.AdventureRPG.WorldSystem.Blocks.BlockContainer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;

public final class SubChunk {

    // Data
    private final BiomeContainer biomes;
    private final BlockContainer blocks;

    // Mesh
    public final SubChunkMesh subChunkMesh;

    // Utility
    private final int biomeShift;

    // Base \\

    public SubChunk(Chunk chunk) {

        // Settings
        int CHUNK_SIZE = chunk.settings.CHUNK_SIZE;
        int BIOME_SIZE = chunk.settings.BIOME_SIZE;

        // Data
        this.biomes = new BiomeContainer(CHUNK_SIZE / BIOME_SIZE);
        this.blocks = new BlockContainer(CHUNK_SIZE);

        // Mesh
        this.subChunkMesh = new SubChunkMesh();

        // Utility
        this.biomeShift = Integer.numberOfTrailingZeros(CHUNK_SIZE / BIOME_SIZE);
    }

    // Data\\

    // Block access
    public short getBlock(int x, int y, int z) {
        return blocks.get(x, y, z);
    }

    public void setBlock(int x, int y, int z, short id) {
        blocks.set(x, y, z, id);
    }

    // Biome access
    public short getBiome(int x, int y, int z) {
        return biomes.get(x >> biomeShift, y >> biomeShift, z >> biomeShift);
    }

    public void setBiome(int x, int y, int z, short id) {
        biomes.set(x, y, z, id);
    }

    // Mesh \\

    public void build() {
        subChunkMesh.build();
    }

    public void assignMeshToModel(Model model) {

        int nodeIndex = 0;

        for (SubChunkMesh.RenderBatch batch : subChunkMesh.getRenderBatches()) {

            if (batch.mesh == null)
                continue;

            // Create MeshPart
            MeshPart meshPart = new MeshPart();
            meshPart.id = "subchunk_part_" + nodeIndex++;
            meshPart.mesh = batch.mesh;
            meshPart.offset = 0;
            meshPart.size = batch.mesh.getNumIndices();
            meshPart.primitiveType = GL20.GL_TRIANGLES;

            // Create NodePart
            NodePart nodePart = new NodePart(meshPart, batch.material);

            // Hook shader/texture array if your pipeline uses them outside Material
            // (LibGDX's default Renderable pipeline usually only needs Material+MeshPart)
            // If you need custom ShaderProgram, you'd handle it in your own
            // RenderableProvider.

            // Create Node and attach
            Node node = new Node();
            node.id = "subchunk_node_" + nodeIndex;
            node.parts.add(nodePart);

            model.nodes.add(node);
        }

        // Rebuild model's internal references
        model.calculateTransforms();
    }

}
