package application.bootstrap.dialoguepipeline.trait;

import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.HandlePackage;

public class TraitHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded trait definition. Registered and owned by
     * TraitManager. Delegates all accessors through TraitData.
     */

    // Internal
    private TraitData traitData;

    // Constructor \\

    public void constructor(TraitData traitData) {

        // Internal
        this.traitData = traitData;
    }

    // Accessible \\

    public TraitData getTraitData() {
        return traitData;
    }

    public String getTraitName() {
        return traitData.getTraitName();
    }

    public short getTraitID() {
        return traitData.getTraitID();
    }

    public int getSeed() {
        return traitData.getSeed();
    }

    public String getDisplayName() {
        return traitData.getDisplayName();
    }

    public String getDescription() {
        return traitData.getDescription();
    }

    public short getOppositeID() {
        return traitData.getOppositeID();
    }

    public boolean isWordless() {
        return traitData.isWordless();
    }

    public ValueMapStruct getMoodLeans() {
        return traitData.getMoodLeans();
    }

    public float getAffinityGain() {
        return traitData.getAffinityGain();
    }

    public float getAffinityLoss() {
        return traitData.getAffinityLoss();
    }
}
