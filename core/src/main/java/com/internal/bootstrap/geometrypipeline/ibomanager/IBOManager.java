package com.internal.bootstrap.geometrypipeline.ibomanager;

import com.internal.bootstrap.geometrypipeline.ibo.IBOHandle;
import com.internal.bootstrap.geometrypipeline.ibo.IBOInstance;
import com.internal.bootstrap.geometrypipeline.ibo.IBOStruct;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshManager;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class IBOManager extends ManagerPackage {

    // Internal
    private MeshManager meshManager;

    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, IBOHandle> iboName2IBOHandle;

    // Base \\

    @Override
    protected void create() {
        this.iboName2IBOHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
    }

    // Handle Registration \\

    void registerIBO(String resourceName, IBOHandle handle) {
        iboName2IBOHandle.put(resourceName, handle);
    }

    /*
     * Bypasses JSON parsing — used when index data was assembled by quad
     * expansion inside the mesh builder.
     */
    public IBOHandle addIBOFromData(String resourceName, short[] indices, VAOInstance vaoInstance) {
        IBOHandle handle = GLSLUtility.uploadIndexData(vaoInstance, create(IBOHandle.class), indices);
        iboName2IBOHandle.put(resourceName, handle);
        return handle;
    }

    // Accessible \\

    /*
     * Pure registry lookup — no load trigger. Safe to call from inside any
     * builder that is already executing within a load() call.
     */
    public boolean hasIBO(String iboName) {
        return iboName2IBOHandle.containsKey(iboName);
    }

    /*
     * Direct registry lookup — no load trigger. Safe to call from inside any
     * builder that is already executing within a load() call.
     */
    public IBOHandle getIBOHandleDirect(String iboName) {
        return iboName2IBOHandle.get(iboName);
    }

    /*
     * Auto-triggers a full mesh load on miss via MeshManager.
     * Safe for external callers only — never call from inside a builder
     * that is itself invoked by the mesh loader or you will recurse infinitely.
     */
    public IBOHandle getIBOHandleFromName(String iboName) {
        IBOHandle handle = iboName2IBOHandle.get(iboName);
        if (handle == null) {
            meshManager.request(iboName);
            handle = iboName2IBOHandle.get(iboName);
        }
        return handle;
    }

    // Runtime \\

    public IBOInstance createIBOInstance(VAOInstance vaoInstance, ShortArrayList indices) {
        return GLSLUtility.uploadIndexData(vaoInstance, create(IBOInstance.class), indices.toShortArray());
    }

    // Removal \\

    public void removeIBO(IBOStruct iboStruct) {
        GLSLUtility.removeIndexData(iboStruct);
    }

    public void removeIBO(IBOHandle iboHandle) {
        GLSLUtility.removeIndexData(iboHandle.getIBOStruct());
    }

    public void removeIBOInstance(IBOInstance iboInstance) {
        GLSLUtility.removeIndexData(iboInstance.getIBOStruct());
    }
}