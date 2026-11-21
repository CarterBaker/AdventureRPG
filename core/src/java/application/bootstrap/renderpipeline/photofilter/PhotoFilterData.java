package application.bootstrap.renderpipeline.photofilter;

import engine.root.DataPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector4;

public class PhotoFilterData extends DataPackage {

    /*
     * Immutable photo filter definition loaded from ARPG: the color matrix
     * the composite lays over the finished scene in photo mode and in every
     * screenshot taken there — each output channel a mix of the red, green
     * and blue in and an offset, as a color matrix filter holds one — and the
     * contrast, vignette and grain it adds on top of the player's own. Owned
     * by PhotoFilterHandle for the engine lifetime.
     */

    // Identity
    private final String photoFilterName;
    private final short photoFilterID;

    // Presentation
    private final String displayName;

    // Look
    private final float[] matrix;
    private final float contrast;
    private final float vignette;
    private final float grain;

    // Constructor \\

    public PhotoFilterData(
            String photoFilterName,
            short photoFilterID,
            String displayName,
            float[] matrix,
            float contrast,
            float vignette,
            float grain) {

        // Identity
        this.photoFilterName = photoFilterName;
        this.photoFilterID = photoFilterID;

        // Presentation
        this.displayName = displayName;

        // Look
        this.matrix = matrix;
        this.contrast = contrast;
        this.vignette = vignette;
        this.grain = grain;
    }

    // Accessible \\

    public String getPhotoFilterName() {
        return photoFilterName;
    }

    public short getPhotoFilterID() {
        return photoFilterID;
    }

    public String getDisplayName() {
        return displayName;
    }

    // One output channel's row of the matrix — red, green, blue in, then the offset — written into a vector
    public Vector4 writeRow(int channel, Vector4 out) {

        int offset = channel * EngineSetting.VECTOR4_COMPONENT_COUNT;

        return out.set(matrix[offset], matrix[offset + 1], matrix[offset + 2], matrix[offset + 3]);
    }

    public float getContrast() {
        return contrast;
    }

    public float getVignette() {
        return vignette;
    }

    public float getGrain() {
        return grain;
    }
}
