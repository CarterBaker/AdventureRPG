package application.bootstrap.renderpipeline.fbo;

import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class FboHandle extends HandlePackage {

    /*
     * Owns the raw GL object lists for a single FBO allocation. Held by
     * FboInstance for the full session. setSize() is called by FboManager
     * after a resize so the handle always reflects current GPU dimensions.
     */

    // Data
    private FboData data;

    // GL Resources
    private IntArrayList framebuffers;
    private IntArrayList textures;
    private IntArrayList depthRenderbuffers;

    // Dimensions
    private int width;
    private int height;

    // Constructor \\

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

    // Resize \\

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    // Accessible \\

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

}