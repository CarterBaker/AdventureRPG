package editor.bootstrap.imagepipeline.imagebrush;

import editor.bootstrap.brushpipeline.brush.BrushHandle;
import editor.bootstrap.imagepipeline.util.ImageStroke;
import engine.root.StructPackage;

public class ImageBrushStruct extends StructPackage {

    /*
     * What a stroke paints with: the brush whose tip, falloff, density and
     * spacing it stamps, what the stroke does, the RGBA8888 color a painting
     * stroke lays down, its radius in pixels, so radius zero stamps single
     * pixels, the strength each stamp takes a pixel by at the tip's centre,
     * how far in degrees a swirl turns there, and whether the image's colors
     * can blend. On an image whose colors name things, as a biome map's do,
     * every stroke only ever writes a color already there or the brush's own,
     * dithering where it would fade. Owned and kept current by the tool that
     * paints with it.
     */

    // Brush
    private BrushHandle brush;
    private ImageStroke stroke;
    private int color;
    private int radius;
    private float strength;
    private float twist;
    private boolean blending;

    // Management \\

    public void set(
            BrushHandle brush,
            ImageStroke stroke,
            int radius,
            float strength,
            float twist,
            boolean blending) {

        this.brush = brush;
        this.stroke = stroke;
        this.radius = radius;
        this.strength = strength;
        this.twist = twist;
        this.blending = blending;
    }

    // The color a painting stroke lays down; no other stroke reads it
    public void setColor(int color) {
        this.color = color;
    }

    // Accessible \\

    public BrushHandle getBrush() {
        return brush;
    }

    public ImageStroke getStroke() {
        return stroke;
    }

    public int getColor() {
        return color;
    }

    public int getRadius() {
        return radius;
    }

    public float getStrength() {
        return strength;
    }

    public float getTwist() {
        return twist;
    }

    public boolean isBlending() {
        return blending;
    }
}
