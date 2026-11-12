package application.bootstrap.geometrypipeline.skinnedbuffer;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class SkinnedOverlayStruct extends StructPackage {

    /*
     * Where one overlay lies on a model and the styles it may paint there:
     * each style's name and the tile of its image in the character atlas,
     * the style worn by default, the centre and size of its box on the
     * bind-pose body, how it is projected, whether it is repeated on the
     * mirrored side, and for a wrapped overlay how many times its image
     * repeats per block. Written into each draw as three texels, the tile
     * the style worn (CharacterAppearance.glsl). Immutable once built.
     */

    // Styles — names, and four tile floats each (u0, u1, v0, v1)
    private final String[] styleNames;
    private final float[] styleTiles;
    private final int defaultStyle;

    // Placement
    private final Vector3 center;
    private final Vector3 size;
    private final SkinnedProjection projection;
    private final boolean mirrored;
    private final float repeat;

    // Constructor \\

    public SkinnedOverlayStruct(
            String[] styleNames,
            float[] styleTiles,
            int defaultStyle,
            Vector3 center,
            Vector3 size,
            SkinnedProjection projection,
            boolean mirrored,
            float repeat) {

        // Styles
        this.styleNames = styleNames;
        this.styleTiles = styleTiles;
        this.defaultStyle = defaultStyle;

        // Placement
        this.center = center;
        this.size = size;
        this.projection = projection;
        this.mirrored = mirrored;
        this.repeat = repeat;
    }

    // Write \\

    // The overlay as one style paints it, the default for a style outside the overlay's
    public void write(float[] out, int offset, int styleIndex) {

        int tile = (styleIndex >= 0 && styleIndex < styleNames.length ? styleIndex : defaultStyle)
                * EngineSetting.COLOR_CHANNEL_COUNT;

        out[offset] = styleTiles[tile];
        out[offset + 1] = styleTiles[tile + 1];
        out[offset + 2] = styleTiles[tile + 2];
        out[offset + 3] = styleTiles[tile + 3];
        out[offset + 4] = center.x;
        out[offset + 5] = center.y;
        out[offset + 6] = center.z;
        out[offset + 7] = projection.ordinal()
                + (mirrored ? EngineSetting.SKINNED_OVERLAY_MIRROR_FLAG : 0f);
        out[offset + 8] = size.x;
        out[offset + 9] = size.y;
        out[offset + 10] = size.z;
        out[offset + 11] = repeat;
    }

    // Accessible \\

    public int getStyleCount() {
        return styleNames.length;
    }

    public String getStyleName(int styleIndex) {
        return styleNames[styleIndex];
    }

    // A style's place by its name, INDEX_NOT_FOUND for a name the overlay does not offer
    public int findStyle(String styleName) {

        for (int i = 0; i < styleNames.length; i++)
            if (styleNames[i].equals(styleName))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    public int getDefaultStyle() {
        return defaultStyle;
    }

    public Vector3 getCenter() {
        return center;
    }

    public Vector3 getSize() {
        return size;
    }

    public SkinnedProjection getProjection() {
        return projection;
    }

    public boolean isMirrored() {
        return mirrored;
    }

    public float getRepeat() {
        return repeat;
    }
}
