package application.bootstrap.geometrypipeline.skinnedbuffer;

public enum SkinnedDetail {

    /*
     * The amounts a skinned character's procedural shading reads, four to a
     * palette texel in this order (CharacterAppearance.glsl): iris and pupil
     * size, hair shine, freckles and skin blemishes, and how far each
     * overlay shows (SkinnedOverlay): the lips, blush and eyeliner, the
     * brows' thickness, the nipples, the body hair of the chest, stomach,
     * groin, underarms, arms and legs, and the eyeshadow.
     */

    IRIS_SIZE,
    PUPIL_SIZE,
    HAIR_SHINE,
    LIPS,
    BLUSH,
    FRECKLES,
    LINER,
    BLEMISHES,
    BROWS,
    NIPPLES,
    CHEST_HAIR,
    STOMACH_HAIR,
    PUBIC_HAIR,
    UNDERARM_HAIR,
    ARM_HAIR,
    LEG_HAIR,
    EYESHADOW;

    // Values
    public static final SkinnedDetail[] VALUES = values();
}
