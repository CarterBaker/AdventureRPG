package application.bootstrap.itempipeline.itemdefinitionmanager;

import java.io.File;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

class PaintedMeshBranch extends BranchPackage {

    /*
     * Builds the mesh a painted item is drawn with from the rigged mesh it was
     * painted as in the mesh tools: every quad at its rest pose, two triangles
     * fanned from its first corner, each corner carrying the quad's flat
     * normal, its own UV and the tile its face is textured with, in the
     * painted item layout every item material's painted twin reads. Its shape,
     * grip and contents still come from the item's sub-voxels; only how it
     * looks comes from here. One drawn copy serves every item naming the same
     * painted mesh.
     */

    // Internal
    private MeshManager meshManager;
    private VAOManager vaoManager;
    private TextureManager textureManager;

    // Directory
    private File meshRoot;

    // Scratch
    private float[] normal;

    // Base \\

    @Override
    protected void create() {

        // Directory
        this.meshRoot = new File(EngineSetting.MESH_PATH);

        // Scratch
        this.normal = new float[EngineSetting.AXIS_COUNT];
    }

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.vaoManager = get(VAOManager.class);
        this.textureManager = get(TextureManager.class);
    }

    // Build \\

    // The drawn copy of the painted mesh, built the first time an item names it
    MeshHandle resolve(String meshPath, String itemName) {

        String handleName = EngineSetting.ITEM_PAINTED_MESH_PREFIX + meshPath;

        if (meshManager.hasMesh(handleName))
            return meshManager.getMeshHandleFromMeshName(handleName);

        ArpgObjectStruct meshArpg = ArpgUtility.loadObject(ArpgUtility.resolveFile(meshRoot, meshPath));

        if (!meshManager.isRiggedMesh(meshArpg))
            return throwException("Item '" + itemName + "' is painted with mesh '" + meshPath
                    + "', which is not a mesh made in the mesh tools.");

        RiggedMeshStruct mesh = meshManager.parseRiggedMesh(meshArpg, meshPath);
        int vertexCount = mesh.getFaceCount() * EngineSetting.QUAD_VERTEX_COUNT;

        if (vertexCount > EngineSetting.MESH_VERT_LIMIT)
            return throwException("Item '" + itemName + "' is painted with mesh '" + meshPath + "', whose "
                    + vertexCount + " vertices pass the mesh limit of " + EngineSetting.MESH_VERT_LIMIT + ".");

        FloatArrayList vertices = new FloatArrayList(vertexCount * EngineSetting.PAINTED_ITEM_VERTEX_STRIDE);
        ShortArrayList indices = new ShortArrayList(mesh.getFaceCount() * EngineSetting.QUAD_INDEX_COUNT);
        int[] faceTiles = new int[mesh.getFaceCount()];

        for (int face = 0; face < mesh.getFaceCount(); face++) {
            faceTiles[face] = textureManager.getTileIDFromTextureName(
                    mesh.getTextureName(mesh.getFaceTexture(face)));
            writeFace(mesh, face, faceTiles[face], vertices, indices);
        }

        VAOHandle vaoTemplate = vaoManager.getVAOHandleFromVAOName(EngineSetting.PAINTED_ITEM_VAO);
        MeshHandle meshHandle = meshManager.createMeshHandle(handleName, vaoTemplate, vertices, indices);

        meshHandle.getTiles().addAll(faceTiles);
        return meshHandle;
    }

    // One face's corners and its triangles: a quad's four and two, a triangle's three and one
    private void writeFace(RiggedMeshStruct mesh, int face, int tile, FloatArrayList vertices, ShortArrayList indices) {

        int base = vertices.size() / EngineSetting.PAINTED_ITEM_VERTEX_STRIDE;

        resolveFaceNormal(mesh, face);

        int corners = mesh.getFaceCornerCount(face);

        for (int corner = 0; corner < corners; corner++) {

            int vertex = mesh.getFaceVertex(face, corner);

            vertices.add(mesh.getX(vertex));
            vertices.add(mesh.getY(vertex));
            vertices.add(mesh.getZ(vertex));
            vertices.add(normal[0]);
            vertices.add(normal[1]);
            vertices.add(normal[2]);
            vertices.add(mesh.getFaceU(face, corner));
            vertices.add(mesh.getFaceV(face, corner));
            vertices.add(tile);
        }

        indices.add((short) base);
        indices.add((short) (base + 1));
        indices.add((short) (base + 2));

        if (corners == EngineSetting.TRIANGLE_VERTEX_COUNT)
            return;

        indices.add((short) (base + 2));
        indices.add((short) (base + 3));
        indices.add((short) base);
    }

    // The face's flat normal, across its two diagonals (a triangle's fourth slot repeating its third)
    private void resolveFaceNormal(RiggedMeshStruct mesh, int face) {

        int a = mesh.getFaceVertex(face, 0);
        int b = mesh.getFaceVertex(face, 1);
        int c = mesh.getFaceVertex(face, 2);
        int d = mesh.getFaceVertex(face, 3);
        float ux = mesh.getX(c) - mesh.getX(a);
        float uy = mesh.getY(c) - mesh.getY(a);
        float uz = mesh.getZ(c) - mesh.getZ(a);
        float vx = mesh.getX(d) - mesh.getX(b);
        float vy = mesh.getY(d) - mesh.getY(b);
        float vz = mesh.getZ(d) - mesh.getZ(b);
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        float scale = length > 0f ? 1f / length : 0f;

        normal[0] = nx * scale;
        normal[1] = ny * scale;
        normal[2] = nz * scale;
    }
}
