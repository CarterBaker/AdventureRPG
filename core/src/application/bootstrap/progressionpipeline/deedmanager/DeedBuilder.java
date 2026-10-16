package application.bootstrap.progressionpipeline.deedmanager;

import java.io.File;

import application.bootstrap.progressionpipeline.deed.DeedData;
import application.bootstrap.progressionpipeline.deed.DeedHandle;
import application.bootstrap.progressionpipeline.trademanager.TradeManager;
import application.bootstrap.progressionpipeline.util.ProgressionArpgUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class DeedBuilder extends BuilderPackage {

    /*
     * Parses deed ARPG files and builds DeedHandle instances. Each file lists
     * its deeds under a "deeds" array, each named by its "name" field: its
     * "experience" as a share of one even-level kill, the "morality" it moves,
     * the practice it gives the trades under "trades", and whether it is
     * "counted". Bootstrap-only.
     */

    // Internal
    private DeedManager deedManager;
    private TradeManager tradeManager;

    // Base \\

    @Override
    protected void get() {
        this.deedManager = get(DeedManager.class);
        this.tradeManager = get(TradeManager.class);
    }

    // Build \\

    ObjectArrayList<DeedHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct deedArray = ArpgUtility.validateArray(rootArpg, "deeds");
        ObjectArrayList<DeedHandle> deeds = new ObjectArrayList<>();

        for (int i = 0; i < deedArray.size(); i++)
            deeds.add(parseDeed(deedArray.get(i).getAsObject(), arpgFile));

        return deeds;
    }

    // Parse \\

    private DeedHandle parseDeed(ArpgObjectStruct deedArpg, File arpgFile) {

        String deedName = ArpgUtility.validateString(deedArpg, "name");
        String owner = "Deed '" + deedName + "' in " + arpgFile.getPath();
        short deedID = deedManager.registerDeedName(deedName);
        float experience = ArpgUtility.getFloat(deedArpg, "experience", 0f);

        if (experience < 0f)
            throwException(owner + " needs an \"experience\" of at least 0.");

        DeedData deedData = new DeedData(
                deedName,
                deedID,
                ArpgUtility.getString(deedArpg, "display_name", deedName),
                ArpgUtility.getString(deedArpg, "description", EngineSetting.PROGRESSION_TEXT_NONE),
                experience,
                ArpgUtility.getFloat(deedArpg, "morality", 0f),
                ProgressionArpgUtility.parseValueMap(
                        deedArpg, "trades", tradeManager::getTradeIDFromTradeName, owner),
                ArpgUtility.getBoolean(deedArpg, "counted", false));

        DeedHandle deedHandle = create(DeedHandle.class);
        deedHandle.constructor(deedData);

        return deedHandle;
    }
}
