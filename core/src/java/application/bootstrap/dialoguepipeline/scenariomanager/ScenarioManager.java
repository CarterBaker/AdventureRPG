package application.bootstrap.dialoguepipeline.scenariomanager;

import application.bootstrap.dialoguepipeline.scenario.ScenarioHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ScenarioManager extends ManagerPackage {

    /*
     * Owns the scenario palette: every being a trainer can hold a practice talk
     * with. IDs are assigned in registration order, and a name declared twice is
     * rejected. Every scenario is resolved in awake().
     */

    // Palette
    private Object2IntOpenHashMap<String> scenarioName2ScenarioID;
    private ObjectArrayList<ScenarioHandle> scenarioID2ScenarioHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.scenarioName2ScenarioID = RegistryUtility.createNameIndex();
        this.scenarioID2ScenarioHandle = RegistryUtility.createPalette();
        create(ScenarioLoader.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();
    }

    // Management \\

    short registerScenarioName(String scenarioName) {
        return (short) RegistryUtility.registerID(
                scenarioName2ScenarioID, scenarioID2ScenarioHandle, scenarioName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addScenarioHandle(ScenarioHandle scenarioHandle) {

        short scenarioID = scenarioHandle.getScenarioID();

        if (scenarioID2ScenarioHandle.get(scenarioID) != null)
            throwException("Duplicate scenario name: '" + scenarioHandle.getScenarioName()
                    + "' is declared more than once — every scenario name must be unique");

        scenarioID2ScenarioHandle.set(scenarioID, scenarioHandle);
    }

    // Accessible \\

    public boolean hasScenario(String scenarioName) {
        return RegistryUtility.getHandle(scenarioName2ScenarioID, scenarioID2ScenarioHandle, scenarioName) != null;
    }

    public short getScenarioIDFromScenarioName(String scenarioName) {

        if (!hasScenario(scenarioName) && internalLoader != null)
            ((ScenarioLoader) internalLoader).request(scenarioName);

        if (!hasScenario(scenarioName))
            throwException("Scenario not found after load: \"" + scenarioName + "\"");

        return (short) scenarioName2ScenarioID.getInt(scenarioName);
    }

    public ScenarioHandle getScenarioHandleFromScenarioID(short scenarioID) {

        ScenarioHandle handle = RegistryUtility.getHandle(scenarioID2ScenarioHandle, scenarioID);

        if (handle == null)
            throwException("Scenario ID not found: " + scenarioID);

        return handle;
    }

    public ScenarioHandle getScenarioHandleFromScenarioName(String scenarioName) {
        return getScenarioHandleFromScenarioID(getScenarioIDFromScenarioName(scenarioName));
    }

    // Every scenario ID lies below this — the length of an array indexed by scenario ID
    public int getScenarioIDCount() {
        return scenarioID2ScenarioHandle.size();
    }
}
