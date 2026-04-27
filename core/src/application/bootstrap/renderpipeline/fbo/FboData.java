package application.bootstrap.renderpipeline.fbo;

import engine.root.DataPackage;

public class FboData extends DataPackage {

    /*
     * Immutable FBO descriptor loaded from JSON during bootstrap. Holds sizing
     * strategy and format so the builder can allocate correct GL resources on
     * first request. Width and height are only meaningful when strategy is FIXED.
     */

    // Identity
    private final String name;
    private final int format;
    private final boolean depth;
    private final FboSizingStrategy sizingStrategy;

    // Dimensions
    private final int width;
    private final int height;

    // Constructor \\

    public FboData(String name, int format, boolean depth, FboSizingStrategy sizingStrategy, int width, int height) {
        this.name = name;
        this.format = format;
        this.depth = depth;
        this.sizingStrategy = sizingStrategy;
        this.width = width;
        this.height = height;
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public int getFormat() {
        return format;
    }

    public boolean hasDepth() {
        return depth;
    }

    public FboSizingStrategy getSizingStrategy() {
        return sizingStrategy;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

}