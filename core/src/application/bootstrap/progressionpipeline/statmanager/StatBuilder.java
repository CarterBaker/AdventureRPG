package application.bootstrap.progressionpipeline.statmanager;

import java.io.File;

import application.bootstrap.progressionpipeline.attributemanager.AttributeManager;
import application.bootstrap.progressionpipeline.stat.StatData;
import application.bootstrap.progressionpipeline.stat.StatFormat;
import application.bootstrap.progressionpipeline.stat.StatHandle;
import application.bootstrap.progressionpipeline.util.ProgressionArpgUtility;
import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class StatBuilder extends BuilderPackage {

    /*
     * Parses stat ARPG files and builds StatHandle instances. Each file lists
     * its stats under a "stats" array, each named by its "name" field: whether
     * it is "scaled" against its owner's level, its "base", the "min" and
     * "max" it is held between, the fraction each attribute point above the
     * start adds under "attributes", how it reads ("format"), whether it is
     * "shown" on the character page, and the "bonus_weight" gear rolls it as a
     * bonus with and the "bonus_value" such a bonus is worth at most.
     * Bootstrap-only.
     */

    // Internal
    private StatManager statManager;
    private AttributeManager attributeManager;

    // Base \\

    @Override
    protected void get() {
        this.statManager = get(StatManager.class);
        this.attributeManager = get(AttributeManager.class);
    }

    // Build \\

    ObjectArrayList<StatHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct statArray = ArpgUtility.validateArray(rootArpg, "stats");
        ObjectArrayList<StatHandle> stats = new ObjectArrayList<>();

        for (int i = 0; i < statArray.size(); i++)
            stats.add(parseStat(statArray.get(i).getAsObject(), arpgFile));

        return stats;
    }

    // Parse \\

    private StatHandle parseStat(ArpgObjectStruct statArpg, File arpgFile) {

        String statName = ArpgUtility.validateString(statArpg, "name");
        String owner = "Stat '" + statName + "' in " + arpgFile.getPath();
        short statID = statManager.registerStatName(statName);
        float min = ArpgUtility.getFloat(statArpg, "min", EngineSetting.STAT_DEFAULT_MIN);
        float max = ArpgUtility.getFloat(statArpg, "max", EngineSetting.STAT_DEFAULT_MAX);
        float bonusWeight = ArpgUtility.getFloat(statArpg, "bonus_weight", EngineSetting.STAT_DEFAULT_BONUS_WEIGHT);

        if (min > max || bonusWeight < 0f)
            throwException(owner + " needs a \"min\" no greater than its \"max\" and a \"bonus_weight\" "
                    + "of at least 0.");

        ValueMapStruct attributeFeeds = ProgressionArpgUtility.parseValueMap(
                statArpg, "attributes", attributeManager::getAttributeIDFromAttributeName, owner);

        StatData statData = new StatData(
                statName,
                statID,
                ArpgUtility.getString(statArpg, "display_name", statName),
                ArpgUtility.getString(statArpg, "description", EngineSetting.PROGRESSION_TEXT_NONE),
                ArpgUtility.getEnum(statArpg, "format", StatFormat.class, StatFormat.WHOLE),
                ArpgUtility.getBoolean(statArpg, "shown", true),
                ArpgUtility.getBoolean(statArpg, "scaled", false),
                ArpgUtility.getFloat(statArpg, "base", EngineSetting.STAT_DEFAULT_BASE),
                min,
                max,
                attributeFeeds,
                bonusWeight,
                ArpgUtility.getFloat(statArpg, "bonus_value", EngineSetting.STAT_DEFAULT_BONUS_VALUE));

        StatHandle statHandle = create(StatHandle.class);
        statHandle.constructor(statData);

        return statHandle;
    }
}
