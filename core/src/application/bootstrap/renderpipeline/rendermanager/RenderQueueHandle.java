package application.bootstrap.renderpipeline.rendermanager;

import application.bootstrap.renderpipeline.renderbatch.RenderBatchStruct;
import application.bootstrap.renderpipeline.rendercall.RenderCallStruct;
import engine.root.EngineSetting;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RenderQueueHandle extends HandlePackage {

    // Render Call Buffer
    private RenderCallStruct[] renderCallBuffer;
    int renderCallCursor;

    // Palette — lookup
    Object2ObjectOpenHashMap<String, Int2ObjectOpenHashMap<RenderBatchStruct>> fbo2MaterialBatches;

    // Palette — iteration
    ObjectArrayList<String> queuedFboNames;
    Object2ObjectOpenHashMap<String, ObjectArrayList<RenderBatchStruct>> fbo2BatchList;

    // Screen Queue
    Int2ObjectOpenHashMap<RenderBatchStruct> screenMaterialBatches;
    ObjectArrayList<RenderBatchStruct> screenBatchList;

    public void constructor() {

        this.renderCallBuffer = new RenderCallStruct[EngineSetting.MAX_RENDER_CALLS_PER_FRAME];
        for (int i = 0; i < renderCallBuffer.length; i++)
            renderCallBuffer[i] = new RenderCallStruct();

        this.fbo2MaterialBatches = new Object2ObjectOpenHashMap<>();
        this.queuedFboNames = new ObjectArrayList<>();
        this.fbo2BatchList = new Object2ObjectOpenHashMap<>();

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
        queuedFboNames.clear();
        fbo2MaterialBatches.clear();
        fbo2BatchList.clear();
        screenMaterialBatches.clear();
        screenBatchList.clear();
    }
}
