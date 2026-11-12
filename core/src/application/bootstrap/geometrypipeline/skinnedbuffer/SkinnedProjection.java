package application.bootstrap.geometrypipeline.skinnedbuffer;

public enum SkinnedProjection {

    /*
     * How an overlay is laid on the bind-pose body (CharacterSurface.glsl):
     * Front and Back stretch its image once across its box from the front or
     * the back, on the faces turned that way, and Wrap repeats it across the
     * box from all three axes, for hair that grows all round a limb.
     */

    FRONT,
    BACK,
    WRAP;

    // Values
    public static final SkinnedProjection[] VALUES = values();
}
