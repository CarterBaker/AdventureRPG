package com.AdventureRPG.WorldSystem.Chunks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.AdventureRPG.MaterialManager.MaterialData;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;

public final class SubChunkMesh {

    public static final int VERT_POS = 3;
    public static final int VERT_NOR = 3;
    public static final int VERT_COL = 1;
    public static final int VERT_UV0 = 2;
    public static final int VERT_UV1 = 2;
    public static final int VERT_STRIDE = VERT_POS + VERT_NOR + VERT_COL + VERT_UV0 + VERT_UV1;

    private static final VertexAttributes ATTRS = new VertexAttributes(

            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
            new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "a_color"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord1"));

    private final AtomicReference<SubChunkPacket> pending = new AtomicReference<>(null);

    // GPU resources for current build
    private final List<Mesh> meshes = new ArrayList<>();
    private final List<NodePart> nodeParts = new ArrayList<>();

    private final WorldSystem world;

    // Base \\

    public SubChunkMesh(WorldSystem world) {

        this.world = world;
    }

    public void dispose() {
        clearGPU();
    }

    // Build \\

    public void submit(SubChunkPacket packet) {
        pending.set(packet);
    }

    public void build(Node node) {

        SubChunkPacket packet = pending.getAndSet(null);

        if (packet == null)
            return;

        // Rebuild fresh GPU resources
        clearGPU();
        node.parts.clear();

        // For each material batch: create Mesh, upload, make NodePart
        packet.batches.forEach((matId, batch) -> {

            Mesh mesh = new Mesh(true, batch.vertexCount, batch.indexCount, ATTRS);
            mesh.setVertices(batch.vertices);
            mesh.setIndices(batch.indices);

            meshes.add(mesh);

            // Resolve material from materialId (Option B).
            MaterialData md = world.materialManager.getById(matId);
            Material mat = (md != null) ? md.material : new Material(); // fallback

            NodePart np = new NodePart();
            np.meshPart.set("", mesh, 0, batch.indexCount, com.badlogic.gdx.graphics.GL20.GL_TRIANGLES);
            np.material = mat;

            node.parts.add(np);
            nodeParts.add(np);
        });
    }

    private void clearGPU() {

        for (NodePart np : nodeParts)
            np.meshPart.mesh = null;

        nodeParts.clear();

        for (Mesh m : meshes) {

            try {
                m.dispose();
            }

            catch (Exception ignored) {
            }
        }

        meshes.clear();
    }
}
