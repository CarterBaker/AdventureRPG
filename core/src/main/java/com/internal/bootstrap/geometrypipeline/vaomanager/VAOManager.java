package com.internal.bootstrap.geometrypipeline.vaomanager;

import com.internal.bootstrap.geometrypipeline.meshmanager.MeshManager;
import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.geometrypipeline.vao.VAOStruct;
import com.internal.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class VAOManager extends ManagerPackage {

    // Internal
    private MeshManager meshManager;

    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, VAOHandle> vaoName2VAOHandle;

    // Base \\

    @Override
    protected void create() {
        this.vaoName2VAOHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
    }

    // Handle Registration \\

    void registerVAO(String resourceName, VAOHandle handle) {
        vaoName2VAOHandle.put(resourceName, handle);
    }

    // Accessible \\

    /*
     * Pure registry lookup — no load trigger. Safe to call from inside any
     * builder that is already executing within a load() call.
     */
    public boolean hasVAO(String vaoName) {
        return vaoName2VAOHandle.containsKey(vaoName);
    }

    /*
     * Direct registry lookup — no load trigger. Safe to call from inside any
     * builder that is already executing within a load() call.
     */
    public VAOHandle getVAOHandleDirect(String vaoName) {
        return vaoName2VAOHandle.get(vaoName);
    }

    /*
     * Auto-triggers a full mesh load on miss via MeshManager.
     * Safe for external callers only — never call from inside a builder
     * that is itself invoked by the mesh loader or you will recurse infinitely.
     */
    public VAOHandle getVAOHandleFromName(String vaoName) {
        VAOHandle handle = vaoName2VAOHandle.get(vaoName);
        if (handle == null) {
            meshManager.request(vaoName);
            handle = vaoName2VAOHandle.get(vaoName);
        }
        return handle;
    }

    // Instance Management \\

    public VAOInstance createVAOInstance(VAOHandle template) {
        return GLSLUtility.createVAOInstance(create(VAOInstance.class), template);
    }

    public void removeVAOStruct(VAOStruct vaoStruct) {
        GLSLUtility.removeVAOStruct(vaoStruct);
    }

    public void removeVAOInstance(VAOInstance vaoInstance) {
        GLSLUtility.removeVAOInstance(vaoInstance);
    }
}