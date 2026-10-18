package application.bootstrap.geometrypipeline.meshmanager;

import application.bootstrap.geometrypipeline.ibo.IBOHandle;
import application.bootstrap.geometrypipeline.ibo.IBOInstance;
import application.bootstrap.geometrypipeline.ibomanager.IBOManager;
import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.geometrypipeline.vbo.VBOHandle;
import application.bootstrap.geometrypipeline.vbo.VBOInstance;
import application.bootstrap.geometrypipeline.vbomanager.VBOManager;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.mathematics.vectors.Vector3;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class MeshManager extends ManagerPackage {

    /*
     * Central registry for all GPU-resident mesh data. Owns the name-to-ID
     * and ID-to-handle palettes, with IDs assigned in registration order,
     * drives the mesh load pipeline via MeshLoader, and handles runtime mesh
     * creation, in-place updating, and removal by delegating buffer
     * operations to VAOManager, VBOManager, and IBOManager. createMeshHandle()
     * registers a mesh generated at runtime under a name, exactly like a
     * loaded one. Rigged meshes pass through RiggedMeshStruct: parsed from
     * and written to ARPG, built into skinned vertex data with one reusable
     * scratch, and rebuilt in place on their registered handle when an editor
     * changes one, so every view drawing it shows the change at once.
     */

    // Internal
    private VAOManager vaoManager;
    private VBOManager vboManager;
    private IBOManager iboManager;
    private RigManager rigManager;
    private TextureManager textureManager;

    // Rigged
    private RiggedMeshScratchStruct riggedScratch;
    private FloatArrayList riggedVertices;
    private ShortArrayList riggedIndices;

    // Palette
    private Object2IntOpenHashMap<String> meshName2MeshID;
    private ObjectArrayList<MeshHandle> meshID2MeshHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.meshName2MeshID = RegistryUtility.createNameIndex();
        this.meshID2MeshHandle = RegistryUtility.createPalette();

        // Rigged
        this.riggedScratch = new RiggedMeshScratchStruct();
        this.riggedVertices = new FloatArrayList();
        this.riggedIndices = new ShortArrayList();
        create(MeshLoader.class);
    }

    @Override
    protected void get() {

        // Internal
        this.vaoManager = get(VAOManager.class);
        this.vboManager = get(VBOManager.class);
        this.iboManager = get(IBOManager.class);
        this.rigManager = get(RigManager.class);
        this.textureManager = get(TextureManager.class);
    }

    // Management \\

    void addMeshHandle(String meshName, MeshHandle meshHandle) {
        RegistryUtility.registerHandle(
                meshName2MeshID, meshID2MeshHandle, meshName, meshHandle, EngineSetting.REGISTRY_INT_ID_COUNT);
    }

    // Bounds \\

    void computeBounds(FloatArrayList vertices, int vertStride, Vector3 outMin, Vector3 outMax) {

        int vertexCount = vertices.size() / vertStride;

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;

        for (int i = 0; i < vertexCount; i++) {

            int base = i * vertStride;

            float x = vertices.getFloat(base);
            float y = vertices.getFloat(base + 1);
            float z = vertices.getFloat(base + 2);

            if (x < minX)
                minX = x;
            if (y < minY)
                minY = y;
            if (z < minZ)
                minZ = z;
            if (x > maxX)
                maxX = x;
            if (y > maxY)
                maxY = y;
            if (z > maxZ)
                maxZ = z;
        }

        outMin.set(minX, minY, minZ);
        outMax.set(maxX, maxY, maxZ);
    }

    // Accessible \\

    public void request(String resourceName) {
        ((MeshLoader) internalLoader).request(resourceName);
    }

    public boolean hasMesh(String meshName) {
        return RegistryUtility.getHandle(meshName2MeshID, meshID2MeshHandle, meshName) != null;
    }

    public int getMeshIDFromMeshName(String meshName) {

        if (!hasMesh(meshName))
            request(meshName);

        return meshName2MeshID.getInt(meshName);
    }

    public MeshHandle getMeshHandleFromMeshID(int meshID) {
        return RegistryUtility.getHandle(meshID2MeshHandle, meshID);
    }

    public MeshHandle getMeshHandleFromMeshName(String meshName) {
        return getMeshHandleFromMeshID(getMeshIDFromMeshName(meshName));
    }

    // Runtime Mesh Creation \\

    public MeshInstance createMesh(
            VAOHandle vaoTemplate,
            FloatArrayList vertices,
            ShortArrayList indices) {

        VAOInstance vaoInstance = vaoManager.createVAOInstance(vaoTemplate);
        VBOInstance vboInstance = vboManager.createVBOInstance(vaoInstance, vertices);
        IBOInstance iboInstance = iboManager.createIBOInstance(vaoInstance, indices);

        MeshInstance meshInstance = create(MeshInstance.class);
        meshInstance.constructor(vaoInstance, vboInstance, iboInstance);

        return meshInstance;
    }

    public void updateMesh(MeshInstance meshInstance, FloatArrayList vertices, ShortArrayList indices) {

        VAOInstance vaoInstance = meshInstance.getVAOInstance();
        VBOInstance vboInstance = vboManager.updateVBOInstance(vaoInstance, meshInstance.getVBOInstance(), vertices);
        IBOInstance iboInstance = iboManager.updateIBOInstance(meshInstance.getIBOInstance(), indices);

        meshInstance.constructor(vaoInstance, vboInstance, iboInstance);
    }

    public MeshHandle createMeshHandle(
            String meshName,
            VAOHandle vaoTemplate,
            FloatArrayList vertices,
            ShortArrayList indices) {

        if (hasMesh(meshName))
            throwException("Mesh \"" + meshName + "\" is already registered.");

        VAOInstance vaoInstance = vaoManager.createVAOInstance(vaoTemplate);
        VBOHandle vboHandle = vboManager.addVBOFromData(meshName, vertices.toFloatArray(), vaoInstance);
        IBOHandle iboHandle = iboManager.addIBOFromData(meshName, indices.toShortArray(), vaoInstance);

        Vector3 boundsMin = new Vector3();
        Vector3 boundsMax = new Vector3();
        computeBounds(vertices, vaoInstance.getVAOData().getVertStride(), boundsMin, boundsMax);

        MeshHandle meshHandle = create(MeshHandle.class);
        meshHandle.constructor(meshName, vaoInstance, vboHandle, iboHandle, null, boundsMin, boundsMax);
        addMeshHandle(meshName, meshHandle);

        return meshHandle;
    }

    // Rigged Meshes \\

    public boolean isRiggedMesh(ArpgObjectStruct meshArpg) {
        return RiggedMeshArpgUtility.isRigged(meshArpg);
    }

    public RiggedMeshStruct parseRiggedMesh(ArpgObjectStruct meshArpg, String sourceName) {

        RigHandle rigHandle = rigManager.getRigHandleFromRigName(RiggedMeshArpgUtility.getRigName(meshArpg));
        return RiggedMeshArpgUtility.parse(meshArpg, rigHandle, sourceName);
    }

    // The source file with its quad entries rewritten from the mesh, every other field kept
    public ArpgObjectStruct toRiggedMeshArpg(RiggedMeshStruct mesh, ArpgObjectStruct sourceArpg) {
        return RiggedMeshArpgUtility.write(mesh, sourceArpg);
    }

    public boolean fitsRiggedMeshLimit(RiggedMeshStruct mesh) {
        return mesh.getFaceCount() * EngineSetting.QUAD_VERTEX_COUNT <= EngineSetting.MESH_VERT_LIMIT;
    }

    void buildRiggedGeometry(RiggedMeshStruct mesh, FloatArrayList vertices, ShortArrayList indices) {
        RiggedMeshGeometryUtility.build(mesh, resolveTextureTiles(mesh), riggedScratch, vertices, indices);
    }

    // Rebuilds a registered rigged mesh from a struct, its buffers respecified in place on the same handles
    public void updateRiggedMesh(MeshHandle meshHandle, RiggedMeshStruct mesh) {

        if (meshHandle.getRigHandle() != mesh.getRigHandle())
            throwException("Mesh \"" + meshHandle.getMeshName() + "\" cannot be rebuilt on a different rig.");

        buildRiggedGeometry(mesh, riggedVertices, riggedIndices);

        VAOInstance vaoInstance = meshHandle.getVAOInstance();
        VBOHandle vboHandle = vboManager.updateVBOHandle(vaoInstance, meshHandle.getVBOHandle(), riggedVertices);
        IBOHandle iboHandle = iboManager.updateIBOHandle(meshHandle.getIBOHandle(), riggedIndices);
        Vector3 boundsMin = new Vector3();
        Vector3 boundsMax = new Vector3();

        computeBounds(riggedVertices, vaoInstance.getVAOData().getVertStride(), boundsMin, boundsMax);
        meshHandle.constructor(
                meshHandle.getMeshName(),
                vaoInstance,
                vboHandle,
                iboHandle,
                meshHandle.getRigHandle(),
                boundsMin,
                boundsMax);
    }

    // Each palette texture's tile as u0, u1, v0, v1, width and height, in the scratch's reusable array
    private float[] resolveTextureTiles(RiggedMeshStruct mesh) {

        int floatCount = mesh.getTextureCount() * EngineSetting.RIGGED_MESH_TILE_FLOATS;

        if (riggedScratch.textureTiles.length < floatCount)
            riggedScratch.textureTiles = new float[floatCount];

        float[] tiles = riggedScratch.textureTiles;

        for (int t = 0; t < mesh.getTextureCount(); t++) {

            TextureHandle textureHandle = textureManager.getTextureHandleFromTextureName(mesh.getTextureName(t));
            int base = t * EngineSetting.RIGGED_MESH_TILE_FLOATS;

            tiles[base] = textureHandle.getU0();
            tiles[base + 1] = textureHandle.getU1();
            tiles[base + 2] = textureHandle.getV0();
            tiles[base + 3] = textureHandle.getV1();
            tiles[base + 4] = textureHandle.getTileWidth();
            tiles[base + 5] = textureHandle.getTileHeight();
        }

        return tiles;
    }

    // Removal \\

    public void removeMesh(MeshData meshData) {
        vaoManager.removeSourceVAOClones(meshData.getAttributeHandle());
        vaoManager.removeVAOData(meshData.getVAOData());
        vboManager.removeVBO(meshData.getVBOData());
        iboManager.removeIBO(meshData.getIBOData());
    }

    public void removeMesh(MeshHandle meshHandle) {
        removeMesh(meshHandle.getMeshData());
    }

    public void removeMesh(MeshInstance meshInstance) {
        removeMesh(meshInstance.getMeshData());
    }
}