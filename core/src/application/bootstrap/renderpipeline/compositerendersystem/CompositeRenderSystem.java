package application.bootstrap.renderpipeline.compositerendersystem;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.renderpipeline.compositebatch.CompositeBatchStruct;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class CompositeRenderSystem extends SystemPackage {

    /*
     * Collects instanced draw submissions during update, uploads instance data
     * to the GPU, and flushes all batches in draw(). Drawn after RenderManager
     * depth 0 so composite draws appear over world geometry.
     * Depth testing and depth writes are disabled for the composite pass —
     * UI always draws on top. Blending is enabled for alpha transparency.
     * All hot-path iteration is index-based over pre-allocated arrays — zero
     * allocation per frame after the first few frames of material registration.
     */

    // Per Window Batch Palette
    private Int2ObjectOpenHashMap<WindowCompositeState> windowID2CompositeState;

    // Per Window GPU Cache
    private Int2ObjectOpenHashMap<Object2ObjectOpenHashMap<CompositeBufferInstance, WindowBufferGpuState>> windowID2BufferGpuState;

    // Upload Scratch
    private FloatBuffer uploadBuffer;
    private int uploadBufferCapacity;

    // Internal \\

    @Override
    protected void create() {
        this.windowID2CompositeState = new Int2ObjectOpenHashMap<>();
        this.windowID2BufferGpuState = new Int2ObjectOpenHashMap<>();
    }

    // Submit \\

    public void submit(MaterialInstance material, CompositeBufferInstance buffer, WindowInstance window) {

        if (buffer.isEmpty())
            return;

        WindowCompositeState compositeState = getOrCreateCompositeState(window.getWindowID());
        int id = material.getMaterialID();
        CompositeBatchStruct batch = compositeState.materialID2Batch.get(id);

        if (batch == null) {
            batch = new CompositeBatchStruct(material);
            compositeState.materialID2Batch.put(id, batch);
            compositeState.batches.add(batch);
        }

        batch.add(buffer);
    }

    // Draw \\

    public void draw(WindowInstance window) {

        WindowCompositeState compositeState = windowID2CompositeState.get(window.getWindowID());

        if (compositeState == null || compositeState.batches.isEmpty())
            return;

        GLSLUtility.beginUIPass();

        Object[] batchElements = compositeState.batches.elements();
        int batchCount = compositeState.batches.size();

        for (int i = 0; i < batchCount; i++) {

            CompositeBatchStruct batch = (CompositeBatchStruct) batchElements[i];

            if (batch.isEmpty())
                continue;

            bindMaterial(batch);

            ObjectArrayList<CompositeBufferInstance> buffers = batch.getBuffers();
            Object[] bufferElements = buffers.elements();
            int bufferCount = buffers.size();

            for (int j = 0; j < bufferCount; j++)
                drawBuffer((CompositeBufferInstance) bufferElements[j], window.getWindowID());

            batch.clear();
        }

        GLSLUtility.endUIPass();
    }

    // Upload and Draw \\

    private void drawBuffer(CompositeBufferInstance buffer, int windowID) {

        if (buffer.isEmpty())
            return;

        WindowBufferGpuState gpuState = getOrCreateGpuState(buffer, windowID);

        if (gpuState.maxInstances < buffer.getMaxInstances()) {
            GLSLUtility.deleteBuffer(gpuState.instanceVBO);
            GLSLUtility.deleteVAO(gpuState.compositeVAO);
            gpuState.instanceVBO = EngineSetting.GL_HANDLE_NONE;
            gpuState.compositeVAO = EngineSetting.GL_HANDLE_NONE;
            gpuState.uploadedVersion = EngineSetting.COMPOSITE_UPLOAD_VERSION_UNINITIALIZED;
        }

        ensureGpuObjects(buffer, gpuState);
        upload(buffer, gpuState);

        GLSLUtility.drawElementsInstanced(
                gpuState.compositeVAO,
                buffer.getIndexCount(),
                buffer.getInstanceCount());
    }

    private void upload(CompositeBufferInstance buffer, WindowBufferGpuState gpuState) {

        int cpuVersion = buffer.getCompositeBufferData().getCpuVersion();

        if (gpuState.uploadedVersion == cpuVersion)
            return;

        int floatCount = buffer.getInstanceCount() * buffer.getFloatsPerInstance();
        ensureUploadBuffer(floatCount);

        uploadBuffer.clear();
        uploadBuffer.put(buffer.getInstanceData(), 0, floatCount);
        uploadBuffer.flip();

        GLSLUtility.updateInstanceVBO(gpuState.instanceVBO, uploadBuffer, floatCount);
        gpuState.uploadedVersion = cpuVersion;
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

        uploadBufferCapacity = floatCount * EngineSetting.COMPOSITE_UPLOAD_BUFFER_GROWTH_FACTOR;
        uploadBuffer = ByteBuffer
                .allocateDirect(uploadBufferCapacity * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
    }

    private void ensureGpuObjects(CompositeBufferInstance buffer, WindowBufferGpuState gpuState) {

        if (gpuState.instanceVBO != EngineSetting.GL_HANDLE_NONE
                && gpuState.compositeVAO != EngineSetting.GL_HANDLE_NONE)
            return;

        gpuState.instanceVBO = GLSLUtility.createDynamicInstanceVBO(
                buffer.getMaxInstances(),
                buffer.getFloatsPerInstance());
        gpuState.compositeVAO = GLSLUtility.createInstancedVAO(
                buffer.getMeshHandle().getVertexHandle(),
                buffer.getMeshHandle().getAttrSizes(),
                buffer.getMeshHandle().getIndexHandle(),
                gpuState.instanceVBO,
                buffer.getInstanceAttrSizes());
        gpuState.maxInstances = buffer.getMaxInstances();
    }

    private WindowCompositeState getOrCreateCompositeState(int windowID) {

        WindowCompositeState state = windowID2CompositeState.get(windowID);

        if (state != null)
            return state;

        state = new WindowCompositeState();
        windowID2CompositeState.put(windowID, state);
        return state;
    }

    private WindowBufferGpuState getOrCreateGpuState(CompositeBufferInstance buffer, int windowID) {

        Object2ObjectOpenHashMap<CompositeBufferInstance, WindowBufferGpuState> buffer2State = windowID2BufferGpuState
                .get(windowID);

        if (buffer2State == null) {
            buffer2State = new Object2ObjectOpenHashMap<>();
            windowID2BufferGpuState.put(windowID, buffer2State);
        }

        WindowBufferGpuState gpuState = buffer2State.get(buffer);

        if (gpuState != null)
            return gpuState;

        gpuState = new WindowBufferGpuState();
        buffer2State.put(buffer, gpuState);
        return gpuState;
    }

    public void removeWindow(int windowID) {

        WindowCompositeState compositeState = windowID2CompositeState.remove(windowID);

        if (compositeState != null) {
            Object[] batches = compositeState.batches.elements();
            int batchCount = compositeState.batches.size();
            for (int i = 0; i < batchCount; i++)
                ((CompositeBatchStruct) batches[i]).clear();
        }

        Object2ObjectOpenHashMap<CompositeBufferInstance, WindowBufferGpuState> buffer2State = windowID2BufferGpuState
                .remove(windowID);

        if (buffer2State == null)
            return;

        for (WindowBufferGpuState gpuState : buffer2State.values()) {
            GLSLUtility.deleteBuffer(gpuState.instanceVBO);
            GLSLUtility.deleteVAO(gpuState.compositeVAO);
        }
    }

    private static class WindowCompositeState {
        private final Int2ObjectOpenHashMap<CompositeBatchStruct> materialID2Batch = new Int2ObjectOpenHashMap<>();
        private final ObjectArrayList<CompositeBatchStruct> batches = new ObjectArrayList<>();
    }

    private static class WindowBufferGpuState {
        private int compositeVAO;
        private int instanceVBO;
        private int uploadedVersion = EngineSetting.COMPOSITE_UPLOAD_VERSION_UNINITIALIZED;
        private int maxInstances;
    }
}