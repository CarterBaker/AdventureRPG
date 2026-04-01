package program.bootstrap.geometrypipeline.meshmanager;

import program.bootstrap.geometrypipeline.ibo.IBOInstance;
import program.bootstrap.geometrypipeline.ibomanager.IBOManager;
import program.bootstrap.geometrypipeline.mesh.MeshData;
import program.bootstrap.geometrypipeline.mesh.MeshHandle;
import program.bootstrap.geometrypipeline.mesh.MeshInstance;
import program.bootstrap.geometrypipeline.vao.VAOHandle;
import program.bootstrap.geometrypipeline.vao.VAOInstance;
import program.bootstrap.geometrypipeline.vaomanager.VAOManager;
import program.bootstrap.geometrypipeline.vbo.VBOInstance;
import program.bootstrap.geometrypipeline.vbomanager.VBOManager;
import program.core.engine.ManagerPackage;
import program.core.util.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class MeshManager extends ManagerPackage {

    /*
     * Central registry for all GPU-resident mesh data. Owns the name-to-ID
     * and ID-to-handle palettes for static bootstrap meshes, drives the mesh
     * load pipeline via InternalLoader, and handles runtime mesh creation and
     * removal by delegating buffer operations to VAOManager, VBOManager,
     * and IBOManager.
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
        create(InternalLoader.class);
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

    // Accessible \\

    public void request(String resourceName) {
        ((InternalLoader) internalLoader).request(resourceName);
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