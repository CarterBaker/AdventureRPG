package application.bootstrap.menupipeline.fontrendersystem;

import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.geometrypipeline.compositebuffermanager.CompositeBufferManager;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.menupipeline.font.FontInstance;
import application.bootstrap.renderpipeline.util.MaskStruct;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class FontRenderSystem extends SystemPackage {

    /*
     * Bridges menu font submissions into the shared render pipeline composite
     * path. This system does not issue GL draw/upload calls directly.
     *
     * For each window + FontInstance pair we keep one CompositeBufferInstance
     * containing per-glyph instance data in the Label shader layout:
     * [screenX, screenY, screenW, screenH, atlasU, atlasV, atlasUW, atlasVH].
     *
     * MenuRenderSystem calls submit() while traversing visible elements; each
     * submit updates that font's composite buffer for the current frame and
     * pushes the batch into RenderManager via pushCompositeCall().
     */

    private static final int[] INSTANCE_ATTR_SIZES = { 2, 2, 4 };
    private static final int FLOATS_PER_INSTANCE = 8;

    // Internal
    private RenderManager renderManager;
    private MeshManager meshManager;
    private CompositeBufferManager compositeBufferManager;
    private MeshHandle fontQuadMesh;

    // Per-window per-font state
    private Int2ObjectOpenHashMap<Object2ObjectOpenHashMap<FontInstance, FontCompositeState>> windowID2FontState;

    // Internal \\

    @Override
    protected void create() {
        this.windowID2FontState = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.renderManager = get(RenderManager.class);
        this.meshManager = get(MeshManager.class);
        this.compositeBufferManager = get(CompositeBufferManager.class);
        this.fontQuadMesh = meshManager.getMeshHandleFromMeshName(EngineSetting.FONT_DEFAULT_MESH);
    }

    // Submit \\

    public void submit(
            FontInstance font,
            float screenX, float screenY, float scale,
            MaskStruct mask,
            WindowInstance window) {

        font.prepareComposite(screenX, screenY, scale);

        if (!font.hasGlyphs())
            return;

        int windowID = window.getWindowID();
        Object2ObjectOpenHashMap<FontInstance, FontCompositeState> fontState = getOrCreateFontState(windowID);

        FontCompositeState state = fontState.get(font);
        if (state == null) {
            state = new FontCompositeState();
            state.buffer = create(CompositeBufferInstance.class);
            compositeBufferManager.constructor(state.buffer, fontQuadMesh, INSTANCE_ATTR_SIZES);
            fontState.put(font, state);
        }

        state.buffer.clear();

        float[] instanceData = font.getInstanceData();

        for (int i = 0; i < font.getGlyphCount(); i++) {
            int base = i * FLOATS_PER_INSTANCE;
            System.arraycopy(instanceData, base, state.scratch, 0, FLOATS_PER_INSTANCE);
            state.buffer.addInstance(state.scratch);
        }

        renderManager.pushCompositeCall(font.getMaterial(), state.buffer, window);
    }

    // Draw \\

    public void draw(WindowInstance window) {
        // No-op by design: flush is handled by RenderManager/CompositeRenderSystem.
    }

    // Release \\

    public void release(FontInstance font) {
        for (Object2ObjectOpenHashMap<FontInstance, FontCompositeState> fontState : windowID2FontState.values()) {
            FontCompositeState state = fontState.remove(font);
            if (state != null)
                compositeBufferManager.dispose(state.buffer);
        }
    }

    public void removeWindow(int windowID) {
        Object2ObjectOpenHashMap<FontInstance, FontCompositeState> fontState = windowID2FontState.remove(windowID);
        if (fontState == null)
            return;

        for (FontCompositeState state : fontState.values())
            compositeBufferManager.dispose(state.buffer);
    }

    // Helpers \\

    private Object2ObjectOpenHashMap<FontInstance, FontCompositeState> getOrCreateFontState(int windowID) {
        Object2ObjectOpenHashMap<FontInstance, FontCompositeState> state = windowID2FontState.get(windowID);
        if (state == null) {
            state = new Object2ObjectOpenHashMap<>();
            windowID2FontState.put(windowID, state);
        }
        return state;
    }

    private static final class FontCompositeState {
        CompositeBufferInstance buffer;
        float[] scratch = new float[FLOATS_PER_INSTANCE];
    }
}
