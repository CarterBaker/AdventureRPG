package com.internal.bootstrap.renderpipeline.rendersystem;

import com.internal.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.renderpipeline.compositebatch.CompositeBatchInstance;
import com.internal.bootstrap.renderpipeline.compositerendersystem.CompositeRenderSystem;
import com.internal.bootstrap.renderpipeline.renderbatch.RenderBatchHandle;
import com.internal.bootstrap.renderpipeline.rendercall.MaskStruct;
import com.internal.bootstrap.renderpipeline.rendercall.RenderCallHandle;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubo.UBOInstance;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.WindowInstance;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RenderSystem extends SystemPackage {

    private WindowInstance windowInstance;
    private CompositeRenderSystem compositeRenderSystem;
    private Int2ObjectAVLTreeMap<Int2ObjectOpenHashMap<RenderBatchHandle>> depth2RenderBatchHandles;

    @Override
    protected void create() {
        this.depth2RenderBatchHandles = new Int2ObjectAVLTreeMap<>();
    }

    @Override
    protected void get() {
        this.windowInstance = internal.getWindowInstance();
        this.compositeRenderSystem = get(CompositeRenderSystem.class);
    }

    @Override
    protected void awake() {
        GLSLUtility.enableDepth();
        GLSLUtility.enableBlending();
        GLSLUtility.disableCulling();
    }

    // Draw \\

    public void draw() {

        GLSLUtility.setViewport(
                windowInstance.getWidth(),
                windowInstance.getHeight());

        GLSLUtility.clearBuffer();

        for (var depthEntry : depth2RenderBatchHandles.int2ObjectEntrySet()) {

            int depth = depthEntry.getIntKey();
            var materialBatches = depthEntry.getValue();

            GLSLUtility.clearDepthBuffer();

            MaskStruct activeMask = null;

            for (var batch : materialBatches.values()) {

                if (batch.isEmpty())
                    continue;

                MaterialInstance representative = batch.getRepresentativeMaterial();
                bindMaterial(representative, depth);
                bindSourceUBOs(batch);

                ObjectArrayList<RenderCallHandle> renderCalls = batch.getRenderCalls();
                Object[] elements = renderCalls.elements();
                int size = renderCalls.size();

                for (int i = 0; i < size; i++) {
                    RenderCallHandle renderCall = (RenderCallHandle) elements[i];

                    MaskStruct callMask = renderCall.getMask();
                    if (callMask != activeMask) {
                        if (callMask != null)
                            GLSLUtility.enableScissor(callMask.x, callMask.y, callMask.w, callMask.h);
                        else
                            GLSLUtility.disableScissor();
                        activeMask = callMask;
                    }

                    pushInstanceUBOs(renderCall);
                    pushInstanceUniforms(renderCall);
                    drawBatchedRenderCall(renderCall);
                }

                batch.clear();
            }

            if (activeMask != null)
                GLSLUtility.disableScissor();

            // Composite instanced draws (world items etc.) share layer 0's
            // depth buffer so they occlude and are occluded by world geometry.
            if (depth == 0)
                compositeRenderSystem.draw();
        }
    }

    // Bridge \\

    /*
     * Routes an instanced draw submission to CompositeRenderSystem.
     * Called during update() by any system that needs instanced rendering —
     * never call CompositeRenderSystem.submit() directly from outside the
     * render pipeline.
     */
    public void pushCompositeCall(MaterialInstance material, CompositeBufferInstance buffer) {
        compositeRenderSystem.submit(material, buffer);
    }

    // Internal \\

    private void bindMaterial(MaterialInstance material, int depth) {
        if (depth == 0)
            GLSLUtility.enableDepth();
        else
            GLSLUtility.disableDepth();
        GLSLUtility.useShader(material.getShaderHandle().getShaderHandle());
    }

    private void bindSourceUBOs(RenderBatchHandle batch) {
        UBOHandle[] handles = batch.getCachedSourceUBOs();
        if (handles.length == 0)
            return;
        int shaderHandle = batch.getRepresentativeMaterial().getShaderHandle().getShaderHandle();
        for (int i = 0; i < handles.length; i++) {
            UBOHandle ubo = handles[i];
            GLSLUtility.bindUniformBlockToProgram(shaderHandle, ubo.getBufferName(), ubo.getBindingPoint());
            GLSLUtility.bindUniformBuffer(ubo.getBindingPoint(), ubo.getGpuHandle());
        }
    }

    private void pushInstanceUBOs(RenderCallHandle renderCall) {
        UBOInstance[] instances = renderCall.getCachedInstanceUBOs();
        for (int i = 0; i < instances.length; i++) {
            UBOInstance ubo = instances[i];
            GLSLUtility.bindUniformBuffer(ubo.getBindingPoint(), ubo.getGpuHandle());
        }
    }

    private void pushInstanceUniforms(RenderCallHandle renderCall) {
        Uniform<?>[] uniforms = renderCall.getCachedUniforms();
        int textureUnit = 0;
        for (int i = 0; i < uniforms.length; i++) {
            Uniform<?> uniform = uniforms[i];
            if (uniform.attribute().isSampler()) {
                uniform.attribute().bindTexture(textureUnit);
                textureUnit++;
            }
            uniform.push();
        }
    }

    private void drawBatchedRenderCall(RenderCallHandle renderCall) {
        ModelInstance model = renderCall.getModelInstance();
        GLSLUtility.bindVAO(model.getVAO());
        GLSLUtility.drawElements(model.getIndexCount());
        GLSLUtility.unbindVAO();
    }

    // Accessible \\

    public RenderCallHandle pushRenderCall(ModelInstance modelInstance, int depth) {
        return pushRenderCall(modelInstance, depth, null);
    }

    public RenderCallHandle pushRenderCall(ModelInstance modelInstance, int depth, MaskStruct mask) {

        RenderCallHandle renderCall = create(RenderCallHandle.class);
        renderCall.constructor(modelInstance, mask);

        MaterialInstance material = modelInstance.getMaterial();
        int materialID = material.getMaterialID();

        Int2ObjectOpenHashMap<RenderBatchHandle> materialBatches = depth2RenderBatchHandles
                .computeIfAbsent(depth, k -> new Int2ObjectOpenHashMap<>());

        RenderBatchHandle batch = materialBatches.computeIfAbsent(
                materialID,
                k -> {
                    RenderBatchHandle newBatch = create(RenderBatchHandle.class);
                    newBatch.constructor(material);
                    return newBatch;
                });

        batch.addRenderCall(renderCall);
        return renderCall;
    }
}