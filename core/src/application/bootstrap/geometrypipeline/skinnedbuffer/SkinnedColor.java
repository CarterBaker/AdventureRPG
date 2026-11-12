package application.bootstrap.geometrypipeline.skinnedbuffer;

public enum SkinnedColor {

    /*
     * The colors a skinned character is shaded with, one palette texel each in
     * this order (CharacterAppearance.glsl): skin, iris, hair, brows and
     * lashes, lips, blush, freckles, clothing, nipples and body hair.
     */

    SKIN,
    EYES,
    HAIR,
    BROWS,
    LIPS,
    BLUSH,
    FRECKLES,
    CLOTH,
    NIPPLES,
    BODY_HAIR;

    // Values
    public static final SkinnedColor[] VALUES = values();
}
