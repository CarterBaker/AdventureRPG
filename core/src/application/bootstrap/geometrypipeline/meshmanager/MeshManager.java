package application.bootstrap.geometrypipeline.meshmanager;

import application.bootstrap.geometrypipeline.ibo.IBOHandle;
import application.bootstrap.geometrypipeline.ibo.IBOInstance;
import application.bootstrap.geometrypipeline.ibomanager.IBOManager;
import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.geometrypipeline.vbo.VBOHandle;
import application.bootstrap.geometrypipeline.vbo.VBOInstance;
import application.bootstrap.geometrypipeline.vbomanager.VBOManager;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector3;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class MeshManager extends ManagerPackage {

    /*
     * Central registry for all GPU-resident mesh data. Owns the name-to-ID
     * and ID-to-handle palettes for static bootstrap meshes, drives the mesh
     * load pipeline via MeshLoader, and handles runtime mesh creation,
     * in-place updating, and removal by delegating buffer operations to
     * VAOManager, VBOManager, and IBOManager. createMeshHandle() registers a
     * mesh generated at runtime under a name, exactly like a loaded one.
     */

    // Internal
    private VAOManager vaoManager;
    private VBOManager vboManager;
    private IBOManager iboManager;

    // Palette
    private Object2IntOpenHashMap<String> meshName2MeshID;
    private Int2ObjectOpenHashMap<MeshHandle> meshID2MeshHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.meshName2MeshID = new Object2IntOpenHashMap<>();
        this.meshID2MeshHandle = new Int2ObjectOpenHashMap<>();
        create(MeshLoader.class);
    }

    @Override
    protected void get() {

        // Internal
        this.vaoManager = get(VAOManager.class);
        this.vboManager = get(VBOManager.class);
        this.iboManager = get(IBOManager.class);
    }

    // Management \\

    void addMeshHandle(String meshName, MeshHandle meshHandle) {

        int id = RegistryUtility.toIntID(meshName);

        meshName2MeshID.put(meshName, id);
        meshID2MeshHandle.put(id, meshHandle);
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
        return meshName2MeshID.containsKey(meshName);
    }

    public int getMeshIDFromMeshName(String meshName) {

        if (!meshName2MeshID.containsKey(meshName))
            request(meshName);

        return meshName2MeshID.getInt(meshName);
    }

    public MeshHandle getMeshHandleFromMeshID(int meshID) {
        return meshID2MeshHandle.get(meshID);
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

        if (meshName2MeshID.containsKey(meshName))
            throwException("Mesh \"" + meshName + "\" is already registered.");

        VAOInstance vaoInstance = vaoManager.createVAOInstance(vaoTemplate);
        VBOHandle vboHandle = vboManager.addVBOFromData(meshName, vertices.toFloatArray(), vaoInstance);
        IBOHandle iboHandle = iboManager.addIBOFromData(meshName, indices.toShortArray(), vaoInstance);

        Vector3 boundsMin = new Vector3();
        Vector3 boundsMax = new Vector3();
        computeBounds(vertices, vaoInstance.getVAOData().getVertStride(), boundsMin, boundsMax);

        MeshHandle meshHandle = create(MeshHandle.class);
        meshHandle.constructor(vaoInstance, vboHandle, iboHandle, null, boundsMin, boundsMax);
        addMeshHandle(meshName, meshHandle);

        return meshHandle;
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