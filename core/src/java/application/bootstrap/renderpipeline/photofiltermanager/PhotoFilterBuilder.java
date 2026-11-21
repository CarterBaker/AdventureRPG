package application.bootstrap.renderpipeline.photofiltermanager;

import java.io.File;

import application.bootstrap.renderpipeline.photofilter.PhotoFilterData;
import application.bootstrap.renderpipeline.photofilter.PhotoFilterHandle;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class PhotoFilterBuilder extends BuilderPackage {

    /*
     * Parses photo filter ARPG files and builds PhotoFilterHandle instances.
     * Each file lists its filters under a "photo_filters" array, each named
     * by its "name" field: its "matrix", three rows of red, green, blue and
     * an offset, one per output channel, the identity when left out, and the
     * "contrast", "vignette" and "grain" it adds. Bootstrap-only.
     */

    // Internal
    private PhotoFilterManager photoFilterManager;

    // Base \\

    @Override
    protected void get() {
        this.photoFilterManager = get(PhotoFilterManager.class);
    }

    // Build \\

    ObjectArrayList<PhotoFilterHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct photoFilterArray = ArpgUtility.validateArray(rootArpg, EngineSetting.PHOTO_FILTER_ENTRIES);
        ObjectArrayList<PhotoFilterHandle> photoFilters = new ObjectArrayList<>();

        for (int i = 0; i < photoFilterArray.size(); i++)
            photoFilters.add(parsePhotoFilter(photoFilterArray.get(i).getAsObject()));

        return photoFilters;
    }

    // Parse \\

    private PhotoFilterHandle parsePhotoFilter(ArpgObjectStruct photoFilterArpg) {

        String photoFilterName = ArpgUtility.validateString(photoFilterArpg, "name");
        short photoFilterID = photoFilterManager.registerPhotoFilterName(photoFilterName);

        PhotoFilterData photoFilterData = new PhotoFilterData(
                photoFilterName,
                photoFilterID,
                ArpgUtility.getString(photoFilterArpg, "display_name", photoFilterName),
                parseMatrix(photoFilterName, photoFilterArpg),
                ArpgUtility.getFloat(photoFilterArpg, "contrast", 1f),
                ArpgUtility.getFloat(photoFilterArpg, "vignette", 0f),
                ArpgUtility.getFloat(photoFilterArpg, "grain", 0f));

        PhotoFilterHandle photoFilterHandle = create(PhotoFilterHandle.class);
        photoFilterHandle.constructor(photoFilterData);

        return photoFilterHandle;
    }

    // Three rows of four, the identity when the filter names none
    private float[] parseMatrix(String photoFilterName, ArpgObjectStruct photoFilterArpg) {

        float[] matrix = EngineSetting.PHOTO_FILTER_IDENTITY.clone();

        if (!ArpgUtility.hasArray(photoFilterArpg, "matrix"))
            return matrix;

        ArpgArrayStruct matrixArray = ArpgUtility.validateArray(photoFilterArpg, "matrix");

        if (matrixArray.size() != matrix.length)
            throwException("Photo filter \"" + photoFilterName + "\" has a matrix of " + matrixArray.size()
                    + " values; it takes " + matrix.length + ", three rows of red, green, blue and offset");

        for (int i = 0; i < matrix.length; i++)
            matrix[i] = matrixArray.get(i).getAsFloat();

        return matrix;
    }
}
