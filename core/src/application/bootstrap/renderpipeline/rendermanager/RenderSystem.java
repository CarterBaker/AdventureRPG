package application.bootstrap.renderpipeline.rendermanager;

import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedAppearanceStruct;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedBufferInstance;
import application.bootstrap.geometrypipeline.skinnedbuffermanager.SkinnedBufferManager;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.renderpipeline.cameramanager.CameraManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.render.MaskStruct;
import application.bootstrap.renderpipeline.render.RenderBatchStruct;
import application.bootstrap.renderpipeline.render.RenderCallStruct;
import application.bootstrap.renderpipeline.render.RenderQueueHandle;
import application.bootstrap.renderpipeline.render.SkinnedBatchStruct;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.shader.ShaderHandle;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RenderSystem extends SystemPackage {

    /*
     * Drives all draw submission and flushing for a window's render queue —
     * depth-sorted batches, screen passes, and skinned characters.
     */

    // Internal
    private CompositeRenderSystem compositeRenderSystem;
    private VAOManager vaoManager;
    private CameraManager cameraManager;
    private SkinnedBufferManager skinnedBufferManager;

    // Skinned VAO Cache
    private Int2ObjectOpenHashMap<Object2IntOpenHashMap<SkinnedBufferInstance>> windowID2SkinnedVAOCache;

    // Base \\

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

    // Draw \\

    void drawToMappedTargets(WindowInstance window) {

        RenderQueueHandle queue = window.getRenderQueueHandle();

        if (queue == null)
            return;

        Object[] fboObjects = queue.queuedFbos.elements();
        int fboCount = queue.queuedFbos.size();

        for (int f = 0; f < fboCount; f++) {
            FBOInstance target = (FBOInstance) fboObjects[f];
            if (target == null)
                continue;

            WindowInstance fboWindow = queue.fbo2Window.get(target);
            if (fboWindow != null)
                cameraManager.pushCamera(fboWindow);

            bindTarget(window, target);
            RenderGLSLUtility.enableDepth();

            if (target.getFboData().isPremultipliedBlend())
                RenderGLSLUtility.enablePremultipliedBlending();
            else
                RenderGLSLUtility.enableBlending();

            RenderGLSLUtility.disableCulling();
            Color clearColor = target.getFboData().getClearColor();
            RenderGLSLUtility.clearBuffer(clearColor.r, clearColor.g, clearColor.b, clearColor.a);
            RenderGLSLUtility.clearDepthBuffer();

            drawDepthSortedBatches(queue, target, window);
            drawSkinnedBatches(queue, target, window);

            compositeRenderSystem.draw(queue, target, window);
            target.unbind();
        }

        queue.rewindFrame();
    }

    void drawToTarget(WindowInstance window, FBOInstance target) {

        RenderQueueHandle queue = window.getRenderQueueHandle();

        if (queue == null)
            return;

        bindTarget(window, target);
        RenderGLSLUtility.enableDepth();
        RenderGLSLUtility.enableBlending();
        RenderGLSLUtility.disableCulling();
        RenderGLSLUtility.clearBuffer();
        RenderGLSLUtility.clearDepthBuffer();

        drawScreenPass(queue, window, EngineSetting.SCREEN_ORDER_BACKGROUND);
        compositeRenderSystem.drawScreen(queue, window);
        drawScreenPass(queue, window, EngineSetting.SCREEN_ORDER_FOREGROUND);

        queue.rewindFrame();

        if (target != null)
            target.unbind();
    }

    private void drawDepthSortedBatches(RenderQueueHandle queue, FBOInstance fbo, WindowInstance window) {
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
        RenderGLSLUtility.disableDepth();
        RenderGLSLUtility.enableBlending();
        drawBatches(batchList, window);
        RenderGLSLUtility.enableDepth();
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
                RenderGLSLUtility.setPatchVertices(representative.getPatchVertexCount());

            ObjectArrayList<RenderCallStruct> renderCalls = batch.getRenderCalls();
            Object[] callElements = renderCalls.elements();
            int callCount = renderCalls.size();

            for (int i = 0; i < callCount; i++) {

                RenderCallStruct renderCall = (RenderCallStruct) callElements[i];
                MaskStruct callMask = renderCall.getMask();

                if (callMask == null ? activeMask != null : !callMask.matches(activeMask)) {
                    if (callMask != null)
                        RenderGLSLUtility.enableScissor(
                                callMask.getX(), callMask.getY(),
                                callMask.getW(), callMask.getH());
                    else
                        RenderGLSLUtility.disableScissor();
                    activeMask = callMask;
                }

                pushInstanceUBOs(renderCall);
                pushInstanceUniforms(renderCall);
                drawBatchedRenderCall(renderCall, window, tessellated);
            }

            batch.clear();
        }

        if (activeMask != null)
            RenderGLSLUtility.disableScissor();
    }

    private void bindTarget(WindowInstance window, FBOInstance target) {
        if (target == null) {
            internal.windowPlatform.makeContextCurrent(window);
            RenderGLSLUtility.unbindFramebuffer();
            RenderGLSLUtility.setViewport(window.getWidth(), window.getHeight());
            return;
        }

        target.bind();
    }

    private void bindMaterial(MaterialInstance material) {
        RenderGLSLUtility.useShader(material.getShaderHandle().getGpuHandle());
    }

    private void bindSourceUBOs(RenderBatchStruct batch) {

        UBOHandle[] handles = batch.getCachedSourceUBOs();
        ShaderHandle shader = batch.getRepresentativeMaterial().getShaderHandle();

        for (int i = 0; i < handles.length; i++) {
            UBOHandle ubo = handles[i];
            bindUBO(shader, ubo.getBlockName(), ubo.getBindingPoint(), ubo.getGpuHandle());
        }
    }

    private void pushInstanceUBOs(RenderCallStruct renderCall) {

        UBOInstance[] instances = renderCall.getCachedInstanceUBOs();
        ShaderHandle shader = renderCall.getMaterialInstance().getShaderHandle();

        for (int i = 0; i < instances.length; i++) {
            UBOInstance ubo = instances[i];
            bindUBO(shader, ubo.getBlockName(), ubo.getBindingPoint(), ubo.getGpuHandle());
        }
    }

    private void bindUBO(ShaderHandle shader, String blockName, int bindingPoint, int gpuHandle) {

        if (shader.claimBlockBinding(bindingPoint))
            RenderGLSLUtility.bindUniformBlockToProgram(shader.getGpuHandle(), blockName, bindingPoint);

        RenderGLSLUtility.bindUniformBuffer(bindingPoint, gpuHandle);
    }

    private void pushInstanceUniforms(RenderCallStruct renderCall) {

        UniformStruct<?>[] uniforms = renderCall.getCachedUniforms();
        int textureUnit = 0;

        for (int i = 0; i < uniforms.length; i++) {

            UniformStruct<?> uniform = uniforms[i];

            if (uniform.attribute().isSampler()) {
                uniform.attribute().bindTexture(textureUnit);
                RenderGLSLUtility.bindSamplerUniform(uniform.getUniformHandle(), textureUnit);
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

        RenderGLSLUtility.bindVAO(vao);

        if (tessellated)
            RenderGLSLUtility.drawPatches(
                    model.getIndexCount() / EngineSetting.QUAD_INDEX_COUNT * EngineSetting.QUAD_VERTEX_COUNT);
        else
            RenderGLSLUtility.drawElements(model.getIndexCount());

        RenderGLSLUtility.unbindVAO();
    }

    // Submit \\

    void pushRenderCall(
            ModelInstance modelInstance,
            FBOInstance fbo,
            int depth,
            MaskStruct mask,
            WindowInstance window) {

        RenderQueueHandle queue = window.getRenderQueueHandle();

        if (queue == null || queue.isRenderBufferFull() || fbo == null)
            return;

        RenderCallStruct renderCall = queue.nextCall();
        renderCall.init(modelInstance, mask);

        MaterialInstance material = modelInstance.getMaterial();
        int materialID = material.getMaterialID();

        Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<RenderBatchStruct>> depth2MaterialBatches =
                queue.fbo2Depth2MaterialBatches.get(fbo);
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
            MaskStruct mask,
            FBOInstance fbo,
            WindowInstance window) {
        compositeRenderSystem.submit(material, buffer, mask, fbo, window);
    }

    void removeWindowResources(WindowInstance window) {
        compositeRenderSystem.removeWindow(window.getWindowID());
        removeSkinnedVAOs(window);
    }

    private void removeSkinnedVAOs(WindowInstance window) {

        Object2IntOpenHashMap<SkinnedBufferInstance> buffer2VAO = windowID2SkinnedVAOCache.remove(window.getWindowID());

        if (buffer2VAO == null)
            return;

        internal.windowPlatform.makeContextCurrent(window.getGLWindow());

        IntIterator iterator = buffer2VAO.values().iterator();

        while (iterator.hasNext())
            RenderGLSLUtility.deleteVAO(iterator.nextInt());

        internal.windowPlatform.restoreMainContext();
    }

    // Skinned \\

    void pushSkinnedCall(
            MeshHandle meshHandle,
            MaterialInstance material,
            Matrix4 modelMatrix,
            SkinnedAppearanceStruct appearance,
            Matrix4[] skinningMatrices,
            FBOInstance fbo,
            WindowInstance window) {

        RenderQueueHandle queue = window.getRenderQueueHandle();

        if (queue == null || fbo == null)
            return;

        SkinnedBufferInstance skinnedBuffer = skinnedBufferManager.getSkinnedBuffer(meshHandle, material);
        skinnedBuffer.addInstance(modelMatrix, appearance, skinningMatrices);

        ensureSkinnedBatchQueued(queue, fbo, skinnedBuffer, material, window);
    }

    private void ensureSkinnedBatchQueued(
            RenderQueueHandle queue,
            FBOInstance fbo,
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

    void ensureTargetQueued(FBOInstance fbo, WindowInstance window) {

        RenderQueueHandle queue = window.getRenderQueueHandle();

        if (queue == null || fbo == null)
            return;

        ensureFboQueued(queue, fbo, window);
    }

    private void ensureFboQueued(RenderQueueHandle queue, FBOInstance fbo, WindowInstance window) {

        if (queue.queuedFbos.contains(fbo))
            return;

        queue.queuedFbos.add(fbo);
        queue.fbo2Window.put(fbo, window);
    }

    private void drawSkinnedBatches(RenderQueueHandle queue, FBOInstance fbo, WindowInstance window) {

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
            material.setUniform(EngineSetting.UNIFORM_BONE_PALETTE, skinnedBuffer.getBonePaletteTexture());

            bindSkinnedMaterial(batch, material);

            int vao = getOrCreateSkinnedVAO(skinnedBuffer, window);

            RenderGLSLUtility.bindVAO(vao);
            RenderGLSLUtility.drawElementsInstanced(
                    skinnedBuffer.getMeshHandle().getIndexCount(),
                    skinnedBuffer.getInstanceCount());
            RenderGLSLUtility.unbindVAO();
        }
    }

    private void bindSkinnedMaterial(SkinnedBatchStruct batch, MaterialInstance material) {

        ShaderHandle shader = material.getShaderHandle();
        RenderGLSLUtility.useShader(shader.getGpuHandle());

        UBOHandle[] handles = batch.getCachedSourceUBOs();

        for (int i = 0; i < handles.length; i++) {
            UBOHandle ubo = handles[i];
            bindUBO(shader, ubo.getBlockName(), ubo.getBindingPoint(), ubo.getGpuHandle());
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
                RenderGLSLUtility.bindSamplerUniform(uniform.getUniformHandle(), textureUnit);
                textureUnit++;
            }

            else
                uniform.push();
        }
    }

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

        vao = RenderGLSLUtility.createInstancedVAO(
                skinnedBuffer.getMeshHandle().getVertexHandle(),
                skinnedBuffer.getMeshHandle().getAttrSizes(),
                skinnedBuffer.getMeshHandle().getIndexHandle(),
                skinnedBuffer.getInstanceVBO(),
                EngineSetting.SKINNED_INSTANCE_ATTRIBUTE_SIZES);

        buffer2VAO.put(skinnedBuffer, vao);

        return vao;
    }
}