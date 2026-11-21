package application.bootstrap.renderpipeline.photofiltermanager;

import java.io.File;

import application.bootstrap.renderpipeline.photofilter.PhotoFilterHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class PhotoFilterLoader extends LoaderPackage {

    /*
     * Scans the photo filter ARPG directory and loads every filter into
     * PhotoFilterManager. Each filter's name is mapped to its file during
     * scan, so a name asked for before the bootstrap batch reaches it loads
     * its file on demand.
     */

    // Internal
    private File root;
    private PhotoFilterManager photoFilterManager;
    private PhotoFilterBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> photoFilterName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.PHOTO_FILTER_PATH);
        this.photoFilterName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Photo filter directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(PhotoFilterBuilder.class);
    }

    @Override
    protected void get() {
        this.photoFilterManager = get(PhotoFilterManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct photoFilterArray = ArpgUtility.validateArray(
                ArpgUtility.loadObject(file), EngineSetting.PHOTO_FILTER_ENTRIES);

        for (int i = 0; i < photoFilterArray.size(); i++) {
            ArpgObjectStruct photoFilterArpg = photoFilterArray.get(i).getAsObject();
            photoFilterName2File.put(ArpgUtility.validateString(photoFilterArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<PhotoFilterHandle> photoFilters = internalBuilder.build(file);

        for (int i = 0; i < photoFilters.size(); i++)
            photoFilterManager.addPhotoFilterHandle(photoFilters.get(i));
    }

    // On-Demand \\

    void request(String photoFilterName) {

        File file = photoFilterName2File.get(photoFilterName);

        if (file == null)
            throwException("On-demand photo filter load failed — no file found for: \"" + photoFilterName + "\"");

        request(file);
    }
}
