package application.bootstrap.renderpipeline.photofilter;

import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector4;

public class PhotoFilterHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded photo filter definition. Registered
     * and owned by PhotoFilterManager. Delegates all accessors through
     * PhotoFilterData.
     */

    // Internal
    private PhotoFilterData photoFilterData;

    // Constructor \\

    public void constructor(PhotoFilterData photoFilterData) {

        // Internal
        this.photoFilterData = photoFilterData;
    }

    // Accessible \\

    public PhotoFilterData getPhotoFilterData() {
        return photoFilterData;
    }

    public String getPhotoFilterName() {
        return photoFilterData.getPhotoFilterName();
    }

    public short getPhotoFilterID() {
        return photoFilterData.getPhotoFilterID();
    }

    public String getDisplayName() {
        return photoFilterData.getDisplayName();
    }

    public Vector4 writeRow(int channel, Vector4 out) {
        return photoFilterData.writeRow(channel, out);
    }

    public float getContrast() {
        return photoFilterData.getContrast();
    }

    public float getVignette() {
        return photoFilterData.getVignette();
    }

    public float getGrain() {
        return photoFilterData.getGrain();
    }
}
