package com.internal.bootstrap.renderpipeline.compositerendersystem;

import com.internal.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import com.internal.bootstrap.geometrypipeline.compositebuffermanager.CompositeBufferManager;
import com.internal.bootstrap.renderpipeline.compositebatch.CompositeBatchStruct;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformStruct;
import com.internal.core.engine.SystemPackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class CompositeRenderSystem extends SystemPackage {

    /*
     * Collects instanced draw submissions during update, uploads instance data
     * to the GPU, and flushes all batches in draw(). Drawn after RenderManager
     * depth 0 so composite draws share and respect world geometry depth.
     * All hot-path iteration is index-based over pre-allocated arrays — zero
     * allocation per frame after the first few frames of material registration.
     */

    // Internal
    private CompositeBufferManager compositeBufferManager;

    // Palette
    private Int2ObjectOpenHashMap<CompositeBatchStruct> materialID2Batch;
    private ObjectArrayList<CompositeBatchStruct> batches;

    // Upload Scratch
    private FloatBuffer uploadBuffer;
    private int uploadBufferCapacity;

    // Internal \\

    @Override
    protected void create() {
        this.materialID2Batch = new Int2ObjectOpenHashMap<>();
        this.batches = new ObjectArrayList<>();
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
        CompositeBatchStruct batch = materialID2Batch.get(id);

        if (batch == null) {
            batch = new CompositeBatchStruct(material);
            materialID2Batch.put(id, batch);
            batches.add(batch);
        }

        batch.add(buffer);
    }

    // Draw \\

    public void draw() {

        if (batches.isEmpty())
            return;

        GLSLUtility.enableDepth();

        Object[] batchElements = batches.elements();
        int batchCount = batches.size();

        for (int i = 0; i < batchCount; i++) {

            CompositeBatchStruct batch = (CompositeBatchStruct) batchElements[i];

            if (batch.isEmpty())
                continue;

            bindMaterial(batch);

            ObjectArrayList<CompositeBufferInstance> buffers = batch.getBuffers();
            Object[] bufferElements = buffers.elements();
            int bufferCount = buffers.size();

            for (int j = 0; j < bufferCount; j++)
                drawBuffer((CompositeBufferInstance) bufferElements[j]);

            batch.clear();
        }
    }

    // Upload and Draw \\

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

        if (!buffer.needsUpload())
            return;

        int floatCount = buffer.getInstanceCount() * buffer.getFloatsPerInstance();
        ensureUploadBuffer(floatCount);

        uploadBuffer.clear();
        uploadBuffer.put(buffer.getInstanceData(), 0, floatCount);
        uploadBuffer.flip();

        GLSLUtility.updateInstanceVBO(buffer.getInstanceVBO(), uploadBuffer, floatCount);
        buffer.markUploaded();
    }

    // Bind \\

    private void bindMaterial(CompositeBatchStruct batch) {

        MaterialInstance material = batch.getMaterial();
        int shaderHandle = material.getShaderHandle().getGpuHandle();

        GLSLUtility.useShader(shaderHandle);

        UBOHandle[] sourceUBOs = batch.getCachedSourceUBOs();

        for (int i = 0; i < sourceUBOs.length; i++) {
            UBOHandle ubo = sourceUBOs[i];
            GLSLUtility.bindUniformBlock(shaderHandle, ubo.getBlockName(), ubo.getBindingPoint());
            GLSLUtility.bindUniformBuffer(ubo.getBindingPoint(), ubo.getGpuHandle());
        }

        UniformStruct<?>[] uniforms = batch.getCachedUniforms();
        int textureUnit = 0;

        for (int i = 0; i < uniforms.length; i++) {
            UniformStruct<?> uniform = uniforms[i];
            if (uniform.attribute().isSampler()) {
                uniform.attribute().bindTexture(textureUnit);
                textureUnit++;
            }
            uniform.push();
        }
    }

    // Upload Buffer \\

    private void ensureUploadBuffer(int floatCount) {

        if (floatCount <= uploadBufferCapacity)
            return;

        uploadBufferCapacity = floatCount * 2;
        uploadBuffer = ByteBuffer
                .allocateDirect(uploadBufferCapacity * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
    }
}