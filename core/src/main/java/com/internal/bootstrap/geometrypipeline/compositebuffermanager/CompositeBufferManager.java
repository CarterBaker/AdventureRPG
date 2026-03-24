package com.internal.bootstrap.geometrypipeline.compositebuffermanager;

import com.internal.bootstrap.geometrypipeline.compositebuffer.CompositeBufferData;
import com.internal.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.bootstrap.renderpipeline.window.WindowInstance;
import com.internal.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public class CompositeBufferManager extends ManagerPackage {

    /*
     * Creates, grows, and disposes CompositeBufferInstances.
     * Upload and draw are the responsibility of CompositeRenderSystem.
     *
     * Composite VAOs are window-specific — each window gets its own VAO
     * pointing to the shared mesh VBO/IBO and the buffer's instance VBO.
     * VAOs are created lazily on first render per window.
     */

    // Internal
    private VAOManager vaoManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.vaoManager = get(VAOManager.class);
    }

    // Constructor \\

    public void constructor(CompositeBufferInstance buffer, MeshHandle meshHandle, int[] instanceAttrSizes) {

        CompositeBufferData compositeBufferData = new CompositeBufferData(meshHandle, instanceAttrSizes);
        buffer.constructor(compositeBufferData);
    }

    // Window-Specific Creation \\

    public void ensureWindowResources(CompositeBufferInstance buffer, WindowInstance window) {

        if (buffer.hasCompositeVAOForWindow(window))
            return;

        int vbo = GLSLUtility.createDynamicInstanceVBO(buffer.getMaxInstances(), buffer.getFloatsPerInstance());

        MeshHandle meshHandle = buffer.getMeshHandle();
        VAOHandle meshVAOTemplate = meshHandle.getVAOHandle();
        VAOInstance meshVAOInstance = vaoManager.getOrCreateVAOInstance(meshVAOTemplate, window);

        int vao = GLSLUtility.createInstancedVAO(
                meshHandle.getVBOHandle().getVBOData().getVertexHandle(),
                meshVAOInstance.getVAOData().getAttrSizes(),
                meshHandle.getIBOHandle().getIBOData().getIndexHandle(),
                vbo,
                buffer.getInstanceAttrSizes());

        buffer.setInstanceVBOForWindow(window, vbo);
        buffer.setCompositeVAOForWindow(window, vao);
    }

    // Grow \\

    public void grow(CompositeBufferInstance buffer, WindowInstance window) {

        int oldVBO = buffer.getInstanceVBOForWindow(window);
        int oldVAO = buffer.getCompositeVAOForWindow(window);

        GLSLUtility.deleteBuffer(oldVBO);
        GLSLUtility.deleteVAO(oldVAO);

        int vbo = GLSLUtility.createDynamicInstanceVBO(buffer.getMaxInstances(), buffer.getFloatsPerInstance());

        MeshHandle meshHandle = buffer.getMeshHandle();
        VAOHandle meshVAOTemplate = meshHandle.getVAOHandle();
        VAOInstance meshVAOInstance = vaoManager.getOrCreateVAOInstance(meshVAOTemplate, window);

        int vao = GLSLUtility.createInstancedVAO(
                meshHandle.getVBOHandle().getVBOData().getVertexHandle(),
                meshVAOInstance.getVAOData().getAttrSizes(),
                meshHandle.getIBOHandle().getIBOData().getIndexHandle(),
                vbo,
                buffer.getInstanceAttrSizes());

        buffer.setInstanceVBOForWindow(window, vbo);
        buffer.setCompositeVAOForWindow(window, vao);
        buffer.clearNeedsGpuRealloc();
    }

    // Dispose \\

    public void dispose(CompositeBufferInstance buffer) {

        Int2IntOpenHashMap window2VBO = buffer.getWindow2InstanceVBO();
        Int2IntOpenHashMap window2VAO = buffer.getWindow2CompositeVAO();

        int[] windowIDs = window2VBO.keySet().toIntArray();

        for (int i = 0; i < windowIDs.length; i++) {
            int windowID = windowIDs[i];
            GLSLUtility.deleteBuffer(window2VBO.get(windowID));
            GLSLUtility.deleteVAO(window2VAO.get(windowID));
        }
    }
}