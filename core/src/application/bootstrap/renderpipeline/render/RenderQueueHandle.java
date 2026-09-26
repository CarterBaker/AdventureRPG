package application.bootstrap.renderpipeline.render;

import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.render.RenderBatchStruct;
import application.bootstrap.renderpipeline.render.RenderCallStruct;
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
     * after the window draws.
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
    public Int2ObjectOpenHashMap<CompositeBatchStruct> screenCompositeMaterialBatches;
    public ObjectArrayList<CompositeBatchStruct> screenCompositeBatchList;
    public Object2ObjectOpenHashMap<FBOInstance, ObjectArrayList<SkinnedBatchStruct>> fbo2SkinnedBatchList;

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
        this.screenCompositeMaterialBatches = new Int2ObjectOpenHashMap<>();
        this.screenCompositeBatchList = new ObjectArrayList<>();
        this.fbo2SkinnedBatchList = new Object2ObjectOpenHashMap<>();
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
        fbo2Window.clear();
        screenOrder2MaterialBatches.clear();
        screenOrder2BatchList.clear();
        screenDepthOrder.clear();
        fbo2CompositeMaterialBatches.clear();
        fbo2CompositeBatchList.clear();
        screenCompositeMaterialBatches.clear();
        screenCompositeBatchList.clear();
        fbo2SkinnedBatchList.clear();
    }
}