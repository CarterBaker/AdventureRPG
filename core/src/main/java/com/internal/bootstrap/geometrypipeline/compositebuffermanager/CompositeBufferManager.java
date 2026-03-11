package com.internal.bootstrap.geometrypipeline.compositebuffermanager;

import com.internal.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.core.engine.ManagerPackage;

/*
 * Handles creation and disposal of CompositeBufferInstances only.
 * Upload and draw belong to CompositeRenderSystem.
 */
public class CompositeBufferManager extends ManagerPackage {

    // Constructor \\

    public void constructor(CompositeBufferInstance buffer, MeshHandle meshHandle, int[] instanceAttrSizes) {
        buffer.init(meshHandle, instanceAttrSizes);
        int vbo = GLSLUtility.createDynamicInstanceVBO(buffer.getMaxInstances(), buffer.getFloatsPerInstance());
        int vao = GLSLUtility.createInstancedVAO(
                meshHandle.getVBOHandle().getVBOStruct().vertexHandle,
                meshHandle.getVAOInstance().getVAOStruct().attrSizes,
                meshHandle.getIBOHandle().getIBOStruct().indexHandle,
                vbo,
                instanceAttrSizes);
        buffer.setInstanceVBO(vbo);
        buffer.setCompositeVAO(vao);
    }

    // Grow \\

    public void grow(CompositeBufferInstance buffer) {
        GLSLUtility.deleteBuffer(buffer.getInstanceVBO());
        GLSLUtility.deleteVAO(buffer.getCompositeVAO());
        int vbo = GLSLUtility.createDynamicInstanceVBO(buffer.getMaxInstances(), buffer.getFloatsPerInstance());
        int vao = GLSLUtility.createInstancedVAO(
                buffer.getMeshHandle().getVBOHandle().getVBOStruct().vertexHandle,
                buffer.getMeshHandle().getVAOInstance().getVAOStruct().attrSizes,
                buffer.getMeshHandle().getIBOHandle().getIBOStruct().indexHandle,
                vbo,
                buffer.getInstanceAttrSizes());
        buffer.setInstanceVBO(vbo);
        buffer.setCompositeVAO(vao);
        buffer.clearNeedsGpuRealloc();
    }

    // Dispose \\

    public void dispose(CompositeBufferInstance buffer) {
        GLSLUtility.deleteBuffer(buffer.getInstanceVBO());
        GLSLUtility.deleteVAO(buffer.getCompositeVAO());
    }
}