package application.bootstrap.progressionpipeline.trademanager;

import java.io.File;

import application.bootstrap.progressionpipeline.statmanager.StatManager;
import application.bootstrap.progressionpipeline.trade.TradeData;
import application.bootstrap.progressionpipeline.trade.TradeHandle;
import application.bootstrap.progressionpipeline.util.ProgressionArpgUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TradeBuilder extends BuilderPackage {

    /*
     * Parses trade ARPG files and builds TradeHandle instances. Each file
     * lists its trades under a "trades" array, each named by its "name" field:
     * the "group" it is listed under, the "min" and "max" its value lies
     * between, its "learn_rate", whether it "decays", and the fraction each
     * whole step adds to the stats named under "stats". Bootstrap-only.
     */

    // Internal
    private TradeManager tradeManager;
    private StatManager statManager;

    // Base \\

    @Override
    protected void get() {
        this.tradeManager = get(TradeManager.class);
        this.statManager = get(StatManager.class);
    }

    // Build \\

    ObjectArrayList<TradeHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct tradeArray = ArpgUtility.validateArray(rootArpg, "trades");
        ObjectArrayList<TradeHandle> trades = new ObjectArrayList<>();

        for (int i = 0; i < tradeArray.size(); i++)
            trades.add(parseTrade(tradeArray.get(i).getAsObject(), arpgFile));

        return trades;
    }

    // Parse \\

    private TradeHandle parseTrade(ArpgObjectStruct tradeArpg, File arpgFile) {

        String tradeName = ArpgUtility.validateString(tradeArpg, "name");
        String owner = "Trade '" + tradeName + "' in " + arpgFile.getPath();
        short tradeID = tradeManager.registerTradeName(tradeName);
        float min = ArpgUtility.getFloat(tradeArpg, "min", EngineSetting.PROFICIENCY_DEFAULT_MIN);
        float max = ArpgUtility.getFloat(tradeArpg, "max", EngineSetting.PROFICIENCY_DEFAULT_MAX);
        float learnRate = ArpgUtility.getFloat(tradeArpg, "learn_rate", EngineSetting.PROFICIENCY_DEFAULT_LEARN_RATE);

        if (min > 0f || max < 0f || learnRate < 0f)
            throwException(owner + " needs a \"min\" of at most 0, a \"max\" of at least 0, "
                    + "and a \"learn_rate\" of at least 0.");

        TradeData tradeData = new TradeData(
                tradeName,
                tradeID,
                ArpgUtility.getString(tradeArpg, "display_name", tradeName),
                ArpgUtility.getString(tradeArpg, "description", EngineSetting.PROGRESSION_TEXT_NONE),
                ArpgUtility.getString(tradeArpg, "group", EngineSetting.PROGRESSION_TEXT_NONE),
                min,
                max,
                learnRate,
                ArpgUtility.getBoolean(tradeArpg, "decays", true),
                ProgressionArpgUtility.parseValueMap(
                        tradeArpg, "stats", statManager::getStatIDFromStatName, owner));

        TradeHandle tradeHandle = create(TradeHandle.class);
        tradeHandle.constructor(tradeData);

        return tradeHandle;
    }
}
