package application.bootstrap.geometrypipeline.skinnedbuffer;

public enum SkinnedDetail {

    /*
     * The amounts a skinned character's procedural shading reads, four to a
     * palette texel in this order (CharacterAppearance.glsl): iris and pupil
     * size, hair shine, and the strength of lip color, blush, freckles,
     * eyeliner and skin blemishes.
     */

    IRIS_SIZE,
    PUPIL_SIZE,
    HAIR_SHINE,
    LIPS,
    BLUSH,
    FRECKLES,
    LINER,
    BLEMISHES;

    // Values
    public static final SkinnedDetail[] VALUES = values();
}
