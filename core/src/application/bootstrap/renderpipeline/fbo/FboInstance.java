package application.bootstrap.renderpipeline.fbo;

import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class FboInstance extends InstancePackage {

    /*
     * Runtime FBO wrapper. Holds GL resources directly — no handle indirection
     * at call sites. Delegates bind, unbind, and resize back to FboManager so
     * all GL state changes stay in one place. Blit overrides are optional and
     * checked by the render pipeline before falling back to defaults.
     *
     * Push data is a full snapshot of everything the FBO was queued with —
     * window, layer, sort key, screen order, and dest rect. Null dest rect
     * means fullscreen.
     *
     * sortKey = windowDepth * LAYER_STRIDE + layer
     * Primary draw order: editor(0) < tabs(1) < content(2).
     * Layer is secondary within each window depth band.
     * 10,000 layer slots per band — sufficient for any realistic
     * layer count per window.
     *
     * screenOrder = 0 (before composite) or 1 (after composite).
     * Separate from draw order. Not derived from window depth.
     * Written at queue time by the caller. Defaults to 1.
     *
     * Written at queue time by FboRenderSystem.pushFbo, read during flush.
     */

    // Data
    private FboData data;

    // GL Resources
    private IntArrayList framebuffers;
    private IntArrayList textures;
    private IntArrayList depthTextures;

    // Dimensions
    private int width;
    private int height;

    // Blit
    private MeshData blitMeshOverride;
    private MaterialInstance blitMaterialOverride;

    // Push data — full snapshot written at queue time, read during flush
    private WindowInstance pushWindow;
    private int pushLayer;
    private int pushSortKey; // windowDepth * LAYER_STRIDE + layer
    private int pushScreenOrder; // 0 = before composite, 1 = after
    private FBODestinationStruct pushDestRect;

    // Internal
    private FboManager fboManager;

    // Constructor \\

    public void constructor(
            FboData data,
            IntArrayList framebuffers,
            IntArrayList textures,
            IntArrayList depthTextures,
            int width,
            int height) {

        this.data = data;
        this.framebuffers = framebuffers;
        this.textures = textures;
        this.depthTextures = depthTextures;
        this.width = width;
        this.height = height;
    }

    @Override
    protected void get() {
        fboManager = get(FboManager.class);
    }

    // Framebuffer \\

    public void bind() {
        fboManager.bind(this);
    }

    public void unbind() {
        fboManager.unbind();
    }

    // Resize \\

    public void resize(int width, int height) {
        fboManager.resize(this, width, height);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    // Blit \\

    public void setBlitOverride(MeshData mesh, MaterialInstance material) {
        blitMeshOverride = mesh;
        blitMaterialOverride = material;
    }

    // Push \\

    public void setPushData(
            WindowInstance window,
            int layer,
            int sortKey,
            int screenOrder,
            FBODestinationStruct destRect) {

        pushWindow = window;
        pushLayer = layer;
        pushSortKey = sortKey;
        pushScreenOrder = screenOrder;
        pushDestRect = destRect;
    }

    // Accessible \\

    public int getTextureId() {
        return textures.isEmpty() ? 0 : textures.getInt(textures.size() - 1);
    }

    public FboData getFboData() {
        return data;
    }

    public IntArrayList getFramebuffers() {
        return framebuffers;
    }

    public IntArrayList getTextures() {
        return textures;
    }

    public int getColorTexture(String attachmentName) {

        int index = data.getColorIndex(attachmentName);

        if (index == -1)
            throwException("No color attachment named '" + attachmentName + "' on FBO: " + data.getName());

        return textures.getInt(index);
    }

    public int getDepthTexture() {

        if (depthTextures.isEmpty())
            throwException("FBO has no depth attachment: " + data.getName());

        return depthTextures.getInt(0);
    }

    public IntArrayList getDepthTextures() {
        return depthTextures;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public MeshData getBlitMeshOverride() {
        return blitMeshOverride;
    }

    public MaterialInstance getBlitMaterialOverride() {
        return blitMaterialOverride;
    }

    public WindowInstance getPushWindow() {
        return pushWindow;
    }

    public int getPushLayer() {
        return pushLayer;
    }

    public int getPushSortKey() {
        return pushSortKey;
    }

    public int getPushScreenOrder() {
        return pushScreenOrder;
    }

    public FBODestinationStruct getPushDestRect() {
        return pushDestRect;
    }
}