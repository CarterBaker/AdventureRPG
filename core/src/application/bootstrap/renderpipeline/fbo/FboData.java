package application.bootstrap.renderpipeline.fbo;

import engine.root.DataPackage;

public class FboData extends DataPackage {

    public enum SizingStrategy {
        WINDOW_RELATIVE,
        FIXED
    }

    private final String name;
    private final int format;
    private final boolean depth;
    private final SizingStrategy sizingStrategy;
    private final int width;
    private final int height;

    public FboData(String name, int format, boolean depth, SizingStrategy sizingStrategy, int width, int height) {
        this.name = name;
        this.format = format;
        this.depth = depth;
        this.sizingStrategy = sizingStrategy;
        this.width = width;
        this.height = height;
    }

    public String getName() {
        return name;
    }

    public int getFormat() {
        return format;
    }

    public boolean hasDepth() {
        return depth;
    }

    public SizingStrategy getSizingStrategy() {
        return sizingStrategy;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
