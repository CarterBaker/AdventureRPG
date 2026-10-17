package editor.bootstrap.imagepipeline.imagebrush;

import engine.root.StructPackage;

public class ImageBrushStruct extends StructPackage {

    /*
     * What a stroke paints with: an RGBA8888 color and a round tip, every
     * pixel within radius of the stroke's path, so radius zero paints single
     * pixels. A smoothing brush paints no color of its own but blends every
     * pixel it covers with its neighbours. Owned and kept current by the tool
     * that paints with it.
     */

    // Brush
    private int color;
    private int radius;
    private boolean smoothing;

    // Management \\

    public void set(int color, int radius) {
        this.color = color;
        this.radius = radius;
        this.smoothing = false;
    }

    public void setSmoothing(int radius) {
        this.radius = radius;
        this.smoothing = true;
    }

    // Accessible \\

    public int getColor() {
        return color;
    }

    public int getRadius() {
        return radius;
    }

    public boolean isSmoothing() {
        return smoothing;
    }
}
