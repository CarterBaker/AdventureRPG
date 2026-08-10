package application.bootstrap.geometrypipeline.meshmanager;

import application.bootstrap.geometrypipeline.ibo.IBOInstance;
import application.bootstrap.geometrypipeline.ibomanager.IBOManager;
import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.geometrypipeline.vbo.VBOInstance;
import application.bootstrap.geometrypipeline.vbomanager.VBOManager;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class MeshManager extends ManagerPackage {

    /*
     * Central registry for all GPU-resident mesh data. Owns the name-to-ID
     * and ID-to-handle palettes for static bootstrap meshes, drives the mesh
     * load pipeline via InternalLoader, and handles runtime mesh creation,
     * in-place updating, and removal by delegating buffer operations to
     * VAOManager, VBOManager, and IBOManager.
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

    /*
     * Reuploads vertex and index data into an EXISTING MeshInstance's GPU
     * buffers, keeping the same VAO/VBO/IBO handles. Used for geometry that
     * changes shape but not identity — a streamed chunk's merged packet
     * being rebuilt after a block edit or liquid flow — so repeated updates
     * never pay for GL object allocation or per-window VAO clone rebuilding.
     */
    public void updateMesh(MeshInstance meshInstance, FloatArrayList vertices, ShortArrayList indices) {

        VAOInstance vaoInstance = meshInstance.getVAOInstance();
        VBOInstance vboInstance = vboManager.updateVBOInstance(vaoInstance, meshInstance.getVBOInstance(), vertices);
        IBOInstance iboInstance = iboManager.updateIBOInstance(meshInstance.getIBOInstance(), indices);

        meshInstance.constructor(vaoInstance, vboInstance, iboInstance);
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