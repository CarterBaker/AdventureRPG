package application.bootstrap.geometrypipeline.subvoxelmanager;

import com.google.gson.JsonObject;

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
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class SubVoxelManager extends ManagerPackage {

    /*
     * Engine entry point for sub-voxel models. Owns the one geometry path —
     * bootstrap meshes and live editor meshes are built identically here — and
     * is the single access point for sub-voxel raycasting, the mesh format,
     * and converting authored quad meshes into sub-voxel models.
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

        for (int partIndex = 0; partIndex < model.getPartCount(); partIndex++) {

            TextureHandle textureHandle = textureManager.getTextureHandleFromTextureName(
                    model.getPart(partIndex).getTextureName());
            int uvBase = partIndex * 4;

            partUVBounds[uvBase] = textureHandle.getU0();
            partUVBounds[uvBase + 1] = textureHandle.getV0();
            partUVBounds[uvBase + 2] = textureHandle.getU1();
            partUVBounds[uvBase + 3] = textureHandle.getV1();
        }

        return partUVBounds;
    }

    // Runtime Mesh \\

    public MeshInstance createMesh(SubVoxelModelStruct model) {

        FloatArrayList vertices = new FloatArrayList();
        ShortArrayList indices = new ShortArrayList();
        buildGeometry(model, vertices, indices);

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

    public boolean hasSubVoxels(JsonObject meshJson) {
        return SubVoxelJsonUtility.hasSubVoxels(meshJson);
    }

    public SubVoxelModelStruct parseModel(JsonObject meshJson) {
        return SubVoxelJsonUtility.parse(meshJson);
    }

    public JsonObject toMeshJson(SubVoxelModelStruct model) {
        return SubVoxelJsonUtility.toMeshJson(model);
    }

    // Parts \\

    public int findTexturePart(SubVoxelModelStruct model, String textureName) {
        return SubVoxelPartUtility.findTexturePart(model, textureName);
    }

    public int addTexturePart(SubVoxelModelStruct model, String textureName) {
        return SubVoxelPartUtility.addTexturePart(model, textureName);
    }

    // Import \\

    public boolean hasQuads(JsonObject meshJson) {
        return SubVoxelImportUtility.hasQuads(meshJson);
    }

    public SubVoxelModelStruct importQuadMesh(JsonObject meshJson, String fallbackTextureName) {
        return SubVoxelImportUtility.importQuads(meshJson, fallbackTextureName);
    }
}
