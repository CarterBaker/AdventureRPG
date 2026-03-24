package com.internal.bootstrap.geometrypipeline.compositebuffermanager;

import com.internal.bootstrap.geometrypipeline.compositebuffer.CompositeBufferData;
import com.internal.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.core.engine.ManagerPackage;

public class CompositeBufferManager extends ManagerPackage {

    /*
     * Creates, grows, and disposes CompositeBufferInstances.
     * Upload and draw are the responsibility of CompositeRenderSystem.
     */

    // Constructor \\

    public void constructor(CompositeBufferInstance buffer, MeshHandle meshHandle, int[] instanceAttrSizes) {

        CompositeBufferData compositeBufferData = new CompositeBufferData(meshHandle, instanceAttrSizes);
        buffer.constructor(compositeBufferData);

        int vbo = GLSLUtility.createDynamicInstanceVBO(buffer.getMaxInstances(), buffer.getFloatsPerInstance());
        int vao = GLSLUtility.createInstancedVAO(
                meshHandle.getVBOHandle().getVBOData().getVertexHandle(),
                meshHandle.getVAOInstance().getVAOData().getAttrSizes(),
                meshHandle.getIBOHandle().getIBOData().getIndexHandle(),
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
                buffer.getMeshHandle().getVBOHandle().getVBOData().getVertexHandle(),
                buffer.getMeshHandle().getVAOInstance().getVAOData().getAttrSizes(),
                buffer.getMeshHandle().getIBOHandle().getIBOData().getIndexHandle(),
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