package application.bootstrap.renderpipeline.rendermanager;

import application.bootstrap.shaderpipeline.shader.ShaderHandle;
import engine.util.memory.BufferUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.FloatBuffer;

import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.render.CompositeBatchStruct;
import application.bootstrap.renderpipeline.render.MaskStruct;
import application.bootstrap.renderpipeline.render.RenderQueueHandle;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class CompositeRenderSystem extends SystemPackage {

    /*
     * Collects instanced composite submissions during update, uploads instance
     * data, and draws every batch after the depth-sorted pass. Into targets
     * with depth it depth-tests like scene geometry; elsewhere it draws on top
     * with blending, scissored to any mask. Iteration is index-based over
     * reused arrays.
     */

    // Per Window GPU Cache
    private Int2ObjectOpenHashMap<Object2ObjectOpenHashMap<CompositeBufferInstance, WindowBufferGpuState>> //
            windowID2BufferGpuState;

    // Upload Scratch
    private FloatBuffer uploadBuffer;

    // Internal \\

    @Override
    protected void create() {
        this.windowID2BufferGpuState = new Int2ObjectOpenHashMap<>();
    }

    // Submit \\

    public void submit(
            MaterialInstance material,
            CompositeBufferInstance buffer,
            MaskStruct mask,
            FBOInstance fbo,
            WindowInstance window) {

        if (buffer.isEmpty())
            return;

        RenderQueueHandle queue = window.getRenderQueueHandle();

        if (queue == null)
            return;

        int id = material.getMaterialID();
        CompositeBatchStruct batch;

        if (fbo == null) {
            batch = queue.screenCompositeMaterialBatches.get(id);

            if (batch == null) {
                batch = new CompositeBatchStruct(material);
                queue.screenCompositeMaterialBatches.put(id, batch);
                queue.screenCompositeBatchList.add(batch);
            }
        } else {
            Int2ObjectOpenHashMap<CompositeBatchStruct> materialBatches = queue.fbo2CompositeMaterialBatches.get(fbo);

            if (materialBatches == null) {
                materialBatches = new Int2ObjectOpenHashMap<>();
                queue.fbo2CompositeMaterialBatches.put(fbo, materialBatches);
                queue.fbo2CompositeBatchList.put(fbo, new ObjectArrayList<>());
            }

            batch = materialBatches.get(id);

            if (batch == null) {
                batch = new CompositeBatchStruct(material);
                materialBatches.put(id, batch);
                queue.fbo2CompositeBatchList.get(fbo).add(batch);
            }
        }

        batch.add(buffer, material, mask);
    }

    // Draw \\

    public void draw(RenderQueueHandle queue, FBOInstance fbo, WindowInstance window) {
        ObjectArrayList<CompositeBatchStruct> batches = fbo == null
                ? queue.screenCompositeBatchList
                : queue.fbo2CompositeBatchList.get(fbo);

        if (batches == null || batches.isEmpty())
            return;

        if (fbo != null && !fbo.getDepthTextures().isEmpty())
            CompositeRenderGLSLUtility.beginScenePass();
        else
            CompositeRenderGLSLUtility.beginUIPass(fbo != null && fbo.getFboData().isPremultipliedBlend());

        Object[] batchElements = batches.elements();
        int batchCount = batches.size();
        MaskStruct activeMask = null;

        for (int i = 0; i < batchCount; i++) {

            CompositeBatchStruct batch = (CompositeBatchStruct) batchElements[i];

            if (batch.isEmpty())
                continue;

            bindMaterial(batch);

            ObjectArrayList<CompositeBufferInstance> buffers = batch.getBuffers();
            Object[] bufferElements = buffers.elements();
            Object[] materialElements = batch.getBufferMaterials().elements();
            Object[] maskElements = batch.getBufferMasks().elements();
            int bufferCount = buffers.size();
            MaterialInstance pushedMaterial = null;

            for (int j = 0; j < bufferCount; j++) {

                MaterialInstance bufferMaterial = (MaterialInstance) materialElements[j];

                if (bufferMaterial != pushedMaterial) {
                    pushUniforms(bufferMaterial);
                    pushedMaterial = bufferMaterial;
                }

                activeMask = applyMask((MaskStruct) maskElements[j], activeMask);
                drawBuffer((CompositeBufferInstance) bufferElements[j], window.getWindowID());
            }

            batch.clear();
        }

        if (activeMask != null)
            CompositeRenderGLSLUtility.disableScissor();

        CompositeRenderGLSLUtility.endPass();
    }

    private MaskStruct applyMask(MaskStruct mask, MaskStruct activeMask) {

        if (mask == null ? activeMask == null : mask.matches(activeMask))
            return activeMask;

        if (mask != null)
            CompositeRenderGLSLUtility.enableScissor(mask.getX(), mask.getY(), mask.getW(), mask.getH());
        else
            CompositeRenderGLSLUtility.disableScissor();

        return mask;
    }

    public void drawScreen(RenderQueueHandle queue, WindowInstance window) {
        draw(queue, null, window);
    }

    // Upload and Draw \\

    private void drawBuffer(CompositeBufferInstance buffer, int windowID) {

        if (buffer.isEmpty())
            return;

        WindowBufferGpuState gpuState = getOrCreateGpuState(buffer, windowID);

        if (gpuState.maxInstances < buffer.getMaxInstances()) {
            CompositeRenderGLSLUtility.deleteBuffer(gpuState.instanceVBO);
            CompositeRenderGLSLUtility.deleteVAO(gpuState.compositeVAO);
            gpuState.instanceVBO = EngineSetting.GL_HANDLE_NONE;
            gpuState.compositeVAO = EngineSetting.GL_HANDLE_NONE;
            gpuState.uploadedVersion = EngineSetting.COMPOSITE_UPLOAD_VERSION_UNINITIALIZED;
        }

        ensureGpuObjects(buffer, gpuState);
        upload(buffer, gpuState);

        CompositeRenderGLSLUtility.drawElementsInstanced(
                gpuState.compositeVAO,
                buffer.getIndexCount(),
                buffer.getInstanceCount());
    }

    private void upload(CompositeBufferInstance buffer, WindowBufferGpuState gpuState) {

        int cpuVersion = buffer.getCompositeBufferData().getCpuVersion();

        if (gpuState.uploadedVersion == cpuVersion)
            return;

        int floatCount = buffer.getInstanceCount() * buffer.getFloatsPerInstance();
        uploadBuffer = BufferUtility.ensureCapacity(uploadBuffer, floatCount);

        uploadBuffer.put(buffer.getInstanceData(), 0, floatCount);
        uploadBuffer.flip();

        CompositeRenderGLSLUtility.updateInstanceVBO(gpuState.instanceVBO, uploadBuffer, floatCount);
        gpuState.uploadedVersion = cpuVersion;
    }

    // Bind \\

    private void bindMaterial(CompositeBatchStruct batch) {

        MaterialInstance material = batch.getMaterial();
        ShaderHandle shader = material.getShaderHandle();

        CompositeRenderGLSLUtility.useShader(shader.getGpuHandle());

        UBOHandle[] sourceUBOs = batch.getCachedSourceUBOs();

        for (int i = 0; i < sourceUBOs.length; i++) {

            UBOHandle ubo = sourceUBOs[i];

            if (shader.claimBlockBinding(ubo.getBindingPoint()))
                CompositeRenderGLSLUtility.bindUniformBlock(
                        shader.getGpuHandle(),
                        ubo.getBlockName(),
                        ubo.getBindingPoint());

            CompositeRenderGLSLUtility.bindUniformBuffer(ubo.getBindingPoint(), ubo.getGpuHandle());
        }
    }

    private void pushUniforms(MaterialInstance material) {

        ObjectArrayList<String> uniformKeys = material.getUniformKeys();
        int textureUnit = 0;

        for (int i = 0; i < uniformKeys.size(); i++) {
            UniformStruct<?> uniform = material.getUniform(uniformKeys.get(i));
            if (uniform.attribute().isSampler()) {
                uniform.attribute().bindTexture(textureUnit);
                textureUnit++;
            }
            uniform.push();
        }
    }

    private void ensureGpuObjects(CompositeBufferInstance buffer, WindowBufferGpuState gpuState) {

        if (gpuState.instanceVBO != EngineSetting.GL_HANDLE_NONE
                && gpuState.compositeVAO != EngineSetting.GL_HANDLE_NONE)
            return;

        gpuState.instanceVBO = CompositeRenderGLSLUtility.createDynamicInstanceVBO(
                buffer.getMaxInstances(),
                buffer.getFloatsPerInstance());
        gpuState.compositeVAO = CompositeRenderGLSLUtility.createInstancedVAO(
                buffer.getMeshHandle().getVertexHandle(),
                buffer.getMeshHandle().getAttrSizes(),
                buffer.getMeshHandle().getIndexHandle(),
                gpuState.instanceVBO,
                buffer.getInstanceAttrSizes());
        gpuState.maxInstances = buffer.getMaxInstances();
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
        Object2ObjectOpenHashMap<CompositeBufferInstance, WindowBufferGpuState> buffer2State = windowID2BufferGpuState
                .remove(windowID);

        if (buffer2State == null)
            return;

        for (WindowBufferGpuState gpuState : buffer2State.values()) {
            CompositeRenderGLSLUtility.deleteBuffer(gpuState.instanceVBO);
            CompositeRenderGLSLUtility.deleteVAO(gpuState.compositeVAO);
        }
    }

    private static class WindowBufferGpuState {
        private int compositeVAO;
        private int instanceVBO;
        private int uploadedVersion = EngineSetting.COMPOSITE_UPLOAD_VERSION_UNINITIALIZED;
        private int maxInstances;
    }
}
