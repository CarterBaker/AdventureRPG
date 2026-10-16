package application.runtime.menueventsmanager.util;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.progression.CareerHandle;
import application.bootstrap.entitypipeline.progression.ProgressionHandle;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import application.bootstrap.progressionpipeline.progressionmanager.ProgressionManager;
import application.bootstrap.progressionpipeline.stat.StatHandle;
import application.bootstrap.progressionpipeline.statmanager.StatManager;
import application.bootstrap.progressionpipeline.tiermanager.TierManager;
import application.bootstrap.progressionpipeline.util.PowerUtility;
import application.bootstrap.progressionpipeline.util.ProficiencyUtility;
import application.runtime.RuntimeSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

public class ProgressionDisplayBranch extends BranchPackage {

    /*
     * The one place progression is written for the screen, shared by the
     * inventory, the character page and the character creator. A stat reads by
     * its own format, a scaled stat written absolute at the level it is read
     * for, so a high-level sword shows the growth of every level it was forged
     * at. Experience reads as banked against needed, a profession by the title
     * its holder's gender gives it, morality and condition by a word, and a
     * trade or skill by its whole step. Each gear tier keeps one colour its
     * names are tinted with. getDisplayRevision() changes whenever anything
     * shown here might have, so a view redraws only then.
     */

    // Internal
    private ProgressionManager progressionManager;
    private StatManager statManager;
    private TierManager tierManager;
    private ProfessionManager professionManager;

    // Colors
    private MenuColorStruct[] tierID2Color;

    // Base \\

    @Override
    protected void get() {
        this.progressionManager = get(ProgressionManager.class);
        this.statManager = get(StatManager.class);
        this.tierManager = get(TierManager.class);
        this.professionManager = get(ProfessionManager.class);
    }

    @Override
    protected void awake() {

        // Colors
        this.tierID2Color = new MenuColorStruct[tierManager.getTierIDCount()];

        for (short tierID = EngineSetting.REGISTRY_RESERVED_ID + 1; tierID < tierID2Color.length; tierID++)
            tierID2Color[tierID] = new MenuColorStruct(tierManager.getTierHandleFromTierID(tierID).getColor());
    }

    // Revision \\

    // Rises whenever the being's gear, level, attributes, trade or skill steps or profession move — every revision it
    // sums only ever counts up
    public int getDisplayRevision(EntityInstance entity) {
        return entity.getInventoryHandle().getRevision()
                + entity.getProgressionHandle().getRevision()
                + entity.getProficiencyHandle().getRevision()
                + entity.getCareerHandle().getRevision();
    }

    // Stats \\

    public String formatStat(EntityInstance entity, short statID) {
        return formatStatValue(statID, entity.getStat(statID), entity.getLevel());
    }

    // A stat's value as its format reads, a scaled one written absolute at the level it was read for
    public String formatStatValue(short statID, float value, int level) {

        StatHandle statHandle = statManager.getStatHandleFromStatID(statID);

        return switch (statHandle.getFormat()) {
            case PERCENT -> String.format(RuntimeSetting.PROGRESSION_FORMAT_PERCENT, value * EngineSetting.PERCENT_MAX);
            case DECIMAL -> String.format(RuntimeSetting.PROGRESSION_FORMAT_DECIMAL, value);
            case WHOLE -> statHandle.isScaled()
                    ? PowerUtility.formatAbsolute(value, level, RuntimeSetting.PROGRESSION_FORMAT_WHOLE)
                    : String.format(RuntimeSetting.PROGRESSION_FORMAT_WHOLE, value);
        };
    }

    // One stat of a piece of gear, as it stands at the gear's own level
    public String formatItemStat(ItemInstance itemInstance, short statID) {
        return formatStatValue(
                statID,
                progressionManager.getItemStat(itemInstance, statID, itemInstance.getLevel()),
                itemInstance.getLevel());
    }

    // Experience \\

    public String formatExperience(EntityInstance entity) {

        ProgressionHandle progressionHandle = entity.getProgressionHandle();

        return String.format(
                RuntimeSetting.PROGRESSION_FORMAT_EXPERIENCE,
                progressionManager.getExperienceIntoLevel(entity),
                progressionManager.getLevelExperience(progressionHandle.getLevel()));
    }

    // Profession \\

    public String formatProfession(EntityInstance entity) {

        CareerHandle careerHandle = entity.getCareerHandle();

        if (!careerHandle.hasActiveProfession())
            return RuntimeSetting.PROGRESSION_TEXT_NO_PROFESSION;

        return professionManager.getProfessionHandleFromProfessionID(careerHandle.getActiveProfessionID())
                .getTitle(entity.getGenderID());
    }

    // Words \\

    public String resolveMoralityWord(EntityInstance entity) {

        float shade = progressionManager.getMoralityShade(entity);

        for (int i = 0; i < RuntimeSetting.PROGRESSION_MORALITY_THRESHOLDS.length; i++)
            if (shade < RuntimeSetting.PROGRESSION_MORALITY_THRESHOLDS[i])
                return RuntimeSetting.PROGRESSION_MORALITY_WORDS[i];

        return RuntimeSetting.PROGRESSION_MORALITY_WORDS[RuntimeSetting.PROGRESSION_MORALITY_WORDS.length - 1];
    }

    public String resolveConditionWord(float condition) {

        for (int i = 0; i < RuntimeSetting.PROGRESSION_CONDITION_THRESHOLDS.length; i++)
            if (condition >= RuntimeSetting.PROGRESSION_CONDITION_THRESHOLDS[i])
                return RuntimeSetting.PROGRESSION_CONDITION_WORDS[i];

        return RuntimeSetting.PROGRESSION_CONDITION_WORDS[RuntimeSetting.PROGRESSION_CONDITION_WORDS.length - 1];
    }

    public String formatStep(float value) {
        return String.valueOf(ProficiencyUtility.step(value));
    }

    // Tiers \\

    // The colour a tier tints names with, null for anything with no tier
    public MenuColorStruct getTierColor(short tierID) {
        return tierID == EngineSetting.TIER_NONE ? null : tierID2Color[tierID];
    }
}
