// MOVED FROM: com.internal.bootstrap.renderpipeline.rendersystem
// MOVED TO:   com.internal.bootstrap.renderpipeline.rendermanager
//
// No logic changes. Package declaration updated only.
// RenderManager calls draw() directly — no longer called from GameEngine.

package com.internal.bootstrap.renderpipeline.rendermanager;

import com.internal.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.renderpipeline.compositerendersystem.CompositeRenderSystem;
import com.internal.bootstrap.renderpipeline.renderbatch.RenderBatchStruct;
import com.internal.bootstrap.renderpipeline.rendercall.RenderCallStruct;
import com.internal.bootstrap.renderpipeline.util.MaskStruct;
import com.internal.bootstrap.renderpipeline.window.WindowInstance;
import com.internal.bootstrap.renderpipeline.windowmanager.WindowManager;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubo.UBOInstance;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformStruct;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RenderSystem extends SystemPackage {

    /*
     * Collects render calls each frame sorted by depth and material, then
     * flushes them to the GPU in draw(). Viewport dimensions come from the
     * active window on WindowManager. Camera data is already pushed to the
     * shared UBO by CameraBufferSystem before draw() fires — RenderSystem
     * has no camera knowledge. Zero allocation per frame after warmup.
     */

    // Internal
    private WindowManager windowManager;
    private CompositeRenderSystem compositeRenderSystem;

    // Palette — lookup
    private Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<RenderBatchStruct>> depth2MaterialBatches;

    // Palette — iteration
    private IntArrayList sortedDepths;
    private Int2ObjectOpenHashMap<ObjectArrayList<RenderBatchStruct>> depth2BatchList;

    // Render Call Buffer
    private RenderCallStruct[] renderCallBuffer;
    private int renderCallCursor;

    // Internal \\

    @Override
    protected void create() {

        this.depth2MaterialBatches = new Int2ObjectOpenHashMap<>();
        this.sortedDepths = new IntArrayList();
        this.depth2BatchList = new Int2ObjectOpenHashMap<>();

        this.renderCallBuffer = new RenderCallStruct[EngineSetting.MAX_RENDER_CALLS_PER_FRAME];
        for (int i = 0; i < renderCallBuffer.length; i++)
            renderCallBuffer[i] = new RenderCallStruct();
    }

    @Override
    protected void get() {

        // Internal
        this.windowManager = get(WindowManager.class);
        this.compositeRenderSystem = get(CompositeRenderSystem.class);
    }

    @Override
    protected void awake() {
        GLSLUtility.enableDepth();
        GLSLUtility.enableBlending();
        GLSLUtility.disableCulling();
    }

    // Draw \\

    void draw() {

        WindowInstance window = windowManager.getActiveWindow();

        renderCallCursor = 0;

        GLSLUtility.setViewport(
                window.getWidth(),
                window.getHeight());

        GLSLUtility.clearBuffer();

        int[] depths = sortedDepths.elements();
        int depthCount = sortedDepths.size();

        for (int d = 0; d < depthCount; d++) {

            int depth = depths[d];

            GLSLUtility.clearDepthBuffer();

            ObjectArrayList<RenderBatchStruct> batchList = depth2BatchList.get(depth);
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
                    drawBatchedRenderCall(renderCall);
                }

                batch.clear();
            }

            if (activeMask != null)
                GLSLUtility.disableScissor();

            if (depth == 0)
                compositeRenderSystem.draw();
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

    private void drawBatchedRenderCall(RenderCallStruct renderCall) {
        ModelInstance model = renderCall.getModelInstance();
        GLSLUtility.bindVAO(model.getVAO());
        GLSLUtility.drawElements(model.getIndexCount());
        GLSLUtility.unbindVAO();
    }

    // Accessible \\

    public void pushCompositeCall(MaterialInstance material, CompositeBufferInstance buffer) {
        compositeRenderSystem.submit(material, buffer);
    }

    public void pushRenderCall(ModelInstance modelInstance, int depth) {
        pushRenderCall(modelInstance, depth, null);
    }

    public void pushRenderCall(ModelInstance modelInstance, int depth, MaskStruct mask) {

        RenderCallStruct renderCall = renderCallBuffer[renderCallCursor++];
        renderCall.init(modelInstance, mask);

        MaterialInstance material = modelInstance.getMaterial();
        int materialID = material.getMaterialID();

        Int2ObjectOpenHashMap<RenderBatchStruct> materialBatches = depth2MaterialBatches.get(depth);

        if (materialBatches == null) {
            materialBatches = new Int2ObjectOpenHashMap<>();
            depth2MaterialBatches.put(depth, materialBatches);
            insertDepthSorted(depth);
            depth2BatchList.put(depth, new ObjectArrayList<>());
        }

        RenderBatchStruct batch = materialBatches.get(materialID);

        if (batch == null) {
            batch = new RenderBatchStruct(material);
            materialBatches.put(materialID, batch);
            depth2BatchList.get(depth).add(batch);
        }

        batch.addRenderCall(renderCall);
    }

    // Depth Ordering \\

    private void insertDepthSorted(int depth) {

        int[] elements = sortedDepths.elements();
        int size = sortedDepths.size();

        for (int i = 0; i < size; i++) {
            if (depth < elements[i]) {
                sortedDepths.add(i, depth);
                return;
            }
        }

        sortedDepths.add(depth);
    }
}