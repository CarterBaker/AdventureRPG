package com.internal.bootstrap.geometrypipeline.vbomanager;

import java.io.File;

import com.internal.bootstrap.geometrypipeline.meshmanager.InternalLoadManager;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
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

        // Internal
        this.internalBuildSystem = create(InternalBuildSystem.class);

        // Retrieval Mapping
        this.vboName2VBOHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void release() {
        internalBuildSystem = release(InternalBuildSystem.class);
    }

    // Utility \\

    public void addVBO(String resourceName, File file, InternalLoadManager loadManager) {
        vboName2VBOHandle.put(resourceName, internalBuildSystem.addVBO(file, loadManager));
    }

    public VBOHandle getVBOHandleFromName(String vboName) {
        return vboName2VBOHandle.get(vboName);
    }

    // Accessible \\

    // Accessible \\
    public VBOHandle createVBO(VAOHandle vaoHandle, FloatArrayList vertices) {
        VBOHandle vboHandle = create(VBOHandle.class);
        float[] vertexArray = vertices.toFloatArray();
        return GLSLUtility.uploadVertexData(vaoHandle, vboHandle, vertexArray);
    }

    public void removeVBO(VBOHandle vboHandle) {
        GLSLUtility.removeVertexData(vboHandle);
    }
}
