package com.internal.bootstrap.geometrypipeline.vaomanager;

import com.internal.bootstrap.geometrypipeline.meshmanager.InternalLoader;
import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.geometrypipeline.vao.VAOStruct;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class VAOManager extends ManagerPackage {

    // Internal
    private InternalLoader meshLoader;

    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, VAOHandle> vaoName2VAOHandle;

    // Base \\

    @Override
    protected void create() {
        this.vaoName2VAOHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.meshLoader = get(InternalLoader.class);
    }

    // Handle Registration \\

    void registerVAO(String resourceName, VAOHandle handle) {
        vaoName2VAOHandle.put(resourceName, handle);
    }

    // Accessible \\

    /*
     * Pure registry lookup — no load trigger. For use inside builders
     * where the owning file is already mid-load.
     */
    public boolean hasVAO(String vaoName) {
        return vaoName2VAOHandle.containsKey(vaoName);
    }

    /*
     * Direct registry lookup — no load trigger. For use inside builders
     * that are themselves invoked by the mesh loader. Calling
     * getVAOHandleFromName() from inside a builder would recurse infinitely.
     */
    public VAOHandle getVAOHandleDirect(String vaoName) {
        return vaoName2VAOHandle.get(vaoName);
    }

    /*
     * Auto-triggers load on miss. For external callers only —
     * never call this from inside a builder that is itself invoked by
     * the mesh loader, or you will recurse infinitely.
     */
    public VAOHandle getVAOHandleFromName(String vaoName) {
        VAOHandle handle = vaoName2VAOHandle.get(vaoName);
        if (handle == null) {
            meshLoader.request(vaoName);
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