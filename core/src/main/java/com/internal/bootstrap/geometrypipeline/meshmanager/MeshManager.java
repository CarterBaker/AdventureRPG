package com.internal.bootstrap.geometrypipeline.meshmanager;

import com.internal.bootstrap.geometrypipeline.ibomanager.IBOHandle;
import com.internal.bootstrap.geometrypipeline.ibomanager.IBOManager;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.bootstrap.geometrypipeline.vbomanager.VBOHandle;
import com.internal.bootstrap.geometrypipeline.vbomanager.VBOManager;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class MeshManager extends ManagerPackage {

    // Internal
    private InternalLoadManager internalLoadManager;

    private VAOManager vaoManager;
    private VBOManager vboManager;
    private IBOManager iboManager;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> meshHandleName2MeshHandleID;
    private Int2ObjectOpenHashMap<MeshHandle> meshHandleID2MeshHandle;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalLoadManager = create(InternalLoadManager.class);

        // Retrieval Mapping
        this.meshHandleName2MeshHandleID = new Object2IntOpenHashMap<>();
        this.meshHandleID2MeshHandle = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {

        // Internal
        this.vaoManager = get(VAOManager.class);
        this.vboManager = get(VBOManager.class);
        this.iboManager = get(IBOManager.class);
    }

    @Override
    protected void awake() {
        compileMeshData();
    }

    @Override
    protected void release() {
        internalLoadManager = release(InternalLoadManager.class);
    }

    // Model Management \\

    void compileMeshData() {
        internalLoadManager.loadMeshData();
    }

    void addMeshHandle(String meshName, int meshID, MeshHandle meshHandle) {
        meshHandleName2MeshHandleID.put(meshName, meshID);
        meshHandleID2MeshHandle.put(meshID, meshHandle);
    }

    // Accessible \\

    public int getMeshHandleIDFromMeshName(String meshName) {
        return meshHandleName2MeshHandleID.getInt(meshName);
    }

    public MeshHandle getMeshHandleFromMeshHandleID(int meshID) {
        return meshHandleID2MeshHandle.get(meshID);
    }

    public VBOHandle createVBO(
            VAOHandle vaoHandle,
            FloatArrayList vertices) {
        return vboManager.createVBO(
                vaoHandle,
                vertices);
    }

    public IBOHandle createIBO(
            VAOHandle vaoHandle,
            ShortArrayList indices) {
        return createIBO(
                vaoHandle,
                indices);
    }

    public MeshHandle createMesh(
            VAOHandle vaoHandle,
            FloatArrayList vertices,
            ShortArrayList indices) {

        VBOHandle vboHandle = createVBO(
                vaoHandle,
                vertices);

        IBOHandle iboHandle = createIBO(
                vaoHandle,
                indices);

        MeshHandle meshHandle = create(MeshHandle.class);
        meshHandle.constructor(
                vaoHandle,
                vboHandle,
                iboHandle);

        return meshHandle;
    }

    public MeshHandle createMesh(
            VAOHandle vaoHandle,
            VBOHandle vboHandle,
            IBOHandle iboHandle) {

        MeshHandle meshHandle = create(MeshHandle.class);
        meshHandle.constructor(
                vaoHandle,
                vboHandle,
                iboHandle);

        return meshHandle;
    }
}
