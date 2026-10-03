package editor.bootstrap.imagepipeline.imagebrush;

import engine.root.StructPackage;

public class ImageBrushStruct extends StructPackage {

    /*
     * What a stroke paints with: an RGBA8888 color and a round tip, every
     * pixel within radius of the stroke's path, so radius zero paints single
     * pixels. Owned and kept current by the tool that paints with it.
     */

    // Brush
    private int color;
    private int radius;

    // Management \\

    public void set(int color, int radius) {
        this.color = color;
        this.radius = radius;
    }

    // Accessible \\

    public int getColor() {
        return color;
    }

    public int getRadius() {
        return radius;
    }
}
