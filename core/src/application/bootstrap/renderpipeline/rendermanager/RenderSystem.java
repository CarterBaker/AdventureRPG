package application.bootstrap.renderpipeline.rendermanager;

import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedBufferInstance;
import application.bootstrap.geometrypipeline.skinnedbuffermanager.SkinnedBufferManager;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.renderpipeline.cameramanager.CameraManager;
import application.bootstrap.renderpipeline.compositerendersystem.CompositeRenderSystem;
import application.bootstrap.renderpipeline.fbo.FboData;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.renderbatch.RenderBatchStruct;
import application.bootstrap.renderpipeline.rendercall.RenderCallStruct;
import application.bootstrap.renderpipeline.renderqueue.RenderQueueHandle;
import application.bootstrap.renderpipeline.skinnedbatch.SkinnedBatchStruct;
import application.bootstrap.renderpipeline.util.MaskStruct;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RenderSystem extends SystemPackage {

    private CompositeRenderSystem compositeRenderSystem;
    private VAOManager vaoManager;
    private CameraManager cameraManager;
    private SkinnedBufferManager skinnedBufferManager;

    // Skinned — per-window instanced VAO cache. VAOs are context-local and
    // cannot be shared, so each window gets its own compiled VAO the first
    // time a given SkinnedBufferInstance is drawn in it, mirroring
    // CompositeRenderSystem.windowID2BufferGpuState.
    private Int2ObjectOpenHashMap<Object2IntOpenHashMap<SkinnedBufferInstance>> windowID2SkinnedVAOCache;

    @Override
    protected void create() {
        this.windowID2SkinnedVAOCache = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.compositeRenderSystem = get(CompositeRenderSystem.class);
        this.vaoManager = get(VAOManager.class);
        this.cameraManager = get(CameraManager.class);
        this.skinnedBufferManager = get(SkinnedBufferManager.class);
    }

    void drawToMappedTargets(WindowInstance window) {

        RenderQueueHandle queue = window.getRenderQueueHandle();

        if (queue == null)
            return;

        Object[] fboObjects = queue.queuedFbos.elements();
        int fboCount = queue.queuedFbos.size();

        for (int f = 0; f < fboCount; f++) {
            FboInstance target = (FboInstance) fboObjects[f];
            if (target == null)
                continue;

            WindowInstance fboWindow = queue.fbo2Window.get(target);
            if (fboWindow != null)
                cameraManager.pushCamera(fboWindow);

            bindTarget(window, target);
            GLSLUtility.enableDepth();
            GLSLUtility.enableBlending();
            GLSLUtility.disableCulling();
            GLSLUtility.clearBuffer(0f, 0f, 0f, 0f);
            GLSLUtility.clearDepthBuffer();

            drawDepthSortedBatches(queue, target, window);
            drawSkinnedBatches(queue, target, window);
            compositeRenderSystem.draw(queue, target, window);
            target.unbind();
        }

        queue.rewindFrame();
    }

    void drawToTarget(WindowInstance window, FboInstance target) {

        RenderQueueHandle queue = window.getRenderQueueHandle();

        if (queue == null)
            return;

        bindTarget(window, target);
        GLSLUtility.enableDepth();
        GLSLUtility.enableBlending();
        GLSLUtility.disableCulling();
        GLSLUtility.clearBuffer();
        GLSLUtility.clearDepthBuffer();

        drawScreenPass(queue, window, 0);
        compositeRenderSystem.drawScreen(queue, window);
        drawScreenPass(queue, window, 1);

        queue.rewindFrame();

        if (target != null)
            target.unbind();
    }

    private void drawDepthSortedBatches(RenderQueueHandle queue, FboInstance fbo, WindowInstance window) {
        IntArrayList depthOrder = queue.fbo2DepthOrder.get(fbo);
        Int2ObjectOpenHashMap<ObjectArrayList<RenderBatchStruct>> depth2BatchList = queue.fbo2Depth2BatchList.get(fbo);

        if (depthOrder == null || depth2BatchList == null)
            return;

        for (int i = 0; i < depthOrder.size(); i++) {
            int depth = depthOrder.getInt(i);
            drawBatches(depth2BatchList.get(depth), window);
        }
    }

    private void drawScreenPass(RenderQueueHandle queue, WindowInstance window, int order) {
        ObjectArrayList<RenderBatchStruct> batchList = queue.screenOrder2BatchList.get(order);
        if (batchList == null)
            return;
        GLSLUtility.disableDepth();
        GLSLUtility.enableBlending();
        drawBatches(batchList, window);
        GLSLUtility.enableDepth();
    }

    private void drawBatches(ObjectArrayList<RenderBatchStruct> batchList, WindowInstance window) {
        if (batchList == null)
            return;

        Object[] batchElements = batchList.elements();
        int batchCount = batchList.size();

        MaskStruct activeMask = null;

        for (int b = 0; b < batchCount; b++) {

            RenderBatchStruct batch = (RenderBatchStruct) batchElements[b];

            if (batch.isEmpty())
                continue;

            MaterialInstance representative = batch.getRepresentativeMaterial();
            bindMaterial(representative);
            bindSourceUBOs(batch);

            boolean tessellated = representative.usesTessellation();
            if (tessellated)
                GLSLUtility.setPatchVertices(representative.getPatchVertexCount());

            ObjectArrayList<RenderCallStruct> renderCalls = batch.getRenderCalls();
            Object[] callElements = renderCalls.elements();
            int callCount = renderCalls.size();

            for (int i = 0; i < callCount; i++) {

                RenderCallStruct renderCall = (RenderCallStruct) callElements[i];
                MaskStruct callMask = renderCall.getMask();

                if (callMask != activeMask) {
                    if (callMask != null)
                        GLSLUtility.enableScissor(
                                callMask.getX(), callMask.getY(),
                                callMask.getW(), callMask.getH());
                    else
                        GLSLUtility.disableScissor();
                    activeMask = callMask;
                }

                pushInstanceUBOs(renderCall);
                pushInstanceUniforms(renderCall);
                drawBatchedRenderCall(renderCall, window, tessellated);
            }

            batch.clear();
        }

        if (activeMask != null)
            GLSLUtility.disableScissor();
    }

    private void bindTarget(WindowInstance window, FboInstance target) {
        if (target == null) {
            internal.windowPlatform.makeContextCurrent(window);
            GLSLUtility.unbindFramebuffer();
            GLSLUtility.setViewport(window.getWidth(), window.getHeight());
            return;
        }

        target.bind();
    }

    private void bindMaterial(MaterialInstance material) {
        GLSLUtility.useShader(material.getShaderHandle().getGpuHandle());
    }

    private void bindSourceUBOs(RenderBatchStruct batch) {

        UBOHandle[] handles = batch.getCachedSourceUBOs();

        if (handles.length == 0)
            return;

        int shaderHandle = batch.getRepresentativeMaterial().getShaderHandle().getGpuHandle();

        for (int i = 0; i < handles.length; i++) {
            UBOHandle ubo = handles[i];
            GLSLUtility.bindUniformBlockToProgram(shaderHandle, ubo.getBlockName(), ubo.getBindingPoint());
            GLSLUtility.bindUniformBuffer(ubo.getBindingPoint(), ubo.getGpuHandle());
        }
    }

    private void pushInstanceUBOs(RenderCallStruct renderCall) {

        UBOInstance[] instances = renderCall.getCachedInstanceUBOs();

        for (int i = 0; i < instances.length; i++) {
            UBOInstance ubo = instances[i];
            GLSLUtility.bindUniformBuffer(ubo.getBindingPoint(), ubo.getGpuHandle());
        }
    }

    private void pushInstanceUniforms(RenderCallStruct renderCall) {

        UniformStruct<?>[] uniforms = renderCall.getCachedUniforms();
        int textureUnit = 0;

        for (int i = 0; i < uniforms.length; i++) {

            UniformStruct<?> uniform = uniforms[i];

            if (uniform.attribute().isSampler()) {
                uniform.attribute().bindTexture(textureUnit);
                GLSLUtility.bindSamplerUniform(uniform.getUniformHandle(), textureUnit);
                textureUnit++;
            }

            else
                uniform.push();
        }
    }

    private void drawBatchedRenderCall(RenderCallStruct renderCall, WindowInstance window, boolean tessellated) {

        ModelInstance model = renderCall.getModelInstance();
        MeshData meshData = model.getMeshData();
        int vao = vaoManager.getVAOForWindow(meshData, window.getWindowID());

        GLSLUtility.bindVAO(vao);

        if (tessellated)
            GLSLUtility.drawPatches(
                    model.getIndexCount() / EngineSetting.QUAD_INDEX_COUNT * EngineSetting.QUAD_VERTEX_COUNT);
        else
            GLSLUtility.drawElements(model.getIndexCount());

        GLSLUtility.unbindVAO();
    }

    void pushRenderCall(ModelInstance modelInstance, FboInstance fbo, int depth, MaskStruct mask,
            WindowInstance window) {

        RenderQueueHandle queue = window.getRenderQueueHandle();

        if (queue == null || queue.isRenderBufferFull() || fbo == null)
            return;

        RenderCallStruct renderCall = queue.nextCall();
        renderCall.init(modelInstance, mask);

        MaterialInstance material = modelInstance.getMaterial();
        int materialID = material.getMaterialID();

        Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<RenderBatchStruct>> depth2MaterialBatches = queue.fbo2Depth2MaterialBatches
                .get(fbo);
        if (depth2MaterialBatches == null) {
            depth2MaterialBatches = new Int2ObjectOpenHashMap<>();
            queue.fbo2Depth2MaterialBatches.put(fbo, depth2MaterialBatches);
            queue.fbo2Depth2BatchList.put(fbo, new Int2ObjectOpenHashMap<>());
            queue.fbo2DepthOrder.put(fbo, new IntArrayList());
            ensureFboQueued(queue, fbo, window);
        }

        Int2ObjectOpenHashMap<RenderBatchStruct> materialBatches = depth2MaterialBatches.get(depth);
        if (materialBatches == null) {
            materialBatches = new Int2ObjectOpenHashMap<>();
            depth2MaterialBatches.put(depth, materialBatches);
            queue.fbo2Depth2BatchList.get(fbo).put(depth, new ObjectArrayList<>());

            IntArrayList depths = queue.fbo2DepthOrder.get(fbo);
            int index = 0;
            while (index < depths.size() && depths.getInt(index) < depth)
                index++;
            depths.add(index, depth);
        }

        RenderBatchStruct batch = materialBatches.get(materialID);
        if (batch == null) {
            batch = new RenderBatchStruct(material);
            materialBatches.put(materialID, batch);
            queue.fbo2Depth2BatchList.get(fbo).get(depth).add(batch);
        }

        batch.addRenderCall(renderCall);
    }

    void pushScreenCall(ModelInstance modelInstance, MaskStruct mask, WindowInstance window, int order) {

        RenderQueueHandle queue = window.getRenderQueueHandle();

        if (queue == null || queue.isRenderBufferFull())
            return;

        RenderCallStruct renderCall = queue.nextCall();
        renderCall.init(modelInstance, mask);

        MaterialInstance material = modelInstance.getMaterial();
        int materialID = material.getMaterialID();

        Int2ObjectOpenHashMap<RenderBatchStruct> materialBatches = queue.screenOrder2MaterialBatches.get(order);
        if (materialBatches == null) {
            materialBatches = new Int2ObjectOpenHashMap<>();
            queue.screenOrder2MaterialBatches.put(order, materialBatches);
            queue.screenOrder2BatchList.put(order, new ObjectArrayList<>());

            IntArrayList orders = queue.screenDepthOrder;
            int index = 0;
            while (index < orders.size() && orders.getInt(index) < order)
                index++;
            orders.add(index, order);
        }

        RenderBatchStruct batch = materialBatches.get(materialID);
        if (batch == null) {
            batch = new RenderBatchStruct(material);
            materialBatches.put(materialID, batch);
            queue.screenOrder2BatchList.get(order).add(batch);
        }

        batch.addRenderCall(renderCall);
    }

    void pushCompositeCall(
            MaterialInstance material,
            CompositeBufferInstance buffer,
            FboInstance fbo,
            WindowInstance window) {
        compositeRenderSystem.submit(material, buffer, fbo, window);
    }

    void removeWindowResources(WindowInstance window) {
        compositeRenderSystem.removeWindow(window.getWindowID());
    }

    // Skinned \\

    /*
     * Queues one entity's contribution into the shared instanced buffer for
     * its (mesh, material) pair, creating that buffer on first use. Actual
     * upload and drawing happen once per buffer in drawSkinnedBatches, not
     * here — this only ever appends CPU-side instance data and makes sure
     * the batch is registered against this fbo for the current frame.
     */
    void pushSkinnedCall(
            MeshHandle meshHandle,
            MaterialInstance material,
            Matrix4 modelMatrix,
            Matrix4[] skinningMatrices,
            FboInstance fbo,
            WindowInstance window) {

        RenderQueueHandle queue = window.getRenderQueueHandle();

        if (queue == null || fbo == null)
            return;

        SkinnedBufferInstance skinnedBuffer = skinnedBufferManager.getSkinnedBuffer(meshHandle, material);
        skinnedBuffer.addInstance(modelMatrix, skinningMatrices);

        ensureSkinnedBatchQueued(queue, fbo, skinnedBuffer, material, window);
    }

    private void ensureSkinnedBatchQueued(
            RenderQueueHandle queue,
            FboInstance fbo,
            SkinnedBufferInstance skinnedBuffer,
            MaterialInstance material,
            WindowInstance window) {

        ObjectArrayList<SkinnedBatchStruct> batches = queue.fbo2SkinnedBatchList.get(fbo);

        if (batches == null) {
            batches = new ObjectArrayList<>();
            queue.fbo2SkinnedBatchList.put(fbo, batches);
        }

        Object[] elements = batches.elements();
        int count = batches.size();

        for (int i = 0; i < count; i++)
            if (((SkinnedBatchStruct) elements[i]).getSkinnedBuffer() == skinnedBuffer)
                return;

        batches.add(new SkinnedBatchStruct(skinnedBuffer, material));
        ensureFboQueued(queue, fbo, window);
    }

    private void ensureFboQueued(RenderQueueHandle queue, FboInstance fbo, WindowInstance window) {

        if (queue.queuedFbos.contains(fbo))
            return;

        queue.queuedFbos.add(fbo);
        queue.fbo2Window.put(fbo, window);
    }

    private void drawSkinnedBatches(RenderQueueHandle queue, FboInstance fbo, WindowInstance window) {

        ObjectArrayList<SkinnedBatchStruct> batches = queue.fbo2SkinnedBatchList.get(fbo);

        if (batches == null || batches.isEmpty())
            return;

        Object[] elements = batches.elements();
        int count = batches.size();

        for (int i = 0; i < count; i++) {

            SkinnedBatchStruct batch = (SkinnedBatchStruct) elements[i];
            SkinnedBufferInstance skinnedBuffer = batch.getSkinnedBuffer();

            if (skinnedBuffer.isEmpty())
                continue;

            skinnedBufferManager.upload(skinnedBuffer);

            MaterialInstance material = batch.getMaterial();
            material.setUniform("u_bonePalette", skinnedBuffer.getBonePaletteTexture());

            bindSkinnedMaterial(batch, material);

            int vao = getOrCreateSkinnedVAO(skinnedBuffer, window);

            GLSLUtility.bindVAO(vao);
            GLSLUtility.drawElementsInstanced(
                    skinnedBuffer.getMeshHandle().getIndexCount(),
                    skinnedBuffer.getInstanceCount());
            GLSLUtility.unbindVAO();
        }
    }

    private void bindSkinnedMaterial(SkinnedBatchStruct batch, MaterialInstance material) {

        int shaderHandle = material.getShaderHandle().getGpuHandle();
        GLSLUtility.useShader(shaderHandle);

        UBOHandle[] handles = batch.getCachedSourceUBOs();

        for (int i = 0; i < handles.length; i++) {
            UBOHandle ubo = handles[i];
            GLSLUtility.bindUniformBlockToProgram(shaderHandle, ubo.getBlockName(), ubo.getBindingPoint());
            GLSLUtility.bindUniformBuffer(ubo.getBindingPoint(), ubo.getGpuHandle());
        }

        pushMaterialUniforms(material);
    }

    private void pushMaterialUniforms(MaterialInstance material) {

        var keys = material.getUniformKeys();

        if (keys == null || keys.isEmpty())
            return;

        int textureUnit = 0;

        for (int i = 0; i < keys.size(); i++) {

            UniformStruct<?> uniform = material.getUniform(keys.get(i));

            if (uniform.attribute().isSampler()) {
                uniform.attribute().bindTexture(textureUnit);
                GLSLUtility.bindSamplerUniform(uniform.getUniformHandle(), textureUnit);
                textureUnit++;
            }

            else
                uniform.push();
        }
    }

    /*
     * One instanced VAO per (window, SkinnedBufferInstance) pair — never
     * shared across windows, since VAOs are context-local. The instance
     * attribute layout is always {4,4,4,4}: the four vec4 columns GLSL
     * packs automatically into a single mat4 instance attribute.
     */
    private int getOrCreateSkinnedVAO(SkinnedBufferInstance skinnedBuffer, WindowInstance window) {

        int windowID = window.getWindowID();
        Object2IntOpenHashMap<SkinnedBufferInstance> buffer2VAO = windowID2SkinnedVAOCache.get(windowID);

        if (buffer2VAO == null) {
            buffer2VAO = new Object2IntOpenHashMap<>();
            buffer2VAO.defaultReturnValue(0);
            windowID2SkinnedVAOCache.put(windowID, buffer2VAO);
        }

        int vao = buffer2VAO.getInt(skinnedBuffer);

        if (vao != 0)
            return vao;

        int[] instanceAttrSizes = { 4, 4, 4, 4 };

        vao = GLSLUtility.createInstancedVAO(
                skinnedBuffer.getMeshHandle().getVertexHandle(),
                skinnedBuffer.getMeshHandle().getAttrSizes(),
                skinnedBuffer.getMeshHandle().getIndexHandle(),
                skinnedBuffer.getInstanceVBO(),
                instanceAttrSizes);

        buffer2VAO.put(skinnedBuffer, vao);

        return vao;
    }
}