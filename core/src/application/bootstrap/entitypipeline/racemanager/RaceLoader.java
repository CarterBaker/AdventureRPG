package application.bootstrap.entitypipeline.racemanager;

import java.io.File;

import application.bootstrap.entitypipeline.race.RaceHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class RaceLoader extends LoaderPackage {

    /*
     * Scans the race ARPG directory and loads every race into RaceManager,
     * which assigns each its ID as it registers. Supports on-demand loading
     * for races not yet in the palette — entity templates resolve their race
     * through that path while they load — and unregistered preview builds of
     * a race still being edited.
     */

    // Internal
    private File root;
    private RaceManager raceManager;
    private RaceBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> raceName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.RACE_PATH);
        this.raceName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Race directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            String raceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            raceName2File.put(raceName, file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        create(AppearanceBuilder.class);
        this.internalBuilder = create(RaceBuilder.class);
    }

    @Override
    protected void get() {
        this.raceManager = get(RaceManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String raceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        RaceHandle raceHandle = internalBuilder.build(file, raceName);

        if (raceHandle == null)
            throwException("Failed to build race from: " + file.getAbsolutePath());

        raceManager.addRace(raceName, raceHandle);
    }

    // On-Demand \\

    void request(String raceName) {

        File file = raceName2File.get(raceName);

        if (file == null)
            throwException("On-demand race load failed — not found in scan registry: \"" + raceName + "\"");

        request(file);
    }

    // Preview \\

    RaceHandle buildPreview(String raceName, ArpgObjectStruct raceArpg) {
        return internalBuilder.build(raceArpg, raceName);
    }
}
