package editor.bootstrap.imagepipeline.util;

public enum MapPaintRule {

    /*
     * How a brush paints a map, so painting one never leaves its range, as a
     * texturing tool paints a channel by its kind. Colour maps (the albedo,
     * specular colour, diffuse, emissive) take the paint colour. Level maps
     * (height, displacement, bump, occlusion, cavity, curvature, thickness,
     * roughness, glossiness, metallic, opacity, tint) are raised by a colour
     * brighter than mid grey and lowered by a darker one, by as much as it
     * lies from mid grey, their grey kept. Relief maps (the normal, in either
     * green convention) are bent as if the brush pressed a bump into them, a
     * bright colour raising it and a dark one carving it, and stay unit
     * normals; the albedo is never touched. The brush's own tip and Strength
     * shape every rule.
     */

    COLOR(false, 0f),
    LEVEL(false, 0f),
    RELIEF(true, 1f),
    RELIEF_DIRECTX(true, -1f);

    // Internal
    private final boolean relief;
    private final float green;

    // Constructor \\

    MapPaintRule(boolean relief, float green) {
        this.relief = relief;
        this.green = green;
    }

    // Accessible \\

    // The stroke a paint stroke becomes on a map of this rule, from the paint colour's brightness
    public ImageStroke resolveStroke(float brightness) {

        if (relief)
            return ImageStroke.RELIEF;

        if (this == LEVEL)
            return brightness >= 0.5f ? ImageStroke.RAISE : ImageStroke.LOWER;

        return ImageStroke.PAINT;
    }

    // How much of the brush's Strength the stroke takes: whole for colour, by the colour's distance from mid grey
    // otherwise
    public float resolveAmount(float brightness) {
        return this == COLOR ? 1f : Math.abs(brightness * 2f - 1f);
    }

    // The way a relief stroke bends the normal: raising for a colour above mid grey, carving below, the green
    // convention's sign on its up slope; none for any other rule
    public float resolveRelief(float brightness) {
        return relief ? (brightness >= 0.5f ? 1f : -1f) : 0f;
    }

    public float getGreen() {
        return green;
    }
}
