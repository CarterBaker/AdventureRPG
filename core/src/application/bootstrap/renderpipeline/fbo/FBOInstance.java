package application.bootstrap.renderpipeline.fbo;

import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class FBOInstance extends InstancePackage {

    /*
     * Runtime FBO wrapper holding its GL resources directly; bind, unbind and
     * resize route back through FBOManager. Carries the snapshot
     * FBORenderSystem.pushFbo() queued it with — window, layer, sort key,
     * screen order and destination rect, null meaning fullscreen — plus
     * optional blit overrides.
     */

    // Data
    private FBOData data;

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
    private FBOManager fboManager;

    // Constructor \\

    public void constructor(
            FBOData data,
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
        fboManager = get(FBOManager.class);
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

    public FBOData getFboData() {
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