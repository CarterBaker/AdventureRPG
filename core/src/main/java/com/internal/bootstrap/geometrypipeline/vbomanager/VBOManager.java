package com.internal.bootstrap.geometrypipeline.vbomanager;

import java.io.File;

import com.internal.bootstrap.geometrypipeline.meshmanager.InternalLoadManager;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.geometrypipeline.vbo.VBOHandle;
import com.internal.bootstrap.geometrypipeline.vbo.VBOInstance;
import com.internal.bootstrap.geometrypipeline.vbo.VBOStruct;
import com.internal.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class VBOManager extends ManagerPackage {

    // Internal
    private InternalBuildSystem internalBuildSystem;

    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, VBOHandle> vboName2VBOHandle;

    // Base \\

    @Override
    protected void create() {
        this.internalBuildSystem = create(InternalBuildSystem.class);
        this.vboName2VBOHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void release() {
        internalBuildSystem = release(InternalBuildSystem.class);
    }

    // Bootstrap \\

    public void addVBO(String resourceName, File file, InternalLoadManager loadManager, VAOInstance vaoInstance) {
        vboName2VBOHandle.put(resourceName, internalBuildSystem.addVBO(file, loadManager, vaoInstance));
    }

    // Bypasses JSON parsing. Used when vertex data has already been assembled
    // by the quad expansion path in meshmanager.InternalBuildSystem.
    public VBOHandle addVBOFromData(String resourceName, float[] vertices, VAOInstance vaoInstance) {
        VBOHandle handle = GLSLUtility.uploadVertexData(vaoInstance, create(VBOHandle.class), vertices);
        vboName2VBOHandle.put(resourceName, handle);
        return handle;
    }

    public VBOHandle getVBOHandleFromName(String vboName) {
        return vboName2VBOHandle.get(vboName);
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