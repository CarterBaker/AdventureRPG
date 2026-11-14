package application.bootstrap.progressionpipeline.skillmanager;

import java.io.File;

import application.bootstrap.progressionpipeline.skill.SkillData;
import application.bootstrap.progressionpipeline.skill.SkillHandle;
import application.bootstrap.progressionpipeline.statmanager.StatManager;
import application.bootstrap.progressionpipeline.trademanager.TradeManager;
import application.bootstrap.progressionpipeline.util.ProgressionArpgUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SkillBuilder extends BuilderPackage {

    /*
     * Parses skill ARPG files and builds SkillHandle instances. Each file
     * lists its skills under a "skills" array, each named by its "name" field:
     * the "min" and "max" its value lies between, its "learn_rate", whether it
     * "decays", the share of each practice fed to the trades under "trades",
     * and the fraction each whole step adds while held to the stats under
     * "stats". Bootstrap-only.
     */

    // Internal
    private SkillManager skillManager;
    private TradeManager tradeManager;
    private StatManager statManager;

    // Base \\

    @Override
    protected void get() {
        this.skillManager = get(SkillManager.class);
        this.tradeManager = get(TradeManager.class);
        this.statManager = get(StatManager.class);
    }

    // Build \\

    ObjectArrayList<SkillHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct skillArray = ArpgUtility.validateArray(rootArpg, "skills");
        ObjectArrayList<SkillHandle> skills = new ObjectArrayList<>();

        for (int i = 0; i < skillArray.size(); i++)
            skills.add(parseSkill(skillArray.get(i).getAsObject(), arpgFile));

        return skills;
    }

    // Parse \\

    private SkillHandle parseSkill(ArpgObjectStruct skillArpg, File arpgFile) {

        String skillName = ArpgUtility.validateString(skillArpg, "name");
        String owner = "Skill '" + skillName + "' in " + arpgFile.getPath();
        short skillID = skillManager.registerSkillName(skillName);
        float min = ArpgUtility.getFloat(skillArpg, "min", EngineSetting.PROFICIENCY_DEFAULT_MIN);
        float max = ArpgUtility.getFloat(skillArpg, "max", EngineSetting.PROFICIENCY_DEFAULT_MAX);
        float learnRate = ArpgUtility.getFloat(skillArpg, "learn_rate", EngineSetting.PROFICIENCY_DEFAULT_LEARN_RATE);

        if (min > 0f || max < 0f || learnRate < 0f)
            throwException(owner + " needs a \"min\" of at most 0, a \"max\" of at least 0, "
                    + "and a \"learn_rate\" of at least 0.");

        SkillData skillData = new SkillData(
                skillName,
                skillID,
                ArpgUtility.getString(skillArpg, "display_name", skillName),
                ArpgUtility.getString(skillArpg, "description", EngineSetting.PROGRESSION_TEXT_NONE),
                min,
                max,
                learnRate,
                ArpgUtility.getBoolean(skillArpg, "decays", true),
                ProgressionArpgUtility.parseValueMap(
                        skillArpg, "trades", tradeManager::getTradeIDFromTradeName, owner),
                ProgressionArpgUtility.parseValueMap(
                        skillArpg, "stats", statManager::getStatIDFromStatName, owner));

        SkillHandle skillHandle = create(SkillHandle.class);
        skillHandle.constructor(skillData);

        return skillHandle;
    }
}
