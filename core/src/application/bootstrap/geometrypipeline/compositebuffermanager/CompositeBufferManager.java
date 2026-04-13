package application.bootstrap.geometrypipeline.compositebuffermanager;

import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferData;
import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.core.engine.ManagerPackage;

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
                meshHandle.getVertexHandle(),
                meshHandle.getAttrSizes(),
                meshHandle.getIndexHandle(),
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
                buffer.getMeshHandle().getVertexHandle(),
                buffer.getMeshHandle().getAttrSizes(),
                buffer.getMeshHandle().getIndexHandle(),
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