package com.internal.bootstrap.geometrypipeline.vbomanager;

import com.internal.bootstrap.geometrypipeline.meshmanager.InternalLoader;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.geometrypipeline.vbo.VBOHandle;
import com.internal.bootstrap.geometrypipeline.vbo.VBOInstance;
import com.internal.bootstrap.geometrypipeline.vbo.VBOStruct;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class VBOManager extends ManagerPackage {

    // Internal
    private InternalLoader meshLoader;

    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, VBOHandle> vboName2VBOHandle;

    // Base \\

    @Override
    protected void create() {
        this.vboName2VBOHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.meshLoader = get(InternalLoader.class);
    }

    // Handle Registration \\

    void registerVBO(String resourceName, VBOHandle handle) {
        vboName2VBOHandle.put(resourceName, handle);
    }

    /*
     * Bypasses JSON parsing — used when vertex data was assembled by quad
     * expansion.
     */
    public VBOHandle addVBOFromData(String resourceName, float[] vertices, VAOInstance vaoInstance) {
        VBOHandle handle = GLSLUtility.uploadVertexData(vaoInstance, create(VBOHandle.class), vertices);
        vboName2VBOHandle.put(resourceName, handle);
        return handle;
    }

    // Accessible \\

    /*
     * Pure registry lookup — no load trigger. For use inside builders
     * where the owning file is already mid-load.
     */
    public boolean hasVBO(String vboName) {
        return vboName2VBOHandle.containsKey(vboName);
    }

    /*
     * Direct registry lookup — no load trigger. For use inside builders
     * that are themselves invoked by the mesh loader. Calling
     * getVBOHandleFromName() from inside a builder would recurse infinitely.
     */
    public VBOHandle getVBOHandleDirect(String vboName) {
        return vboName2VBOHandle.get(vboName);
    }

    /*
     * Auto-triggers load on miss. For external callers only —
     * never call this from inside a builder that is itself invoked by
     * the mesh loader, or you will recurse infinitely.
     */
    public VBOHandle getVBOHandleFromName(String vboName) {
        VBOHandle handle = vboName2VBOHandle.get(vboName);
        if (handle == null) {
            meshLoader.request(vboName);
            handle = vboName2VBOHandle.get(vboName);
        }
        return handle;
    }

    // Runtime \\

    public VBOInstance createVBOInstance(VAOInstance vaoInstance, FloatArrayList vertices) {
        return GLSLUtility.uploadVertexData(vaoInstance, create(VBOInstance.class), vertices.toFloatArray());
    }

    // Removal \\

    public void removeVBO(VBOStruct vboStruct) {
        GLSLUtility.removeVertexData(vboStruct);
    }

    public void removeVBO(VBOHandle vboHandle) {
        GLSLUtility.removeVertexData(vboHandle.getVBOStruct());
    }

    public void removeVBOInstance(VBOInstance vboInstance) {
        GLSLUtility.removeVertexData(vboInstance.getVBOStruct());
    }
}