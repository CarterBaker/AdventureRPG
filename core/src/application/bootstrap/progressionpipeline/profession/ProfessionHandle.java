package application.bootstrap.progressionpipeline.profession;

import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.HandlePackage;

public class ProfessionHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded profession definition. Registered and
     * owned by ProfessionManager. Delegates all accessors through
     * ProfessionData.
     */

    // Internal
    private ProfessionData professionData;

    // Constructor \\

    public void constructor(ProfessionData professionData) {

        // Internal
        this.professionData = professionData;
    }

    // Accessible \\

    public ProfessionData getProfessionData() {
        return professionData;
    }

    public String getProfessionName() {
        return professionData.getProfessionName();
    }

    public short getProfessionID() {
        return professionData.getProfessionID();
    }

    public String getDisplayName() {
        return professionData.getDisplayName();
    }

    public String getDescription() {
        return professionData.getDescription();
    }

    public String getTitle(short genderID) {
        return professionData.getTitle(genderID);
    }

    public boolean isStarting() {
        return professionData.isStarting();
    }

    public int getRequirementGroupCount() {
        return professionData.getRequirementGroupCount();
    }

    public RequirementGroupStruct getRequirementGroup(int index) {
        return professionData.getRequirementGroup(index);
    }

    public ValueMapStruct getStatBonuses() {
        return professionData.getStatBonuses();
    }

    public ValueMapStruct getTradeLearning() {
        return professionData.getTradeLearning();
    }

    public ValueMapStruct getStartingTrades() {
        return professionData.getStartingTrades();
    }

    public ValueMapStruct getAttributeWeights() {
        return professionData.getAttributeWeights();
    }
}
