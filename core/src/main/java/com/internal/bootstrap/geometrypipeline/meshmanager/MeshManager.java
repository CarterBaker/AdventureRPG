package com.internal.bootstrap.geometrypipeline.meshmanager;

import com.internal.bootstrap.geometrypipeline.ibo.IBOInstance;
import com.internal.bootstrap.geometrypipeline.ibomanager.IBOManager;
import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.bootstrap.geometrypipeline.mesh.MeshInstance;
import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.bootstrap.geometrypipeline.vbo.VBOInstance;
import com.internal.bootstrap.geometrypipeline.vbomanager.VBOManager;
import com.internal.bootstrap.shaderpipeline.texturemanager.TextureHandle;
import com.internal.bootstrap.shaderpipeline.texturemanager.TextureManager;
import com.internal.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class MeshManager extends ManagerPackage {

    // Internal
    private VAOManager vaoManager;
    private VBOManager vboManager;
    private IBOManager iboManager;
    private TextureManager textureManager;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> meshHandleName2MeshHandleID;
    private Int2ObjectOpenHashMap<MeshHandle> meshHandleID2MeshHandle;

    // Base \\

    @Override
    protected void create() {
        create(InternalLoader.class);
        this.meshHandleName2MeshHandleID = new Object2IntOpenHashMap<>();
        this.meshHandleID2MeshHandle = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.vaoManager = get(VAOManager.class);
        this.vboManager = get(VBOManager.class);
        this.iboManager = get(IBOManager.class);
        this.textureManager = get(TextureManager.class);
    }

    // Bootstrap Management \\

    void addMeshHandle(String meshName, int meshID, MeshHandle meshHandle) {
        meshHandleName2MeshHandleID.put(meshName, meshID);
        meshHandleID2MeshHandle.put(meshID, meshHandle);
    }

    // UV Provider \\

    UVProvider createUVProvider() {
        return textureName -> {
            TextureHandle handle = textureManager.getHandleFromTextureName(textureName);
            return new float[] { handle.getU0(), handle.getV0(), handle.getU1(), handle.getV1() };
        };
    }

    // On-Demand Loading \\

    /*
     * Routes an on-demand load through the active InternalLoader.
     * All 4 builders fire synchronously — VAO, VBO, IBO, and mesh handle
     * are all registered by the time this returns.
     */
    public void request(String resourceName) {
        ((InternalLoader) internalLoader).request(resourceName);
    }

    // Static Mesh Accessors \\

    public int getMeshHandleIDFromMeshName(String meshName) {

        if (!meshHandleName2MeshHandleID.containsKey(meshName)) {
            request(meshName);
        }

        return meshHandleName2MeshHandleID.getInt(meshName);
    }

    public MeshHandle getMeshHandleFromMeshHandleID(int meshID) {
        return meshHandleID2MeshHandle.get(meshID);
    }

    // Runtime Mesh Creation \\

    public MeshInstance createMesh(VAOHandle vaoTemplate, FloatArrayList vertices, ShortArrayList indices) {
        VAOInstance vaoInstance = vaoManager.createVAOInstance(vaoTemplate);
        VBOInstance vboInstance = vboManager.createVBOInstance(vaoInstance, vertices);
        IBOInstance iboInstance = iboManager.createIBOInstance(vaoInstance, indices);
        MeshInstance meshInstance = create(MeshInstance.class);
        meshInstance.constructor(vaoInstance, vboInstance, iboInstance);
        return meshInstance;
    }

    public void removeMesh(MeshStruct meshStruct) {
        vaoManager.removeVAOStruct(meshStruct.vaoStruct);
        vboManager.removeVBO(meshStruct.vboStruct);
        iboManager.removeIBO(meshStruct.iboStruct);
    }

    public void removeMesh(MeshHandle meshHandle) {
        vaoManager.removeVAOInstance(meshHandle.getVAOInstance());
        vboManager.removeVBO(meshHandle.getVBOHandle());
        iboManager.removeIBO(meshHandle.getIBOHandle());
    }

    public void removeMesh(MeshInstance meshInstance) {
        vaoManager.removeVAOInstance(meshInstance.getVAOInstance());
        vboManager.removeVBOInstance(meshInstance.getVBOInstance());
        iboManager.removeIBOInstance(meshInstance.getIBOInstance());
    }
}