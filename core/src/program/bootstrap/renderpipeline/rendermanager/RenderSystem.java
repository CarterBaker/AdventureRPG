package program.bootstrap.renderpipeline.rendermanager;

import program.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import program.bootstrap.geometrypipeline.mesh.MeshData;
import program.bootstrap.geometrypipeline.model.ModelInstance;
import program.bootstrap.geometrypipeline.vaomanager.VAOManager;
import program.bootstrap.renderpipeline.compositerendersystem.CompositeRenderSystem;
import program.bootstrap.renderpipeline.renderbatch.RenderBatchStruct;
import program.bootstrap.renderpipeline.rendercall.RenderCallStruct;
import program.bootstrap.renderpipeline.util.MaskStruct;
import program.bootstrap.shaderpipeline.material.MaterialInstance;
import program.bootstrap.shaderpipeline.ubo.UBOHandle;
import program.bootstrap.shaderpipeline.ubo.UBOInstance;
import program.bootstrap.shaderpipeline.uniforms.UniformStruct;
import program.core.engine.SystemPackage;
import program.core.kernel.window.WindowInstance;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RenderSystem extends SystemPackage {

    /*
     * Collects render calls into a specific window's RenderQueueHandle and
     * flushes a window's queue on draw(). All push methods take an explicit
     * WindowInstance — no active window state, no WindowManager dependency.
     * The window's queue is read directly via window.getRenderQueueHandle().
     * Zero allocation per frame after warmup.
     */

    // Internal
    private CompositeRenderSystem compositeRenderSystem;
    private VAOManager vaoManager;

    // Internal \\

    @Override
    protected void get() {
        this.compositeRenderSystem = get(CompositeRenderSystem.class);
        this.vaoManager = get(VAOManager.class);
    }

    @Override
    protected void awake() {
        GLSLUtility.enableDepth();
        GLSLUtility.enableBlending();
        GLSLUtility.disableCulling();
    }

    // Draw \\

    void draw(WindowInstance window) {

        RenderQueueHandle queue = window.getRenderQueueHandle();

        if (queue == null || queue.isRenderBufferFull())
            return;

        queue.renderCallCursor = 0;

        GLSLUtility.setViewport(window.getWidth(), window.getHeight());
        GLSLUtility.clearBuffer();

        int[] depths = queue.sortedDepths.elements();
        int depthCount = queue.sortedDepths.size();

        for (int d = 0; d < depthCount; d++) {

            int depth = depths[d];

            GLSLUtility.clearDepthBuffer();

            ObjectArrayList<RenderBatchStruct> batchList = queue.depth2BatchList.get(depth);
            Object[] batchElements = batchList.elements();
            int batchCount = batchList.size();

            MaskStruct activeMask = null;

            for (int b = 0; b < batchCount; b++) {

                RenderBatchStruct batch = (RenderBatchStruct) batchElements[b];

                if (batch.isEmpty())
                    continue;

                MaterialInstance representative = batch.getRepresentativeMaterial();
                bindMaterial(representative, depth);
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

            if (depth == 0)
                compositeRenderSystem.draw(window);
        }
    }

    // Render Binding \\

    private void bindMaterial(MaterialInstance material, int depth) {

        if (depth == 0)
            GLSLUtility.enableDepth();
        else
            GLSLUtility.disableDepth();

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

    // Push \\

    void pushCompositeCall(MaterialInstance material, CompositeBufferInstance buffer, WindowInstance window) {
        compositeRenderSystem.submit(material, buffer, window);
    }

    void removeWindowResources(WindowInstance window) {
        compositeRenderSystem.removeWindow(window.getWindowID());
    }

    void pushRenderCall(ModelInstance modelInstance, int depth, MaskStruct mask, WindowInstance window) {

        RenderQueueHandle queue = window.getRenderQueueHandle();

        if (queue == null)
            return;

        RenderCallStruct renderCall = queue.nextCall();
        renderCall.init(modelInstance, mask);

        MaterialInstance material = modelInstance.getMaterial();
        int materialID = material.getMaterialID();

        Int2ObjectOpenHashMap<RenderBatchStruct> materialBatches = queue.depth2MaterialBatches.get(depth);

        if (materialBatches == null) {
            materialBatches = new Int2ObjectOpenHashMap<>();
            queue.depth2MaterialBatches.put(depth, materialBatches);
            insertDepthSorted(queue, depth);
            queue.depth2BatchList.put(depth, new ObjectArrayList<>());
        }

        RenderBatchStruct batch = materialBatches.get(materialID);

        if (batch == null) {
            batch = new RenderBatchStruct(material);
            materialBatches.put(materialID, batch);
            queue.depth2BatchList.get(depth).add(batch);
        }

        batch.addRenderCall(renderCall);
    }

    // Depth Ordering \\

    private void insertDepthSorted(RenderQueueHandle queue, int depth) {

        int[] elements = queue.sortedDepths.elements();
        int size = queue.sortedDepths.size();

        for (int i = 0; i < size; i++) {
            if (depth < elements[i]) {
                queue.sortedDepths.add(i, depth);
                return;
            }
        }

        queue.sortedDepths.add(depth);
    }
}