package application.bootstrap.geometrypipeline.skinnedbuffer;

public enum SkinnedColor {

    /*
     * The colors a skinned character is shaded with, one palette texel each in
     * this order (CharacterAppearance.glsl): skin, iris, hair, brows and
     * lashes, lips, blush, freckles, clothing, nipples, body hair and teeth.
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
    BODY_HAIR,
    TEETH;

    // Values
    public static final SkinnedColor[] VALUES = values();
}
