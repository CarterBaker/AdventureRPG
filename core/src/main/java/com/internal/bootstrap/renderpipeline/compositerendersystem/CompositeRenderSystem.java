package com.internal.bootstrap.renderpipeline.compositerendersystem;

import com.internal.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import com.internal.bootstrap.geometrypipeline.compositebuffermanager.CompositeBufferManager;
import com.internal.bootstrap.renderpipeline.compositebatch.CompositeBatchInstance;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.core.engine.SystemPackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/*
 * Collects instanced draw submissions during update, uploads to GPU and
 * flushes in draw(). Must be drawn AFTER RenderSystem.draw().
 */
public class CompositeRenderSystem extends SystemPackage {

    private CompositeBufferManager compositeBufferManager;

    // Upload scratch buffer
    private FloatBuffer uploadBuffer;
    private int uploadBufferCapacity;

    // Submissions batched by materialID
    private Int2ObjectOpenHashMap<CompositeBatchInstance> materialID2Batch;

    // Internal \\

    @Override
    protected void create() {
        this.materialID2Batch = new Int2ObjectOpenHashMap<>();
        this.uploadBufferCapacity = 0;
    }

    @Override
    protected void get() {
        this.compositeBufferManager = get(CompositeBufferManager.class);
    }

    // Submit \\

    public void submit(MaterialInstance material, CompositeBufferInstance buffer) {
        if (buffer.isEmpty())
            return;
        int id = material.getMaterialID();
        CompositeBatchInstance batch = materialID2Batch.get(id);
        if (batch == null) {
            batch = create(CompositeBatchInstance.class);
            batch.constructor(material);
            materialID2Batch.put(id, batch);
        }
        batch.add(buffer);
    }

    // Draw \\

    public void draw() {
        if (materialID2Batch.isEmpty())
            return;
        for (var entry : materialID2Batch.int2ObjectEntrySet()) {
            CompositeBatchInstance batch = entry.getValue();
            if (batch.isEmpty())
                continue;
            bindMaterial(batch);
            ObjectArrayList<CompositeBufferInstance> buffers = batch.getBuffers();
            for (int i = 0; i < buffers.size(); i++)
                drawBuffer(buffers.get(i));
            batch.clear();
        }
    }

    // Upload + Draw \\

    private void drawBuffer(CompositeBufferInstance buffer) {
        if (buffer.isEmpty())
            return;
        if (buffer.needsGpuRealloc())
            compositeBufferManager.grow(buffer);
        upload(buffer);
        GLSLUtility.drawElementsInstanced(
                buffer.getCompositeVAO(),
                buffer.getIndexCount(),
                buffer.getInstanceCount());
    }

    private void upload(CompositeBufferInstance buffer) {
        if (!buffer.isDirty())
            return;
        int floatCount = buffer.getInstanceCount() * buffer.getFloatsPerInstance();
        ensureUploadBuffer(floatCount);
        uploadBuffer.clear();
        uploadBuffer.put(buffer.getInstanceData(), 0, floatCount);
        uploadBuffer.flip();
        GLSLUtility.updateInstanceVBO(buffer.getInstanceVBO(), uploadBuffer, floatCount);
        buffer.clearDirty();
    }

    // Bind \\

    private void bindMaterial(CompositeBatchInstance batch) {
        MaterialInstance material = batch.getMaterial();
        int shaderHandle = material.getShaderHandle().getShaderHandle();
        GLSLUtility.useShader(shaderHandle);

        UBOHandle[] sourceUBOs = batch.getCachedSourceUBOs();
        for (int i = 0; i < sourceUBOs.length; i++) {
            UBOHandle ubo = sourceUBOs[i];
            GLSLUtility.bindUniformBlock(shaderHandle, ubo.getBufferName(), ubo.getBindingPoint());
            GLSLUtility.bindUniformBuffer(ubo.getBindingPoint(), ubo.getGpuHandle());
        }

        var uniforms = material.getUniforms();
        if (uniforms != null) {
            int textureUnit = 0;
            for (Uniform<?> uniform : uniforms.values()) {
                if (uniform.attribute().isSampler()) {
                    uniform.attribute().bindTexture(textureUnit);
                    textureUnit++;
                }
                uniform.push();
            }
        }
    }

    // Upload Buffer \\

    private void ensureUploadBuffer(int floatCount) {
        if (floatCount <= uploadBufferCapacity)
            return;
        uploadBufferCapacity = floatCount * 2;
        uploadBuffer = ByteBuffer
                .allocateDirect(uploadBufferCapacity * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
    }
}