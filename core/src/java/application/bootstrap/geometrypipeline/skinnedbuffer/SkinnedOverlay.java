package application.bootstrap.geometrypipeline.skinnedbuffer;

public enum SkinnedOverlay {

    /*
     * The marks painted over a skinned character's skin rather than modelled
     * into it, each tinted by its color and shown as far as its detail
     * amount, in this order (CharacterAppearance.glsl), each laid over the
     * ones before: the makeup (blush, eyeshadow, lips and eyeliner), the
     * brows, the nipples, and the body hair of the chest, stomach, groin,
     * underarms, arms and legs. Where each lies on a body is its model's,
     * written beside the colors and details of every draw.
     */

    BLUSH(SkinnedColor.BLUSH, SkinnedDetail.BLUSH),
    EYESHADOW(SkinnedColor.EYESHADOW, SkinnedDetail.EYESHADOW),
    LIPS(SkinnedColor.LIPS, SkinnedDetail.LIPS),
    EYELINER(SkinnedColor.LINER, SkinnedDetail.LINER),
    BROWS(SkinnedColor.BROWS, SkinnedDetail.BROWS),
    NIPPLES(SkinnedColor.NIPPLES, SkinnedDetail.NIPPLES),
    CHEST_HAIR(SkinnedColor.BODY_HAIR, SkinnedDetail.CHEST_HAIR),
    STOMACH_HAIR(SkinnedColor.BODY_HAIR, SkinnedDetail.STOMACH_HAIR),
    PUBIC_HAIR(SkinnedColor.BODY_HAIR, SkinnedDetail.PUBIC_HAIR),
    UNDERARM_HAIR(SkinnedColor.BODY_HAIR, SkinnedDetail.UNDERARM_HAIR),
    ARM_HAIR(SkinnedColor.BODY_HAIR, SkinnedDetail.ARM_HAIR),
    LEG_HAIR(SkinnedColor.BODY_HAIR, SkinnedDetail.LEG_HAIR);

    // Values
    public static final SkinnedOverlay[] VALUES = values();

    // Internal
    private final SkinnedColor color;
    private final SkinnedDetail detail;

    // Constructor \\

    SkinnedOverlay(SkinnedColor color, SkinnedDetail detail) {
        this.color = color;
        this.detail = detail;
    }

    // Utility \\

    // The overlay whose amount a detail is, null for a detail no overlay shows
    public static SkinnedOverlay findByDetail(SkinnedDetail detail) {

        for (SkinnedOverlay overlay : VALUES)
            if (overlay.detail == detail)
                return overlay;

        return null;
    }

    // Accessible \\

    public SkinnedColor getColor() {
        return color;
    }

    public SkinnedDetail getDetail() {
        return detail;
    }
}
