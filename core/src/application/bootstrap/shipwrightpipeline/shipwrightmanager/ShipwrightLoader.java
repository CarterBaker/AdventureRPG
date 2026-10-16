package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import java.io.File;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;

class ShipwrightLoader extends LoaderPackage {

    /*
     * Scans the ship ARPG directory and loads every ship in the assets into
     * ShipwrightManager, one ship per file, named by its path below the
     * directory, each registered as a vehicle as it loads. ShipwrightManager
     * requests every one in its awake(), so every ship can be launched before
     * the game starts.
     */

    // Internal
    private File root;
    private ShipwrightManager shipwrightManager;
    private ShipwrightBuilder internalBuilder;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.SHIP_PATH);

        FileUtility.verifyDirectory(root, "Ship directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS))
            queueFile(file);
    }

    @Override
    protected void create() {
        this.internalBuilder = create(ShipwrightBuilder.class);
    }

    @Override
    protected void get() {
        this.shipwrightManager = get(ShipwrightManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String shipName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        ShipBlueprintStruct blueprint = internalBuilder.build(file, shipName);

        shipwrightManager.addContentShip(blueprint);
    }
}
