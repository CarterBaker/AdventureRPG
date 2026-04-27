package application.bootstrap.renderpipeline.fbo;

import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class FboHandle extends HandlePackage {

    private FboData data;
    private IntArrayList framebuffers;
    private IntArrayList textures;
    private IntArrayList depthRenderbuffers;
    private int width;
    private int height;

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

    public FboData getData() {
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

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
