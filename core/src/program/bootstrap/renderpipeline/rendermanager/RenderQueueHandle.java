package program.bootstrap.renderpipeline.rendermanager;

import program.bootstrap.renderpipeline.renderbatch.RenderBatchStruct;
import program.bootstrap.renderpipeline.rendercall.RenderCallStruct;
import program.core.engine.HandlePackage;
import program.core.settings.EngineSetting;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RenderQueueHandle extends HandlePackage {

    /*
     * Per-window frame render queue. Created by RenderManager when a context
     * is paired with a window and held on ContextPackage for its lifetime.
     * Owns the pre-allocated render call buffer and all batch structures for
     * one OS window. RenderSystem is the only class that reads and writes
     * internal state. Zero allocation per frame after warmup.
     */

    // Render Call Buffer
    private RenderCallStruct[] renderCallBuffer;
    int renderCallCursor;

    // Palette — lookup
    Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<RenderBatchStruct>> depth2MaterialBatches;

    // Palette — iteration
    IntArrayList sortedDepths;
    Int2ObjectOpenHashMap<ObjectArrayList<RenderBatchStruct>> depth2BatchList;

    // Internal \\

    public void constructor() {

        // Render Call Buffer
        this.renderCallBuffer = new RenderCallStruct[EngineSetting.MAX_RENDER_CALLS_PER_FRAME];
        for (int i = 0; i < renderCallBuffer.length; i++)
            renderCallBuffer[i] = new RenderCallStruct();

        // Palette
        this.depth2MaterialBatches = new Int2ObjectOpenHashMap<>();
        this.sortedDepths = new IntArrayList();
        this.depth2BatchList = new Int2ObjectOpenHashMap<>();
    }

    // Accessible \\

    RenderCallStruct nextCall() {
        return renderCallBuffer[renderCallCursor++];
    }

    boolean isRenderBufferFull() {
        return renderCallCursor >= renderCallBuffer.length;
    }
}