package application.bootstrap.renderpipeline.rendermanager;

import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.renderpipeline.compositerendersystem.CompositeRenderSystem;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbo.FboManager;
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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RenderSystem extends SystemPackage {

    private static final String DEFAULT_FBO = "MainScene";

    private CompositeRenderSystem compositeRenderSystem;
    private VAOManager vaoManager;

    @Override
    protected void get() {
        this.compositeRenderSystem = get(CompositeRenderSystem.class);
        this.vaoManager = get(VAOManager.class);
    }

    void drawToMappedTargets(WindowInstance window, FboManager fboManager) {

        RenderQueueHandle queue = window.getRenderQueueHandle();

        if (queue == null)
            return;

        Object[] fboNames = queue.queuedFboNames.elements();
        int fboCount = queue.queuedFboNames.size();

        for (int f = 0; f < fboCount; f++) {
            String fboName = (String) fboNames[f];
            FboInstance target = fboManager.getFbo(fboName);
            if (target == null)
                continue;

            bindTarget(window, target);
            GLSLUtility.enableDepth();
            GLSLUtility.enableBlending();
            GLSLUtility.disableCulling();
            GLSLUtility.clearBuffer();
            GLSLUtility.clearDepthBuffer();

            drawBatchesForFbo(queue.fbo2BatchList.get(fboName), window);
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

        Object[] fboNames = queue.queuedFboNames.elements();
        int fboCount = queue.queuedFboNames.size();

        for (int f = 0; f < fboCount; f++) {
            String fboName = (String) fboNames[f];
            drawBatchesForFbo(queue.fbo2BatchList.get(fboName), window);
        }

        drawBatchesForFbo(queue.screenBatchList, window);

        compositeRenderSystem.draw(window);
        queue.rewindFrame();

        if (target != null)
            target.unbind();
    }

    private void drawBatchesForFbo(ObjectArrayList<RenderBatchStruct> batchList, WindowInstance window) {
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
        GLSLUtility.enableDepth();
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

    void pushRenderCall(ModelInstance modelInstance, String fboName, MaskStruct mask, WindowInstance window) {

        RenderQueueHandle queue = window.getRenderQueueHandle();

        if (queue == null || queue.isRenderBufferFull())
            return;

        RenderCallStruct renderCall = queue.nextCall();
        renderCall.init(modelInstance, mask);

        String targetFbo = (fboName == null || fboName.isBlank()) ? DEFAULT_FBO : fboName;

        MaterialInstance material = modelInstance.getMaterial();
        int materialID = material.getMaterialID();

        Int2ObjectOpenHashMap<RenderBatchStruct> materialBatches = queue.fbo2MaterialBatches.get(targetFbo);

        if (materialBatches == null) {
            materialBatches = new Int2ObjectOpenHashMap<>();
            queue.fbo2MaterialBatches.put(targetFbo, materialBatches);
            queue.queuedFboNames.add(targetFbo);
            queue.fbo2BatchList.put(targetFbo, new ObjectArrayList<>());
        }

        RenderBatchStruct batch = materialBatches.get(materialID);

        if (batch == null) {
            batch = new RenderBatchStruct(material);
            materialBatches.put(materialID, batch);
            queue.fbo2BatchList.get(targetFbo).add(batch);
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