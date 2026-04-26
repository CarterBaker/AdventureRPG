package application.bootstrap.renderpipeline.fbo;

import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class FboInstance extends InstancePackage {

    private FboData data;
    private IntArrayList framebuffers;
    private IntArrayList textures;
    private IntArrayList depthRenderbuffers;
    private int width;
    private int height;

    private FboManager fboManager;

    public void constructor(
            FboData data,
            IntArrayList framebuffers,
            IntArrayList textures,
            IntArrayList depthRenderbuffers,
            int width,
            int height) {
        this.data = data;
        this.framebuffers = framebuffers;
        this.textures = textures;
        this.depthRenderbuffers = depthRenderbuffers;
        this.width = width;
        this.height = height;
    }

    @Override
    protected void get() {
        this.fboManager = get(FboManager.class);
    }

    public int getTextureId() {
        return textures.isEmpty() ? 0 : textures.getInt(textures.size() - 1);
    }

    public void bind() {
        fboManager.bind(this);
    }

    public void unbind() {
        fboManager.unbind();
    }

    public void resize(int width, int height) {
        fboManager.resize(this, width, height);
    }

    void setSize(int width, int height) {
        this.width = width;
        this.height = height;
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

    public IntArrayList getDepthRenderbuffers() {
        return depthRenderbuffers;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
