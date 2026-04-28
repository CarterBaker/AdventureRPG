package application.bootstrap.renderpipeline.renderqueue;

import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.compositebatch.CompositeBatchStruct;
import application.bootstrap.renderpipeline.renderbatch.RenderBatchStruct;
import application.bootstrap.renderpipeline.rendercall.RenderCallStruct;
import engine.root.EngineSetting;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RenderQueueHandle extends HandlePackage {

    private RenderCallStruct[] renderCallBuffer;
    int renderCallCursor;

    public Object2ObjectOpenHashMap<FboInstance, Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<RenderBatchStruct>>> fbo2Depth2MaterialBatches;
    public Object2ObjectOpenHashMap<FboInstance, Int2ObjectOpenHashMap<ObjectArrayList<RenderBatchStruct>>> fbo2Depth2BatchList;
    public Object2ObjectOpenHashMap<FboInstance, IntArrayList> fbo2DepthOrder;
    public ObjectArrayList<FboInstance> queuedFbos;

    public Int2ObjectOpenHashMap<RenderBatchStruct> screenMaterialBatches;
    public ObjectArrayList<RenderBatchStruct> screenBatchList;
    public Object2ObjectOpenHashMap<FboInstance, Int2ObjectOpenHashMap<CompositeBatchStruct>> fbo2CompositeMaterialBatches;
    public Object2ObjectOpenHashMap<FboInstance, ObjectArrayList<CompositeBatchStruct>> fbo2CompositeBatchList;
    public Int2ObjectOpenHashMap<CompositeBatchStruct> screenCompositeMaterialBatches;
    public ObjectArrayList<CompositeBatchStruct> screenCompositeBatchList;

    public void constructor() {

        this.renderCallBuffer = new RenderCallStruct[EngineSetting.MAX_RENDER_CALLS_PER_FRAME];
        for (int i = 0; i < renderCallBuffer.length; i++)
            renderCallBuffer[i] = new RenderCallStruct();

        this.fbo2Depth2MaterialBatches = new Object2ObjectOpenHashMap<>();
        this.fbo2Depth2BatchList = new Object2ObjectOpenHashMap<>();
        this.fbo2DepthOrder = new Object2ObjectOpenHashMap<>();
        this.queuedFbos = new ObjectArrayList<>();

        this.screenMaterialBatches = new Int2ObjectOpenHashMap<>();
        this.screenBatchList = new ObjectArrayList<>();
        this.fbo2CompositeMaterialBatches = new Object2ObjectOpenHashMap<>();
        this.fbo2CompositeBatchList = new Object2ObjectOpenHashMap<>();
        this.screenCompositeMaterialBatches = new Int2ObjectOpenHashMap<>();
        this.screenCompositeBatchList = new ObjectArrayList<>();
    }

    public RenderCallStruct nextCall() {
        return renderCallBuffer[renderCallCursor++];
    }

    public boolean isRenderBufferFull() {
        return renderCallCursor >= renderCallBuffer.length;
    }

    public void rewindFrame() {
        renderCallCursor = 0;
        queuedFbos.clear();
        fbo2Depth2MaterialBatches.clear();
        fbo2Depth2BatchList.clear();
        fbo2DepthOrder.clear();
        screenMaterialBatches.clear();
        screenBatchList.clear();
        fbo2CompositeMaterialBatches.clear();
        fbo2CompositeBatchList.clear();
        screenCompositeMaterialBatches.clear();
        screenCompositeBatchList.clear();
    }
}
