package com.internal.bootstrap.renderpipeline.rendersystem;

import com.internal.bootstrap.geometrypipeline.modelmanager.ModelHandle;
import com.internal.bootstrap.renderpipeline.renderbatch.RenderBatchHandle;
import com.internal.bootstrap.renderpipeline.rendercall.RenderCallHandle;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialHandle;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.WindowInstance;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class RenderSystem extends SystemPackage {

    // Internal
    private WindowInstance windowInstance;
    private Int2ObjectAVLTreeMap<Object2ObjectOpenHashMap<MaterialHandle, RenderBatchHandle>> depth2RenderBatchHandles;

    private int nextId;
    private IntOpenHashSet freeIds;
    private Int2IntOpenHashMap id2Depth;
    private Int2ObjectOpenHashMap<MaterialHandle> id2MaterialHandle;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.depth2RenderBatchHandles = new Int2ObjectAVLTreeMap<>();

        this.nextId = 0;
        this.freeIds = new IntOpenHashSet();
        this.id2Depth = new Int2IntOpenHashMap();
        this.id2MaterialHandle = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {

        // Internal
        this.windowInstance = internal.getWindowInstance();
    }

    @Override
    protected void awake() {
        GLSLUtility.enableDepth();
        GLSLUtility.enableBlending();
    }

    // Render System \\

    public void draw() {

        GLSLUtility.setViewport(
                windowInstance.getWidth(),
                windowInstance.getHeight());

        GLSLUtility.clearBuffer();

        // Iterate through each depth level
        for (var depthEntry : depth2RenderBatchHandles.int2ObjectEntrySet()) {

            int depth = depthEntry.getIntKey();
            var materialBatches = depthEntry.getValue();

            // Iterate through each material batch at this depth
            for (var batchEntry : materialBatches.entrySet()) {

                MaterialHandle material = batchEntry.getKey();
                RenderBatchHandle batch = batchEntry.getValue();

                // Bind material once for entire batch
                bindMaterial(material, depth);

                // Draw all render calls in this batch
                for (RenderCallHandle renderCall : batch.getRenderCalls())
                    drawBatchedRenderCall(renderCall);
            }
        }
    }

    private void bindMaterial(MaterialHandle material, int depth) {

        // Configure depth testing based on depth level
        if (depth == 0) // Standard 3D rendering with depth testing
            GLSLUtility.enableDepth();

        else // Post-processing passes typically don't use depth testing
            GLSLUtility.disableDepth();

        // Bind shader once for entire batch
        GLSLUtility.useShader(material.getShaderHandle().getShaderHandle());
    }

    private void drawBatchedRenderCall(RenderCallHandle renderCall) {

        ModelHandle modelHandle = renderCall.getModelHandle();

        // Bind VAO
        GLSLUtility.bindVAO(modelHandle.getVAOHandle());

        // Draw
        GLSLUtility.drawElements(modelHandle.getIndexCount());

        // Unbind
        GLSLUtility.unbindVAO();
    }

    // Accessible \\

    public RenderCallHandle pushRenderCall(ModelHandle modelHandle, int depth) {

        // Generate unique ID (reuse freed IDs first)
        int id;

        if (!freeIds.isEmpty()) {
            var iterator = freeIds.iterator();
            id = iterator.nextInt();
            iterator.remove();
        }

        else
            id = nextId++;

        // Create render call handle
        RenderCallHandle renderCall = create(RenderCallHandle.class);
        renderCall.constructor(id, modelHandle);

        // Get material from model
        MaterialHandle material = modelHandle.getMaterial();

        // Get or create material batches map for this depth
        Object2ObjectOpenHashMap<MaterialHandle, RenderBatchHandle> materialBatches = depth2RenderBatchHandles
                .computeIfAbsent(
                        depth,
                        k -> new Object2ObjectOpenHashMap<>());

        // Get or create batch for this material
        RenderBatchHandle batch = materialBatches.computeIfAbsent(
                material,
                k -> {
                    RenderBatchHandle newBatch = create(RenderBatchHandle.class);
                    newBatch.constructor(material);
                    return newBatch;
                });

        // Add render call to batch
        batch.addRenderCall(renderCall);

        // Track ID metadata
        id2Depth.put(id, depth);
        id2MaterialHandle.put(id, material);

        return renderCall;
    }

    public void pullRenderCall(RenderCallHandle renderCall) {

        if (renderCall == null)
            return;

        int id = renderCall.getHandle();

        // Check if this render call actually exists
        if (!id2Depth.containsKey(id))
            return;

        int depth = id2Depth.remove(id);
        MaterialHandle material = id2MaterialHandle.remove(id);

        // Get the material batches for this depth
        Object2ObjectOpenHashMap<MaterialHandle, RenderBatchHandle> materialBatches = depth2RenderBatchHandles
                .get(depth);

        if (materialBatches == null)
            return;

        // Get the batch for this material
        RenderBatchHandle batch = materialBatches.get(material);

        if (batch == null)
            return;

        // Remove render call from batch
        batch.removeRenderCall(renderCall);

        // Clean up empty batch
        if (batch.isEmpty()) {
            batch.dispose();
            materialBatches.remove(material);
        }

        // Clean up empty depth level
        if (materialBatches.isEmpty())
            depth2RenderBatchHandles.remove(depth);

        // Mark ID as free for reuse
        freeIds.add(id);

        // Dispose render call
        renderCall.dispose();
    }
}