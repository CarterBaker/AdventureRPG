package application.bootstrap.renderpipeline.render;

import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedBufferInstance;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RenderQueueHandle extends HandlePackage {

    /*
     * One window's render queue for the frame: the pooled render calls handed
     * out by cursor, and the batches they are sorted into per target FBO, depth
     * and material, plus the screen, composite and skinned batch lists. Rewound
     * after the window draws. Rewinding returns every batch, map and list to
     * the queue's pools instead of dropping them, so each frame rebuilds the
     * same structure without allocating; render and composite batches are
     * pooled per material so their cached source UBOs stay valid.
     */

    private RenderCallStruct[] renderCallBuffer;
    int renderCallCursor;
    public Object2ObjectOpenHashMap<FBOInstance, Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<RenderBatchStruct>>> //
            fbo2Depth2MaterialBatches;
    public Object2ObjectOpenHashMap<FBOInstance, Int2ObjectOpenHashMap<ObjectArrayList<RenderBatchStruct>>> //
            fbo2Depth2BatchList;
    public Object2ObjectOpenHashMap<FBOInstance, IntArrayList> fbo2DepthOrder;
    public ObjectArrayList<FBOInstance> queuedFbos;
    public Object2ObjectOpenHashMap<FBOInstance, WindowInstance> fbo2Window;
    public Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<RenderBatchStruct>> screenOrder2MaterialBatches;
    public Int2ObjectOpenHashMap<ObjectArrayList<RenderBatchStruct>> screenOrder2BatchList;
    public IntArrayList screenDepthOrder;
    public Object2ObjectOpenHashMap<FBOInstance, Int2ObjectOpenHashMap<CompositeBatchStruct>> //
            fbo2CompositeMaterialBatches;
    public Object2ObjectOpenHashMap<FBOInstance, ObjectArrayList<CompositeBatchStruct>> fbo2CompositeBatchList;
    public ObjectArrayList<FBOInstance> compositeFbos;
    public Int2ObjectOpenHashMap<CompositeBatchStruct> screenCompositeMaterialBatches;
    public ObjectArrayList<CompositeBatchStruct> screenCompositeBatchList;
    public Object2ObjectOpenHashMap<FBOInstance, ObjectArrayList<SkinnedBatchStruct>> fbo2SkinnedBatchList;

    // Pools
    private Int2ObjectOpenHashMap<ObjectArrayList<RenderBatchStruct>> materialID2RenderBatchPool;
    private Int2ObjectOpenHashMap<ObjectArrayList<CompositeBatchStruct>> materialID2CompositeBatchPool;
    private ObjectArrayList<SkinnedBatchStruct> skinnedBatchPool;
    private ObjectArrayList<Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<RenderBatchStruct>>> depthBatchMapPool;
    private ObjectArrayList<Int2ObjectOpenHashMap<ObjectArrayList<RenderBatchStruct>>> depthBatchListMapPool;
    private ObjectArrayList<Int2ObjectOpenHashMap<RenderBatchStruct>> materialBatchMapPool;
    private ObjectArrayList<ObjectArrayList<RenderBatchStruct>> batchListPool;
    private ObjectArrayList<IntArrayList> depthOrderPool;
    private ObjectArrayList<Int2ObjectOpenHashMap<CompositeBatchStruct>> compositeBatchMapPool;
    private ObjectArrayList<ObjectArrayList<CompositeBatchStruct>> compositeBatchListPool;
    private ObjectArrayList<ObjectArrayList<SkinnedBatchStruct>> skinnedBatchListPool;

    public void constructor() {
        this.renderCallBuffer = new RenderCallStruct[EngineSetting.MAX_RENDER_CALLS_PER_FRAME];
        for (int i = 0; i < renderCallBuffer.length; i++)
            renderCallBuffer[i] = new RenderCallStruct();
        this.fbo2Depth2MaterialBatches = new Object2ObjectOpenHashMap<>();
        this.fbo2Depth2BatchList = new Object2ObjectOpenHashMap<>();
        this.fbo2DepthOrder = new Object2ObjectOpenHashMap<>();
        this.queuedFbos = new ObjectArrayList<>();
        this.fbo2Window = new Object2ObjectOpenHashMap<>();
        this.screenOrder2MaterialBatches = new Int2ObjectOpenHashMap<>();
        this.screenOrder2BatchList = new Int2ObjectOpenHashMap<>();
        this.screenDepthOrder = new IntArrayList();
        this.fbo2CompositeMaterialBatches = new Object2ObjectOpenHashMap<>();
        this.fbo2CompositeBatchList = new Object2ObjectOpenHashMap<>();
        this.compositeFbos = new ObjectArrayList<>();
        this.screenCompositeMaterialBatches = new Int2ObjectOpenHashMap<>();
        this.screenCompositeBatchList = new ObjectArrayList<>();
        this.fbo2SkinnedBatchList = new Object2ObjectOpenHashMap<>();

        // Pools
        this.materialID2RenderBatchPool = new Int2ObjectOpenHashMap<>();
        this.materialID2CompositeBatchPool = new Int2ObjectOpenHashMap<>();
        this.skinnedBatchPool = new ObjectArrayList<>();
        this.depthBatchMapPool = new ObjectArrayList<>();
        this.depthBatchListMapPool = new ObjectArrayList<>();
        this.materialBatchMapPool = new ObjectArrayList<>();
        this.batchListPool = new ObjectArrayList<>();
        this.depthOrderPool = new ObjectArrayList<>();
        this.compositeBatchMapPool = new ObjectArrayList<>();
        this.compositeBatchListPool = new ObjectArrayList<>();
        this.skinnedBatchListPool = new ObjectArrayList<>();
    }

    public RenderCallStruct nextCall() {
        return renderCallBuffer[renderCallCursor++];
    }

    public boolean isRenderBufferFull() {
        return renderCallCursor >= renderCallBuffer.length;
    }

    public void rewindFrame() {

        renderCallCursor = 0;

        for (int i = 0; i < queuedFbos.size(); i++)
            recycleTarget(queuedFbos.get(i));

        for (int i = 0; i < compositeFbos.size(); i++)
            recycleCompositeTarget(compositeFbos.get(i));

        for (int i = 0; i < screenDepthOrder.size(); i++)
            recycleDepth(screenOrder2MaterialBatches, screenOrder2BatchList, screenDepthOrder.getInt(i));

        recycleCompositeBatches(screenCompositeBatchList);

        queuedFbos.clear();
        fbo2Depth2MaterialBatches.clear();
        fbo2Depth2BatchList.clear();
        fbo2DepthOrder.clear();
        fbo2Window.clear();
        screenOrder2MaterialBatches.clear();
        screenOrder2BatchList.clear();
        screenDepthOrder.clear();
        fbo2CompositeMaterialBatches.clear();
        fbo2CompositeBatchList.clear();
        compositeFbos.clear();
        screenCompositeMaterialBatches.clear();
        screenCompositeBatchList.clear();
        fbo2SkinnedBatchList.clear();
    }

    // Acquire \\

    public RenderBatchStruct acquireRenderBatch(MaterialInstance material) {

        ObjectArrayList<RenderBatchStruct> pool = materialID2RenderBatchPool.get(material.getMaterialID());
        RenderBatchStruct batch = pool == null || pool.isEmpty() ? new RenderBatchStruct() : pool.pop();

        batch.reset(material);
        return batch;
    }

    public CompositeBatchStruct acquireCompositeBatch(MaterialInstance material) {

        ObjectArrayList<CompositeBatchStruct> pool = materialID2CompositeBatchPool.get(material.getMaterialID());
        CompositeBatchStruct batch = pool == null || pool.isEmpty() ? new CompositeBatchStruct() : pool.pop();

        batch.reset(material);
        return batch;
    }

    public SkinnedBatchStruct acquireSkinnedBatch(SkinnedBufferInstance skinnedBuffer, MaterialInstance material) {

        SkinnedBatchStruct batch = skinnedBatchPool.isEmpty() ? new SkinnedBatchStruct() : skinnedBatchPool.pop();

        batch.reset(skinnedBuffer, material);
        return batch;
    }

    public Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<RenderBatchStruct>> acquireDepthBatchMap() {
        return depthBatchMapPool.isEmpty() ? new Int2ObjectOpenHashMap<>() : depthBatchMapPool.pop();
    }

    public Int2ObjectOpenHashMap<ObjectArrayList<RenderBatchStruct>> acquireDepthBatchListMap() {
        return depthBatchListMapPool.isEmpty() ? new Int2ObjectOpenHashMap<>() : depthBatchListMapPool.pop();
    }

    public Int2ObjectOpenHashMap<RenderBatchStruct> acquireMaterialBatchMap() {
        return materialBatchMapPool.isEmpty() ? new Int2ObjectOpenHashMap<>() : materialBatchMapPool.pop();
    }

    public ObjectArrayList<RenderBatchStruct> acquireBatchList() {
        return batchListPool.isEmpty() ? new ObjectArrayList<>() : batchListPool.pop();
    }

    public IntArrayList acquireDepthOrder() {
        return depthOrderPool.isEmpty() ? new IntArrayList() : depthOrderPool.pop();
    }

    public Int2ObjectOpenHashMap<CompositeBatchStruct> acquireCompositeBatchMap() {
        return compositeBatchMapPool.isEmpty() ? new Int2ObjectOpenHashMap<>() : compositeBatchMapPool.pop();
    }

    public ObjectArrayList<CompositeBatchStruct> acquireCompositeBatchList() {
        return compositeBatchListPool.isEmpty() ? new ObjectArrayList<>() : compositeBatchListPool.pop();
    }

    public ObjectArrayList<SkinnedBatchStruct> acquireSkinnedBatchList() {
        return skinnedBatchListPool.isEmpty() ? new ObjectArrayList<>() : skinnedBatchListPool.pop();
    }

    // Recycle \\

    // Every depth a target used is in its depth order, and every batch is in exactly one depth's list
    private void recycleTarget(FBOInstance fbo) {

        Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<RenderBatchStruct>> depth2MaterialBatches =
                fbo2Depth2MaterialBatches.get(fbo);

        if (depth2MaterialBatches != null) {

            Int2ObjectOpenHashMap<ObjectArrayList<RenderBatchStruct>> depth2BatchList = fbo2Depth2BatchList.get(fbo);
            IntArrayList depthOrder = fbo2DepthOrder.get(fbo);

            for (int i = 0; i < depthOrder.size(); i++)
                recycleDepth(depth2MaterialBatches, depth2BatchList, depthOrder.getInt(i));

            depth2MaterialBatches.clear();
            depthBatchMapPool.push(depth2MaterialBatches);
            depth2BatchList.clear();
            depthBatchListMapPool.push(depth2BatchList);
            depthOrder.clear();
            depthOrderPool.push(depthOrder);
        }

        ObjectArrayList<SkinnedBatchStruct> skinnedBatches = fbo2SkinnedBatchList.get(fbo);

        if (skinnedBatches == null)
            return;

        for (int i = 0; i < skinnedBatches.size(); i++)
            skinnedBatchPool.push(skinnedBatches.get(i));

        skinnedBatches.clear();
        skinnedBatchListPool.push(skinnedBatches);
    }

    private void recycleDepth(
            Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<RenderBatchStruct>> depth2MaterialBatches,
            Int2ObjectOpenHashMap<ObjectArrayList<RenderBatchStruct>> depth2BatchList,
            int depth) {

        Int2ObjectOpenHashMap<RenderBatchStruct> materialBatches = depth2MaterialBatches.get(depth);
        ObjectArrayList<RenderBatchStruct> batchList = depth2BatchList.get(depth);

        for (int i = 0; i < batchList.size(); i++)
            recycleRenderBatch(batchList.get(i));

        batchList.clear();
        batchListPool.push(batchList);
        materialBatches.clear();
        materialBatchMapPool.push(materialBatches);
    }

    private void recycleRenderBatch(RenderBatchStruct batch) {

        int materialID = batch.getRepresentativeMaterial().getMaterialID();
        ObjectArrayList<RenderBatchStruct> pool = materialID2RenderBatchPool.get(materialID);

        if (pool == null) {
            pool = new ObjectArrayList<>();
            materialID2RenderBatchPool.put(materialID, pool);
        }

        batch.clear();
        pool.push(batch);
    }

    private void recycleCompositeTarget(FBOInstance fbo) {

        Int2ObjectOpenHashMap<CompositeBatchStruct> materialBatches = fbo2CompositeMaterialBatches.get(fbo);
        ObjectArrayList<CompositeBatchStruct> batchList = fbo2CompositeBatchList.get(fbo);

        recycleCompositeBatches(batchList);
        batchList.clear();
        compositeBatchListPool.push(batchList);
        materialBatches.clear();
        compositeBatchMapPool.push(materialBatches);
    }

    private void recycleCompositeBatches(ObjectArrayList<CompositeBatchStruct> batchList) {

        for (int i = 0; i < batchList.size(); i++) {

            CompositeBatchStruct batch = batchList.get(i);
            int materialID = batch.getMaterial().getMaterialID();
            ObjectArrayList<CompositeBatchStruct> pool = materialID2CompositeBatchPool.get(materialID);

            if (pool == null) {
                pool = new ObjectArrayList<>();
                materialID2CompositeBatchPool.put(materialID, pool);
            }

            batch.clear();
            pool.push(batch);
        }
    }
}
