package application.bootstrap.geometrypipeline.skinnedbuffer;

public enum SkinnedMaskMode {

    /*
     * What a skinned draw does with the bones in its mask, written as its
     * ordinal into the palette row's header (CharacterAppearance.glsl): None
     * draws everything, Hide cuts away every fragment those bones move at
     * all, as a character seen from its own eyes loses its head, and Show
     * keeps only the fragments those bones carry at least half of, as a part
     * editor shows the part alone.
     */

    NONE,
    HIDE,
    SHOW;

    // Values
    public static final SkinnedMaskMode[] VALUES = values();
}
