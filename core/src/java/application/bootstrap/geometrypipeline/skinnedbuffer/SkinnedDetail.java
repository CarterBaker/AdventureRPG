package application.bootstrap.geometrypipeline.skinnedbuffer;

public enum SkinnedDetail {

    /*
     * The amounts a skinned character's procedural shading reads, four to a
     * palette texel in this order (CharacterAppearance.glsl): iris and pupil
     * size, hair shine, the strength of lip color, blush, freckles, eyeliner
     * and skin blemishes, and how far each overlay shows (SkinnedOverlay):
     * the brows' thickness, the nipples, and the body hair of the chest,
     * stomach, groin, underarms, arms and legs.
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
    LEG_HAIR;

    // Values
    public static final SkinnedDetail[] VALUES = values();
}
