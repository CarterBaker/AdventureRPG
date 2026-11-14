package application.bootstrap.progressionpipeline.skill;

import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.HandlePackage;

public class SkillHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded skill definition. Registered and owned
     * by SkillManager. Delegates all accessors through SkillData.
     */

    // Internal
    private SkillData skillData;

    // Constructor \\

    public void constructor(SkillData skillData) {

        // Internal
        this.skillData = skillData;
    }

    // Accessible \\

    public SkillData getSkillData() {
        return skillData;
    }

    public String getSkillName() {
        return skillData.getSkillName();
    }

    public short getSkillID() {
        return skillData.getSkillID();
    }

    public String getDisplayName() {
        return skillData.getDisplayName();
    }

    public String getDescription() {
        return skillData.getDescription();
    }

    public float getMin() {
        return skillData.getMin();
    }

    public float getMax() {
        return skillData.getMax();
    }

    public float getLearnRate() {
        return skillData.getLearnRate();
    }

    public boolean isDecaying() {
        return skillData.isDecaying();
    }

    public ValueMapStruct getTradeShares() {
        return skillData.getTradeShares();
    }

    public ValueMapStruct getStatBonuses() {
        return skillData.getStatBonuses();
    }
}
