package application.bootstrap.renderpipeline.rendermanager;

import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.renderpipeline.compositerendersystem.CompositeRenderSystem;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.renderbatch.RenderBatchStruct;
import application.bootstrap.renderpipeline.rendercall.RenderCallStruct;
import application.bootstrap.renderpipeline.util.MaskStruct;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RenderSystem extends SystemPackage {

    private CompositeRenderSystem compositeRenderSystem;
    private VAOManager vaoManager;

    @Override
    protected void get() {
        this.compositeRenderSystem = get(CompositeRenderSystem.class);
        this.vaoManager = get(VAOManager.class);
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

            bindTarget(window, target);
            GLSLUtility.enableDepth();
            GLSLUtility.enableBlending();
            GLSLUtility.disableCulling();
            GLSLUtility.clearBuffer(0f, 0f, 0f, 0f);
            GLSLUtility.clearDepthBuffer();

            drawDepthSortedBatches(queue, target, window);
            target.unbind();
        }

        compositeRenderSystem.draw(window);
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

        drawScreenPass(queue, window);

        compositeRenderSystem.draw(window);
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

    private void drawScreenPass(RenderQueueHandle queue, WindowInstance window) {
        GLSLUtility.disableDepth();
        GLSLUtility.enableBlending();
        drawBatches(queue.screenBatchList, window);
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
                drawBatchedRenderCall(renderCall, window);
            }

            batch.clear();
        }

        if (activeMask != null)
            GLSLUtility.disableScissor();
    }

    private void bindTarget(WindowInstance window, FboInstance target) {
        if (target == null) {
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
                textureUnit++;
            }
            uniform.push();
        }
    }

    private void drawBatchedRenderCall(RenderCallStruct renderCall, WindowInstance window) {

        ModelInstance model = renderCall.getModelInstance();
        MeshData meshData = model.getMeshData();
        int vao = vaoManager.getVAOForWindow(meshData, window.getWindowID());

        GLSLUtility.bindVAO(vao);
        GLSLUtility.drawElements(model.getIndexCount());
        GLSLUtility.unbindVAO();
    }

    void pushCompositeCall(MaterialInstance material, CompositeBufferInstance buffer, WindowInstance window) {
        compositeRenderSystem.submit(material, buffer, window);
    }

    void removeWindowResources(WindowInstance window) {
        compositeRenderSystem.removeWindow(window.getWindowID());
    }

    void pushRenderCall(ModelInstance modelInstance, FboInstance fbo, int depth, MaskStruct mask, WindowInstance window) {

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
            queue.queuedFbos.add(fbo);
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

    void pushScreenCall(ModelInstance modelInstance, MaskStruct mask, WindowInstance window) {

        RenderQueueHandle queue = window.getRenderQueueHandle();

        if (queue == null || queue.isRenderBufferFull())
            return;

        RenderCallStruct renderCall = queue.nextCall();
        renderCall.init(modelInstance, mask);

        MaterialInstance material = modelInstance.getMaterial();
        int materialID = material.getMaterialID();

        RenderBatchStruct batch = queue.screenMaterialBatches.get(materialID);

        if (batch == null) {
            batch = new RenderBatchStruct(material);
            queue.screenMaterialBatches.put(materialID, batch);
            queue.screenBatchList.add(batch);
        }

        batch.addRenderCall(renderCall);
    }
}
