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
        this.depth2RenderBatchHandles = new Int2ObjectAVLTreeMap<>();
    }

    @Override
    protected void get() {
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

        for (var depthEntry : depth2RenderBatchHandles.int2ObjectEntrySet()) {

            int depth = depthEntry.getIntKey();
            var materialBatches = depthEntry.getValue();

            GLSLUtility.clearDepthBuffer();

            for (var batchEntry : materialBatches.entrySet()) {

                MaterialHandle material = batchEntry.getKey();
                RenderBatchHandle batch = batchEntry.getValue();

                bindMaterial(material, depth);
                bindMaterialUBOs(material);
                pushMaterialUniforms(material);

                for (RenderCallHandle renderCall : batch.getRenderCalls())
                    drawBatchedRenderCall(renderCall);

                batch.clear();
            }
        }
    }

    private void bindMaterial(MaterialHandle material, int depth) {

        if (depth == 0)
            GLSLUtility.enableDepth();
        else
            GLSLUtility.disableDepth();

        GLSLUtility.useShader(material.getShaderHandle().getShaderHandle());
    }

    private void bindMaterialUBOs(MaterialHandle material) {

        var ubos = material.getUBOs();

        if (ubos == null || ubos.isEmpty())
            return;

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

        int textureUnit = 0;

        for (Uniform<?> uniform : uniforms.values()) {

            if (uniform.attribute().isSampler()) {
                uniform.attribute().bindTexture(textureUnit);
                textureUnit++;
            }

            uniform.push();
        }
    }

    private void drawBatchedRenderCall(RenderCallHandle renderCall) {

        ModelHandle modelHandle = renderCall.getModelHandle();

        GLSLUtility.bindVAO(modelHandle.getVAO());
        GLSLUtility.drawElements(modelHandle.getIndexCount());
        GLSLUtility.unbindVAO();
    }

    // Accessible \\

    public RenderCallHandle pushRenderCall(ModelHandle modelHandle, int depth) {

        RenderCallHandle renderCall = create(RenderCallHandle.class);
        renderCall.constructor(modelHandle);

        MaterialHandle material = modelHandle.getMaterial();

        Object2ObjectOpenHashMap<MaterialHandle, RenderBatchHandle> materialBatches = depth2RenderBatchHandles
                .computeIfAbsent(depth, k -> new Object2ObjectOpenHashMap<>());

        RenderBatchHandle batch = materialBatches.computeIfAbsent(
                material,
                k -> {
                    RenderBatchHandle newBatch = create(RenderBatchHandle.class);
                    newBatch.constructor(material);
                    return newBatch;
                });

        batch.addRenderCall(renderCall);

        return renderCall;
    }
}