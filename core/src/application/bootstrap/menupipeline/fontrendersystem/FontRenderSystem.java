package application.bootstrap.menupipeline.fontrendersystem;

import application.bootstrap.menupipeline.font.FontInstance;
import application.bootstrap.renderpipeline.util.MaskStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FontRenderSystem extends SystemPackage {

    /*
     * Owns all GPU resources for composite font rendering. Shared quad
     * VBO/IBO is created once during create(). Per-window per-FontInstance
     * VAO and instance VBO state is created on first submit and grown as
     * needed. GPU objects are released when a FontInstance is ejected or a
     * window is removed.
     *
     * Call order each frame per window:
     * MenuRenderSystem calls submit() for each visible label during traversal.
     * MenuRenderSystem calls draw(window) once after all menus for that window.
     * The submission list is cleared after draw.
     */

    // Quad geometry (shared across all instances and windows)
    private int quadVBO;
    private int quadIBO;

    static final int QUAD_INDEX_COUNT = 6;
    static final int[] QUAD_ATTR_SIZES = { 2 }; // local xy
    static final int[] INSTANCE_ATTR_SIZES = { 2, 2, 4 }; // screenPos, screenSize, atlasUV
    static final int FLOATS_PER_INSTANCE;

    static {
        int sum = 0;
        for (int s : INSTANCE_ATTR_SIZES)
            sum += s;
        FLOATS_PER_INSTANCE = sum;
    }

    // Per-window per-FontInstance GPU state
    private Int2ObjectOpenHashMap<Object2ObjectOpenHashMap<FontInstance, WindowFontGpuState>> windowID2FontState;

    // Per-frame submission list per window
    private Int2ObjectOpenHashMap<ObjectArrayList<FontSubmission>> windowID2Submissions;

    // Upload scratch — grown as needed, reused each frame
    private float[] uploadScratch = new float[256 * FLOATS_PER_INSTANCE];

    // Internal \\

    @Override
    protected void create() {

        this.windowID2FontState = new Int2ObjectOpenHashMap<>();
        this.windowID2Submissions = new Int2ObjectOpenHashMap<>();

        this.quadVBO = GLSLUtility.createQuadVBO();
        this.quadIBO = GLSLUtility.createQuadIBO();
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
        ObjectArrayList<FontSubmission> list = windowID2Submissions.get(windowID);

        if (list == null) {
            list = new ObjectArrayList<>();
            windowID2Submissions.put(windowID, list);
        }

        list.add(new FontSubmission(font, mask));
    }

    // Draw \\

    public void draw(WindowInstance window) {

        int windowID = window.getWindowID();
        ObjectArrayList<FontSubmission> list = windowID2Submissions.get(windowID);

        if (list == null || list.isEmpty())
            return;

        Object2ObjectOpenHashMap<FontInstance, WindowFontGpuState> fontState = getOrCreateFontState(windowID);

        for (int i = 0; i < list.size(); i++)
            drawSubmission(list.get(i), fontState, window);

        list.clear();

        GLSLUtility.disableScissor();
    }

    private void drawSubmission(
            FontSubmission submission,
            Object2ObjectOpenHashMap<FontInstance, WindowFontGpuState> fontState,
            WindowInstance window) {

        FontInstance font = submission.font;

        WindowFontGpuState gpuState = fontState.get(font);
        if (gpuState == null) {
            gpuState = new WindowFontGpuState();
            fontState.put(font, gpuState);
        }

        // Grow or create GPU objects if capacity is insufficient
        if (gpuState.instanceVBO == 0 || gpuState.maxInstances < font.getGlyphCount()) {
            destroyGpuState(gpuState);
            int capacity = Math.max(font.getGlyphCount() * 2, 16);
            gpuState.instanceVBO = GLSLUtility.createInstanceVBO(capacity, FLOATS_PER_INSTANCE);
            gpuState.compositeVAO = GLSLUtility.createFontVAO(
                    quadVBO, QUAD_ATTR_SIZES,
                    quadIBO,
                    gpuState.instanceVBO, INSTANCE_ATTR_SIZES);
            gpuState.maxInstances = capacity;
            gpuState.uploadedVersion = -1;
        }

        // Upload instance data if stale
        if (gpuState.uploadedVersion != font.getInstanceDataVersion()) {

            int floatCount = font.getGlyphCount() * FLOATS_PER_INSTANCE;
            if (uploadScratch.length < floatCount)
                uploadScratch = new float[floatCount * 2];

            System.arraycopy(font.getInstanceData(), 0, uploadScratch, 0, floatCount);
            GLSLUtility.uploadInstanceData(gpuState.instanceVBO, uploadScratch, floatCount);
            gpuState.uploadedVersion = font.getInstanceDataVersion();
        }

        // Scissor mask
        if (submission.mask != null)
            GLSLUtility.enableScissor(
                    submission.mask.getX(), submission.mask.getY(),
                    submission.mask.getW(), submission.mask.getH());
        else
            GLSLUtility.disableScissor();

        // Bind font material (atlas + color uniforms, shader)
        GLSLUtility.bindFontMaterial(font.getMaterial(), window);

        // Draw
        GLSLUtility.drawInstanced(gpuState.compositeVAO, QUAD_INDEX_COUNT, font.getGlyphCount());
    }

    // Release \\

    /*
     * Called when a FontInstance is ejected from an active menu.
     * Removes per-window GPU objects for that instance across all windows.
     */
    public void release(FontInstance font) {
        for (Object2ObjectOpenHashMap<FontInstance, WindowFontGpuState> fontState : windowID2FontState.values()) {
            WindowFontGpuState gpuState = fontState.remove(font);
            if (gpuState != null)
                destroyGpuState(gpuState);
        }
    }

    public void removeWindow(int windowID) {
        windowID2Submissions.remove(windowID);
        Object2ObjectOpenHashMap<FontInstance, WindowFontGpuState> fontState = windowID2FontState.remove(windowID);
        if (fontState == null)
            return;
        for (WindowFontGpuState gpuState : fontState.values())
            destroyGpuState(gpuState);
    }

    // Helpers \\

    private void destroyGpuState(WindowFontGpuState gpuState) {
        if (gpuState.instanceVBO != 0) {
            GLSLUtility.deleteBuffer(gpuState.instanceVBO);
            GLSLUtility.deleteVAO(gpuState.compositeVAO);
            gpuState.instanceVBO = 0;
            gpuState.compositeVAO = 0;
        }
    }

    private Object2ObjectOpenHashMap<FontInstance, WindowFontGpuState> getOrCreateFontState(int windowID) {
        Object2ObjectOpenHashMap<FontInstance, WindowFontGpuState> state = windowID2FontState.get(windowID);
        if (state == null) {
            state = new Object2ObjectOpenHashMap<>();
            windowID2FontState.put(windowID, state);
        }
        return state;
    }

    // Inner Types \\

    private static final class FontSubmission {
        final FontInstance font;
        final MaskStruct mask;

        FontSubmission(FontInstance font, MaskStruct mask) {
            this.font = font;
            this.mask = mask;
        }
    }

    private static final class WindowFontGpuState {
        int instanceVBO;
        int compositeVAO;
        int uploadedVersion = -1;
        int maxInstances;
    }
}