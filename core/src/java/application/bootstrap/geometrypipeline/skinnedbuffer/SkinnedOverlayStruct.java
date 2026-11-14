package application.bootstrap.geometrypipeline.skinnedbuffer;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class SkinnedOverlayStruct extends StructPackage {

    /*
     * Where one overlay lies on a model and the styles it may paint there:
     * the region of the model's UV layout its images cover and its styles, a
     * set of material variants (SkinnedVariantStruct). Written into each draw
     * as three texels — the tile of the style worn, the region and its relief
     * — (CharacterAppearance.glsl). Immutable once built.
     */

    // Styles
    private final SkinnedVariantStruct styles;

    // Placement — the region of the UV layout (u0, v0, u1, v1)
    private final float[] region;

    // Constructor \\

    public SkinnedOverlayStruct(SkinnedVariantStruct styles, float[] region) {

        // Styles
        this.styles = styles;

        // Placement
        this.region = region;
    }

    // Write \\

    // The overlay as one style paints it, the default for a style outside the overlay's
    public void write(float[] out, int offset, int styleIndex) {

        int texel = EngineSetting.COLOR_CHANNEL_COUNT;

        styles.writeTile(out, offset, styleIndex);
        System.arraycopy(region, 0, out, offset + texel, texel);
        out[offset + 2 * texel] = styles.getRelief(styleIndex);
        out[offset + 2 * texel + 1] = 0f;
        out[offset + 2 * texel + 2] = 0f;
        out[offset + 2 * texel + 3] = 0f;
    }

    // Accessible \\

    public int getStyleCount() {
        return styles.getCount();
    }

    public String getStyleName(int styleIndex) {
        return styles.getName(styleIndex);
    }

    // A style's place by its name, INDEX_NOT_FOUND for a name the overlay does not offer
    public int findStyle(String styleName) {
        return styles.find(styleName);
    }

    public int getDefaultStyle() {
        return styles.getDefault();
    }
}
