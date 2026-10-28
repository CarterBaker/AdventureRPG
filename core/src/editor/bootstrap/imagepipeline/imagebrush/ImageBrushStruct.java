package editor.bootstrap.imagepipeline.imagebrush;

import editor.bootstrap.brushpipeline.brush.BrushHandle;
import engine.root.StructPackage;

public class ImageBrushStruct extends StructPackage {

    /*
     * What a stroke paints with: the brush preset whose tip, falloff,
     * density, spacing and mode it stamps, the RGBA8888 color a painting
     * brush lays down, its radius in pixels, so radius zero stamps single
     * pixels, and whether the image's colors can blend. On an image whose
     * colors name things, as a biome map's do, every mode only ever writes a
     * color already there or the brush's own, dithering where it would fade.
     * Owned and kept current by the tool that paints with it.
     */

    // Brush
    private BrushHandle brush;
    private int color;
    private int radius;
    private boolean blending;

    // Management \\

    public void set(BrushHandle brush, int color, int radius, boolean blending) {
        this.brush = brush;
        this.color = color;
        this.radius = radius;
        this.blending = blending;
    }

    // Accessible \\

    public BrushHandle getBrush() {
        return brush;
    }

    public int getColor() {
        return color;
    }

    public int getRadius() {
        return radius;
    }

    public boolean isBlending() {
        return blending;
    }
}
