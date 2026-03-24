package com.internal.bootstrap.geometrypipeline.meshmanager;

import com.internal.bootstrap.geometrypipeline.ibo.IBOInstance;
import com.internal.bootstrap.geometrypipeline.ibomanager.IBOManager;
import com.internal.bootstrap.geometrypipeline.mesh.MeshData;
import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.bootstrap.geometrypipeline.mesh.MeshInstance;
import com.internal.bootstrap.geometrypipeline.vao.VAOData;
import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.bootstrap.geometrypipeline.vbo.VBOInstance;
import com.internal.bootstrap.geometrypipeline.vbomanager.VBOManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.RegistryUtility;
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
     *
     * All meshes store VAOHandle templates — actual VAO instances are created
     * per-window at render time. Runtime mesh creation uses a temporary VAO
     * instance for VBO/IBO upload, then discards it and stores only the template.
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
            VAOHandle vaoHandle,
            FloatArrayList vertices,
            ShortArrayList indices) {

        VAOData vaoData = vaoHandle.getVAOData();
        VBOInstance vboInstance = vboManager.createVBOInstance(vaoData, vertices);
        IBOInstance iboInstance = iboManager.createIBOInstance(vaoData, indices);

        MeshInstance meshInstance = create(MeshInstance.class);
        meshInstance.constructor(vaoHandle, vboInstance, iboInstance);

        return meshInstance;
    }

    // Removal \\

    public void removeMesh(MeshData meshData) {
        vboManager.removeVBO(meshData.getVBOData());
        iboManager.removeIBO(meshData.getIBOData());
    }

    public void removeMesh(MeshHandle meshHandle) {
        vboManager.removeVBO(meshHandle.getVBOHandle());
        iboManager.removeIBO(meshHandle.getIBOHandle());
    }

    public void removeMesh(MeshInstance meshInstance) {
        vboManager.removeVBOInstance(meshInstance.getVBOInstance());
        iboManager.removeIBOInstance(meshInstance.getIBOInstance());
    }
}