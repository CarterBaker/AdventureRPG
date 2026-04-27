package application.bootstrap.renderpipeline.rendermanager;

import application.bootstrap.renderpipeline.fbo.FboInstance;
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

    Object2ObjectOpenHashMap<FboInstance, Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<RenderBatchStruct>>> fbo2Depth2MaterialBatches;
    Object2ObjectOpenHashMap<FboInstance, Int2ObjectOpenHashMap<ObjectArrayList<RenderBatchStruct>>> fbo2Depth2BatchList;
    Object2ObjectOpenHashMap<FboInstance, IntArrayList> fbo2DepthOrder;
    ObjectArrayList<FboInstance> queuedFbos;

    Int2ObjectOpenHashMap<RenderBatchStruct> screenMaterialBatches;
    ObjectArrayList<RenderBatchStruct> screenBatchList;

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
    }

    RenderCallStruct nextCall() {
        return renderCallBuffer[renderCallCursor++];
    }

    boolean isRenderBufferFull() {
        return renderCallCursor >= renderCallBuffer.length;
    }

    void rewindFrame() {
        renderCallCursor = 0;
        queuedFbos.clear();
        fbo2Depth2MaterialBatches.clear();
        fbo2Depth2BatchList.clear();
        fbo2DepthOrder.clear();
        screenMaterialBatches.clear();
        screenBatchList.clear();
    }
}
