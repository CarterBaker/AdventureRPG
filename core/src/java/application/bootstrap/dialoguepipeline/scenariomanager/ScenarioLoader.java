package application.bootstrap.dialoguepipeline.scenariomanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.scenario.ScenarioHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ScenarioLoader extends LoaderPackage {

    /*
     * Scans the scenario ARPG directory and loads every scenario into
     * ScenarioManager. Each scenario's name is mapped to its file during scan, so
     * a name asked for before the bootstrap batch reaches it loads its file on
     * demand.
     */

    // Internal
    private File root;
    private ScenarioManager scenarioManager;
    private ScenarioBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> scenarioName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.SCENARIO_PATH);
        this.scenarioName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Scenario directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(ScenarioBuilder.class);
    }

    @Override
    protected void get() {
        this.scenarioManager = get(ScenarioManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct scenarioArray = ArpgUtility.validateArray(ArpgUtility.loadObject(file), "scenarios");

        for (int i = 0; i < scenarioArray.size(); i++) {
            ArpgObjectStruct scenarioArpg = scenarioArray.get(i).getAsObject();
            scenarioName2File.put(ArpgUtility.validateString(scenarioArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<ScenarioHandle> scenarios = internalBuilder.build(file);

        for (int i = 0; i < scenarios.size(); i++)
            scenarioManager.addScenarioHandle(scenarios.get(i));
    }

    // On-Demand \\

    void request(String scenarioName) {

        File file = scenarioName2File.get(scenarioName);

        if (file == null)
            throwException("On-demand scenario load failed — no file found for: \"" + scenarioName + "\"");

        request(file);
    }
}
