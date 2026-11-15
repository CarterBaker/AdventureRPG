package application.bootstrap.dialoguepipeline.scenario;

import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.HandlePackage;

public class ScenarioHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded scenario definition. Registered and owned
     * by ScenarioManager. Delegates all accessors through ScenarioData.
     */

    // Internal
    private ScenarioData scenarioData;

    // Constructor \\

    public void constructor(ScenarioData scenarioData) {

        // Internal
        this.scenarioData = scenarioData;
    }

    // Accessible \\

    public ScenarioData getScenarioData() {
        return scenarioData;
    }

    public String getScenarioName() {
        return scenarioData.getScenarioName();
    }

    public short getScenarioID() {
        return scenarioData.getScenarioID();
    }

    public int getSeed() {
        return scenarioData.getSeed();
    }

    public String getDisplayName() {
        return scenarioData.getDisplayName();
    }

    public String getDescription() {
        return scenarioData.getDescription();
    }

    public String getListenerName() {
        return scenarioData.getListenerName();
    }

    public short getProfessionID() {
        return scenarioData.getProfessionID();
    }

    public short[] getTraitIDs() {
        return scenarioData.getTraitIDs();
    }

    public ValueMapStruct getMoodLevels() {
        return scenarioData.getMoodLevels();
    }

    public float getAffinity() {
        return scenarioData.getAffinity();
    }

    public float getMorality() {
        return scenarioData.getMorality();
    }

    public short getRelationID() {
        return scenarioData.getRelationID();
    }

    public String getSpeakerName() {
        return scenarioData.getSpeakerName();
    }
}
