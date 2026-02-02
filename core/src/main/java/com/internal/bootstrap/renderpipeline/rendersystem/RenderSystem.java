package com.internal.bootstrap.renderpipeline.rendersystem;

import com.internal.bootstrap.geometrypipeline.modelmanager.ModelHandle;
import com.internal.bootstrap.renderpipeline.renderbatch.RenderBatchHandle;
import com.internal.bootstrap.renderpipeline.rendercall.RenderCallHandle;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.WindowInstance;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class RenderSystem extends SystemPackage {

    // Internal
    private WindowInstance windowInstance;
    private Int2ObjectAVLTreeMap<Object2ObjectOpenHashMap<MaterialHandle, RenderBatchHandle>> depth2RenderBatchHandles;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.depth2RenderBatchHandles = new Int2ObjectAVLTreeMap<>();
    }

    @Override
    protected void get() {

        // Internal
        this.windowInstance = internal.getWindowInstance();
    }

    @Override
    protected void awake() {
        GLSLUtility.enableDepth();
        GLSLUtility.enableBlending();
        GLSLUtility.disableCulling();
    }

    // Render System \\

    public void draw() {

        GLSLUtility.setViewport(
                windowInstance.getWidth(),
                windowInstance.getHeight());

        GLSLUtility.clearBuffer();

        // Iterate through each depth level
        for (var depthEntry : depth2RenderBatchHandles.int2ObjectEntrySet()) {

            int depth = depthEntry.getIntKey();
            var materialBatches = depthEntry.getValue();

            // Clear depth buffer for each custom depth layer
            GLSLUtility.clearDepthBuffer();

            // Iterate through each material batch at this depth
            for (var batchEntry : materialBatches.entrySet()) {

                MaterialHandle material = batchEntry.getKey();
                RenderBatchHandle batch = batchEntry.getValue();

                // Bind material once for entire batch
                bindMaterial(material, depth);

                // Bind all UBOs for this material
                bindMaterialUBOs(material);

                // Push all uniforms per material
                pushMaterialUniforms(material);

                // Draw all render calls in this batch
                for (RenderCallHandle renderCall : batch.getRenderCalls())
                    drawBatchedRenderCall(renderCall);

                batch.clear();
            }
        }
    }

    private void bindMaterial(MaterialHandle material, int depth) {

        // Configure depth testing based on depth level
        if (depth == 0) // Standard 3D rendering with depth testing
            GLSLUtility.enableDepth();

        else // Post-processing passes typically don't use depth testing
            GLSLUtility.disableDepth();

        // Bind shader once for entire batch
        GLSLUtility.useShader(material.getShaderHandle().getShaderHandle());
    }

    private void bindMaterialUBOs(MaterialHandle material) {

        var ubos = material.getUBOs();

        if (ubos == null || ubos.isEmpty())
            return;

        // Bind each UBO to its binding point
        for (UBOHandle ubo : ubos.values()) {
            GLSLUtility.bindUniformBlockToProgram(
                    material.getShaderHandle().getShaderHandle(),
                    ubo.getBufferName(),
                    ubo.getBindingPoint());
            GLSLUtility.bindUniformBuffer(
                    ubo.getBindingPoint(),
                    ubo.getGpuHandle());
        }
    }

    private void pushMaterialUniforms(MaterialHandle material) {

        var uniforms = material.getUniforms();

        if (uniforms == null || uniforms.isEmpty())
            return;

        for (Uniform<?> uniform : uniforms.values()) {
            uniform.push();
        }
    }

    private void drawBatchedRenderCall(RenderCallHandle renderCall) {

        ModelHandle modelHandle = renderCall.getModelHandle();

        // Bind VAO
        GLSLUtility.bindVAO(modelHandle.getVAO());

        // Draw
        GLSLUtility.drawElements(modelHandle.getIndexCount());

        // Unbind
        GLSLUtility.unbindVAO();
    }

    // Accessible \\

    public RenderCallHandle pushRenderCall(ModelHandle modelHandle, int depth) {

        // Create render call handle
        RenderCallHandle renderCall = create(RenderCallHandle.class);
        renderCall.constructor(modelHandle);

        // Get material from model
        MaterialHandle material = modelHandle.getMaterial();

        // Get or create material batches map for this depth
        Object2ObjectOpenHashMap<MaterialHandle, RenderBatchHandle> materialBatches = depth2RenderBatchHandles
                .computeIfAbsent(
                        depth,
                        k -> new Object2ObjectOpenHashMap<>());

        // Get or create batch for this material
        RenderBatchHandle batch = materialBatches.computeIfAbsent(
                material,
                k -> {
                    RenderBatchHandle newBatch = create(RenderBatchHandle.class);
                    newBatch.constructor(material);
                    return newBatch;
                });

        // Add render call to batch
        batch.addRenderCall(renderCall);

        return renderCall;
    }
}