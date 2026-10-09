package application.bootstrap.worldpipeline.roadmanager;

import java.io.File;

import application.bootstrap.worldpipeline.road.RoadHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class RoadLoader extends LoaderPackage {

    /*
     * Scans the road ARPG directory and loads every road type into
     * RoadManager, one type per file, named by its path below the directory.
     * Supports on-demand loading by road name, and requestAll() so
     * RoadManager completes its palette before the first chunk generates.
     */

    // Internal
    private File root;
    private RoadManager roadManager;
    private RoadBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> roadName2File;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(RoadBuilder.class);
    }

    @Override
    protected void get() {
        this.roadManager = get(RoadManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.ROAD_PATH);
        this.roadName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Road root directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            String roadName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            roadName2File.put(roadName, file);
            queueFile(file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        String roadName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        RoadHandle roadHandle = internalBuilder.build(file, roadName);

        roadManager.addRoadHandle(roadHandle);
    }

    // On-Demand \\

    void request(String roadName) {

        if (roadManager.hasRoad(roadName))
            return;

        File file = roadName2File.get(roadName);

        if (file == null)
            throwException("On-demand road load failed — no file found for: \"" + roadName + "\"");

        request(file);
    }
}
