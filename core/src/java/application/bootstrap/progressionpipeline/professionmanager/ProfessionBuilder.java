package application.bootstrap.progressionpipeline.professionmanager;

import java.io.File;

import application.bootstrap.progressionpipeline.attributemanager.AttributeManager;
import application.bootstrap.progressionpipeline.deedmanager.DeedManager;
import application.bootstrap.progressionpipeline.gendermanager.GenderManager;
import application.bootstrap.progressionpipeline.profession.ProfessionData;
import application.bootstrap.progressionpipeline.profession.ProfessionHandle;
import application.bootstrap.progressionpipeline.profession.RequirementGroupStruct;
import application.bootstrap.progressionpipeline.profession.RequirementKind;
import application.bootstrap.progressionpipeline.profession.RequirementStruct;
import application.bootstrap.progressionpipeline.skillmanager.SkillManager;
import application.bootstrap.progressionpipeline.statmanager.StatManager;
import application.bootstrap.progressionpipeline.trademanager.TradeManager;
import application.bootstrap.progressionpipeline.util.ProgressionArpgUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ProfessionBuilder extends BuilderPackage {

    /*
     * Parses profession ARPG files and builds ProfessionHandle instances. Each
     * file lists its professions under a "professions" array, each named by
     * its "name" field: its "titles" by gender, whether it is "starting", its
     * "requirements" as groups whose "all" list must hold at once, and its
     * effects — the "stats" it adds, the "learning" multipliers of trades, the
     * "trades" a newcomer starts with, and the "attributes" an NPC spends its
     * points by. A requirement names its "kind", the "name" it reads when it
     * reads one, and its "min" and "max"; a deed requirement gives its "count"
     * as its min. A profession named by another is given its ID when first
     * named, so professions may require one another in any order;
     * ProfessionManager checks every such name was defined once all are
     * loaded. Bootstrap-only.
     */

    // Internal
    private ProfessionManager professionManager;
    private GenderManager genderManager;
    private StatManager statManager;
    private TradeManager tradeManager;
    private SkillManager skillManager;
    private DeedManager deedManager;
    private AttributeManager attributeManager;

    // Base \\

    @Override
    protected void get() {
        this.professionManager = get(ProfessionManager.class);
        this.genderManager = get(GenderManager.class);
        this.statManager = get(StatManager.class);
        this.tradeManager = get(TradeManager.class);
        this.skillManager = get(SkillManager.class);
        this.deedManager = get(DeedManager.class);
        this.attributeManager = get(AttributeManager.class);
    }

    // Build \\

    ObjectArrayList<ProfessionHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct professionArray = ArpgUtility.validateArray(rootArpg, "professions");
        ObjectArrayList<ProfessionHandle> professions = new ObjectArrayList<>();

        for (int i = 0; i < professionArray.size(); i++)
            professions.add(parseProfession(professionArray.get(i).getAsObject(), arpgFile));

        return professions;
    }

    // Parse \\

    private ProfessionHandle parseProfession(ArpgObjectStruct professionArpg, File arpgFile) {

        String professionName = ArpgUtility.validateString(professionArpg, "name");
        String owner = "Profession '" + professionName + "' in " + arpgFile.getPath();
        short professionID = professionManager.registerProfessionName(professionName);
        boolean starting = ArpgUtility.getBoolean(professionArpg, "starting", false);
        RequirementGroupStruct[] requirementGroups = parseRequirementGroups(professionArpg, owner);

        if (!starting && requirementGroups.length == 0)
            throwException(owner + " can never be held — it needs \"requirements\" unless it is \"starting\".");

        ArpgObjectStruct titlesArpg = ArpgUtility.hasObject(professionArpg, "titles")
                ? professionArpg.getAsObject("titles")
                : new ArpgObjectStruct();
        short[] titleGenderIDs = new short[titlesArpg.size()];
        String[] titles = new String[titlesArpg.size()];
        int titleIndex = 0;

        for (String genderName : titlesArpg.keySet()) {
            titleGenderIDs[titleIndex] = genderManager.getGenderIDFromGenderName(genderName);
            titles[titleIndex] = titlesArpg.get(genderName).getAsString();
            titleIndex++;
        }

        ProfessionData professionData = new ProfessionData(
                professionName,
                professionID,
                ArpgUtility.getString(professionArpg, "display_name", professionName),
                ArpgUtility.getString(professionArpg, "description", EngineSetting.PROGRESSION_TEXT_NONE),
                titleGenderIDs,
                titles,
                starting,
                requirementGroups,
                ProgressionArpgUtility.parseValueMap(
                        professionArpg, "stats", statManager::getStatIDFromStatName, owner),
                ProgressionArpgUtility.parseValueMap(
                        professionArpg, "learning", tradeManager::getTradeIDFromTradeName, owner),
                ProgressionArpgUtility.parseValueMap(
                        professionArpg, "trades", tradeManager::getTradeIDFromTradeName, owner),
                ProgressionArpgUtility.parseValueMap(
                        professionArpg, "attributes", attributeManager::getAttributeIDFromAttributeName, owner));

        ProfessionHandle professionHandle = create(ProfessionHandle.class);
        professionHandle.constructor(professionData);

        return professionHandle;
    }

    // Requirements \\

    private RequirementGroupStruct[] parseRequirementGroups(ArpgObjectStruct professionArpg, String owner) {

        if (!ArpgUtility.hasArray(professionArpg, "requirements"))
            return new RequirementGroupStruct[0];

        ArpgArrayStruct groupArray = professionArpg.getAsArray("requirements");
        RequirementGroupStruct[] groups = new RequirementGroupStruct[groupArray.size()];

        for (int i = 0; i < groups.length; i++) {

            ArpgArrayStruct requirementArray = ArpgUtility.validateArray(groupArray.get(i).getAsObject(), "all");
            RequirementStruct[] requirements = new RequirementStruct[requirementArray.size()];

            if (requirements.length == 0)
                throwException(owner + " has a requirement group with nothing in its \"all\" list.");

            for (int j = 0; j < requirements.length; j++)
                requirements[j] = parseRequirement(requirementArray.get(j).getAsObject(), owner);

            groups[i] = new RequirementGroupStruct(requirements);
        }

        return groups;
    }

    private RequirementStruct parseRequirement(ArpgObjectStruct requirementArpg, String owner) {

        RequirementKind kind = ArpgUtility.toEnum(
                ArpgUtility.validateString(requirementArpg, "kind"), RequirementKind.class);
        float min = ArpgUtility.getFloat(requirementArpg, "min", EngineSetting.STAT_DEFAULT_MIN);
        float max = ArpgUtility.getFloat(requirementArpg, "max", EngineSetting.STAT_DEFAULT_MAX);

        short targetID = switch (kind) {
            case TRADE -> tradeManager.getTradeIDFromTradeName(validateTarget(requirementArpg, owner));
            case SKILL -> skillManager.getSkillIDFromSkillName(validateTarget(requirementArpg, owner));
            case DEED -> deedManager.getDeedIDFromDeedName(validateTarget(requirementArpg, owner));
            case PROFESSION -> professionManager.registerProfessionName(validateTarget(requirementArpg, owner));
            default -> EngineSetting.REGISTRY_RESERVED_ID;
        };

        if (kind == RequirementKind.DEED)
            min = ArpgUtility.getInt(requirementArpg, "count", EngineSetting.PROFESSION_DEFAULT_DEED_COUNT);

        if (min > max)
            throwException(owner + " has a " + ArpgUtility.toEnumName(kind)
                    + " requirement whose \"min\" is above its \"max\".");

        return new RequirementStruct(kind, targetID, min, max);
    }

    private String validateTarget(ArpgObjectStruct requirementArpg, String owner) {

        if (!ArpgUtility.hasString(requirementArpg, "name"))
            throwException(owner + " has a requirement that needs the \"name\" of what it reads.");

        return requirementArpg.get("name").getAsString();
    }
}
