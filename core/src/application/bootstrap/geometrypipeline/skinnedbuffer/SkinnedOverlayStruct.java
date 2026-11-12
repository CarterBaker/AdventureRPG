package application.bootstrap.geometrypipeline.skinnedbuffer;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class SkinnedOverlayStruct extends StructPackage {

    /*
     * Where one overlay lies on a model and what it paints there: the tile of
     * its image in the character atlas, the centre and size of its box
     * on the bind-pose body, how it is projected, whether it is repeated on
     * the mirrored side, and for a wrapped overlay how many times its image
     * repeats per block. Written into each draw as three texels
     * (CharacterAppearance.glsl). Immutable once built.
     */

    // Tile
    private final float u0;
    private final float u1;
    private final float v0;
    private final float v1;

    // Placement
    private final Vector3 center;
    private final Vector3 size;
    private final SkinnedProjection projection;
    private final boolean mirrored;
    private final float repeat;

    // Constructor \\

    public SkinnedOverlayStruct(
            float u0,
            float u1,
            float v0,
            float v1,
            Vector3 center,
            Vector3 size,
            SkinnedProjection projection,
            boolean mirrored,
            float repeat) {

        // Tile
        this.u0 = u0;
        this.u1 = u1;
        this.v0 = v0;
        this.v1 = v1;

        // Placement
        this.center = center;
        this.size = size;
        this.projection = projection;
        this.mirrored = mirrored;
        this.repeat = repeat;
    }

    // Write \\

    public void write(float[] out, int offset) {

        out[offset] = u0;
        out[offset + 1] = u1;
        out[offset + 2] = v0;
        out[offset + 3] = v1;
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
