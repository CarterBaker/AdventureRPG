package application.bootstrap.progressionpipeline.skill;

import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.DataPackage;

public class SkillData extends DataPackage {

    /*
     * Immutable skill definition loaded from ARPG: proficiency with one exact
     * kind of item. It rises only by using items that name it, and each
     * practice feeds the trades it belongs to by their shares. While an item
     * of this skill is held, each whole step of the being's effective skill —
     * its own, plus a share of those trades — adds the fraction its stat
     * bonuses name to those stats. Its value lies between min and max, the
     * learn rate scales every gain, and a skill that does not decay keeps what
     * it reaches. Owned by SkillHandle for the engine lifetime.
     */

    // Identity
    private final String skillName;
    private final short skillID;

    // Presentation
    private final String displayName;
    private final String description;

    // Range
    private final float min;
    private final float max;

    // Learning
    private final float learnRate;
    private final boolean decaying;

    // Feeds
    private final ValueMapStruct tradeShares;

    // Effects
    private final ValueMapStruct statBonuses;

    // Constructor \\

    public SkillData(
            String skillName,
            short skillID,
            String displayName,
            String description,
            float min,
            float max,
            float learnRate,
            boolean decaying,
            ValueMapStruct tradeShares,
            ValueMapStruct statBonuses) {

        // Identity
        this.skillName = skillName;
        this.skillID = skillID;

        // Presentation
        this.displayName = displayName;
        this.description = description;

        // Range
        this.min = min;
        this.max = max;

        // Learning
        this.learnRate = learnRate;
        this.decaying = decaying;

        // Feeds
        this.tradeShares = tradeShares;

        // Effects
        this.statBonuses = statBonuses;
    }

    // Accessible \\

    public String getSkillName() {
        return skillName;
    }

    public short getSkillID() {
        return skillID;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getLearnRate() {
        return learnRate;
    }

    public boolean isDecaying() {
        return decaying;
    }

    public ValueMapStruct getTradeShares() {
        return tradeShares;
    }

    public ValueMapStruct getStatBonuses() {
        return statBonuses;
    }
}
