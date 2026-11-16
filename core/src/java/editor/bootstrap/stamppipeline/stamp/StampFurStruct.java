package editor.bootstrap.stamppipeline.stamp;

import engine.root.StructPackage;

public class StampFurStruct extends StructPackage {

    /*
     * How a fur stamp lays its strand card over a body, as a groom scatters
     * cards over a scalp: over a patch of the given radius about the point
     * aimed at, one card for every square of the spacing the surface holds,
     * each card's length and turn varied by up to the jitter's share.
     */

    // Patch
    private final float radius;
    private final float spacing;
    private final float jitter;

    // Constructor \\

    public StampFurStruct(float radius, float spacing, float jitter) {

        // Patch
        this.radius = radius;
        this.spacing = spacing;
        this.jitter = jitter;
    }

    // Accessible \\

    public float getRadius() {
        return radius;
    }

    public float getSpacing() {
        return spacing;
    }

    public float getJitter() {
        return jitter;
    }
}
