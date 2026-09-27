package application.bootstrap.geometrypipeline.subvoxelmanager;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelHitStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector3Int;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class SubVoxelManager extends ManagerPackage {

    /*
     * Engine entry point for sub-voxel models. Owns the one geometry path —
     * bootstrap meshes and live editor meshes are built identically here — and
     * is the single access point for sub-voxel raycasting, the mesh format,
     * and converting authored quad meshes into sub-voxel models. Also builds a
     * pocket's open box of walls, which may span more than one block.
     */

    // Internal
    private TextureManager textureManager;
    private VAOManager vaoManager;
    private MeshManager meshManager;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.textureManager = get(TextureManager.class);
        this.vaoManager = get(VAOManager.class);
        this.meshManager = get(MeshManager.class);
    }

    // Geometry \\

    public int getVertexCount(SubVoxelModelStruct model) {
        return SubVoxelMeshUtility.build(model, null, null, null) * EngineSetting.QUAD_VERTEX_COUNT;
    }

    public boolean fitsMeshLimit(SubVoxelModelStruct model) {
        return getVertexCount(model) <= EngineSetting.MESH_VERT_LIMIT;
    }

    public void buildGeometry(
            SubVoxelModelStruct model,
            FloatArrayList vertices,
            ShortArrayList indices) {

        if (!fitsMeshLimit(model))
            throwException("Sub-voxel model needs " + getVertexCount(model) + " vertices, over the mesh limit of "
                    + EngineSetting.MESH_VERT_LIMIT + ".");

        vertices.clear();
        indices.clear();

        SubVoxelMeshUtility.build(model, resolvePartUVBounds(model), vertices, indices);
    }

    private float[] resolvePartUVBounds(SubVoxelModelStruct model) {

        float[] partUVBounds = new float[model.getPartCount() * 4];

        for (int partIndex = 0; partIndex < model.getPartCount(); partIndex++)
            writeUVBounds(model.getPart(partIndex).getTextureName(), partUVBounds, partIndex * 4);

        return partUVBounds;
    }

    private void writeUVBounds(String textureName, float[] uvBounds, int uvBase) {

        TextureHandle textureHandle = textureManager.getTextureHandleFromTextureName(textureName);

        uvBounds[uvBase] = textureHandle.getU0();
        uvBounds[uvBase + 1] = textureHandle.getV0();
        uvBounds[uvBase + 2] = textureHandle.getU1();
        uvBounds[uvBase + 3] = textureHandle.getV1();
    }

    // Runtime Mesh \\

    public MeshInstance createMesh(SubVoxelModelStruct model) {

        FloatArrayList vertices = new FloatArrayList();
        ShortArrayList indices = new ShortArrayList();
        buildGeometry(model, vertices, indices);

        VAOHandle vaoTemplate = vaoManager.getVAOHandleFromVAOName(EngineSetting.SUB_VOXEL_VAO);
        return meshManager.createMesh(vaoTemplate, vertices, indices);
    }

    // A pocket's open box around a space of the given size in sub-voxels, its corner at the origin
    public MeshInstance createPocketMesh(Vector3Int size, String textureName) {

        FloatArrayList vertices = new FloatArrayList();
        ShortArrayList indices = new ShortArrayList();
        float[] uvBounds = new float[4];

        writeUVBounds(textureName, uvBounds, 0);
        SubVoxelMeshUtility.buildPocket(size.x, size.y, size.z, uvBounds, vertices, indices);

        VAOHandle vaoTemplate = vaoManager.getVAOHandleFromVAOName(EngineSetting.SUB_VOXEL_VAO);
        return meshManager.createMesh(vaoTemplate, vertices, indices);
    }

    public void updateMesh(MeshInstance meshInstance, SubVoxelModelStruct model) {

        FloatArrayList vertices = new FloatArrayList();
        ShortArrayList indices = new ShortArrayList();
        buildGeometry(model, vertices, indices);

        meshManager.updateMesh(meshInstance, vertices, indices);
    }

    // Raycast \\

    public boolean raycast(
            SubVoxelModelStruct model,
            Vector3 origin,
            Vector3 direction,
            SubVoxelHitStruct hit) {
        return SubVoxelRaycastUtility.raycast(model, origin, direction, hit);
    }

    // Format \\

    public boolean hasSubVoxels(ArpgObjectStruct meshArpg) {
        return SubVoxelArpgUtility.hasSubVoxels(meshArpg);
    }

    public SubVoxelModelStruct parseModel(ArpgObjectStruct meshArpg) {
        return SubVoxelArpgUtility.parse(meshArpg);
    }

    public ArpgObjectStruct toMeshArpg(SubVoxelModelStruct model) {
        return SubVoxelArpgUtility.toMeshArpg(model);
    }

    // Parts \\

    public int findTexturePart(SubVoxelModelStruct model, String textureName) {
        return SubVoxelPartUtility.findTexturePart(model, textureName);
    }

    public int addTexturePart(SubVoxelModelStruct model, String textureName) {
        return SubVoxelPartUtility.addTexturePart(model, textureName);
    }

    // Import \\

    public boolean hasQuads(ArpgObjectStruct meshArpg) {
        return SubVoxelImportUtility.hasQuads(meshArpg);
    }

    public SubVoxelModelStruct importQuadMesh(ArpgObjectStruct meshArpg, String fallbackTextureName) {
        return SubVoxelImportUtility.importQuads(meshArpg, fallbackTextureName);
    }

    // Either format as sub-voxels — null when the mesh holds neither cubes nor quads
    public SubVoxelModelStruct resolveModel(ArpgObjectStruct meshArpg, String fallbackTextureName) {

        if (hasSubVoxels(meshArpg))
            return parseModel(meshArpg);

        if (hasQuads(meshArpg))
            return importQuadMesh(meshArpg, fallbackTextureName);

        return null;
    }
}
